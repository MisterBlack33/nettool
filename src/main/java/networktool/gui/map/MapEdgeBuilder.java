package networktool.gui.map;

import networktool.gui.components.GuiNetworkMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Baut die Kanten (Verbindungen) zwischen Gateway, Switches und Hosts
 * anhand von Subnetz-Nähe und bekannten Hop-Zwischenknoten.
 */
final class MapEdgeBuilder {

    private MapEdgeBuilder() {}

    static List<GuiNetworkMap.Edge> build(
            List<GuiNetworkMap.Node> nodes,
            GuiNetworkMap.Node gateway,
            GuiNetworkMap.Node self,
            Map<String, String> hopParent) {

        List<GuiNetworkMap.Edge> edges = new ArrayList<>();
        if (gateway == null) return edges;

        List<GuiNetworkMap.Node> switches = collectSwitches(nodes);
        switches.forEach(sw -> edges.add(edge(sw, gateway, GuiNetworkMap.EdgeType.UPLINK)));

        if (self != null)
            edges.add(edge(self, gateway, GuiNetworkMap.EdgeType.SELF_LINK));

        for (GuiNetworkMap.Node node : nodes) {
            if (node.type != GuiNetworkMap.NodeType.HOST) continue;
            GuiNetworkMap.Node parent = resolveParent(node, switches, gateway, hopParent, nodes, edges);
            edges.add(edge(node, parent, GuiNetworkMap.EdgeType.NORMAL));
        }
        return edges;
    }

    // ── Parent-Auflösung ──────────────────────────────────────────────────

    private static GuiNetworkMap.Node resolveParent(
            GuiNetworkMap.Node node,
            List<GuiNetworkMap.Node> switches,
            GuiNetworkMap.Node gateway,
            Map<String, String> hopParent,
            List<GuiNetworkMap.Node> allNodes,
            List<GuiNetworkMap.Edge> edges) {

        String hopIp = hopParent.get(node.ip);
        if (hopIp != null)
            return findOrCreateSwitch(hopIp, allNodes, gateway, switches, edges);

        String subnet = MapTopology.subnet24(node.ip);
        if (subnet == null) return gateway;

        GuiNetworkMap.Node manualSwitch = findManualSwitch(switches, subnet);
        if (manualSwitch != null) return manualSwitch;

        return nearestSwitchInSubnet(switches, subnet, node).orElse(gateway);
    }

    private static GuiNetworkMap.Node findManualSwitch(List<GuiNetworkMap.Node> switches, String subnet) {
        return switches.stream()
                .filter(sw -> MapSwitchStore.contains(sw.ip) && subnet.equals(MapTopology.subnet24(sw.ip)))
                .findFirst().orElse(null);
    }

    private static java.util.Optional<GuiNetworkMap.Node> nearestSwitchInSubnet(
            List<GuiNetworkMap.Node> switches, String subnet, GuiNetworkMap.Node node) {
        return switches.stream()
                .filter(sw -> subnet.equals(MapTopology.subnet24(sw.ip)))
                .min(Comparator.comparingInt(sw -> Math.abs(MapTopology.lastOctet(sw.ip) - MapTopology.lastOctet(node.ip))));
    }

    private static GuiNetworkMap.Node findOrCreateSwitch(
            String ip,
            List<GuiNetworkMap.Node> allNodes,
            GuiNetworkMap.Node gateway,
            List<GuiNetworkMap.Node> switches,
            List<GuiNetworkMap.Edge> edges) {

        GuiNetworkMap.Node existing = allNodes.stream()
                .filter(n -> n.ip.equals(ip)).findFirst().orElse(null);

        if (existing == null) {
            GuiNetworkMap.Node sw = new GuiNetworkMap.Node(ip, ip, "Router / Switch", GuiNetworkMap.NodeType.SWITCH);
            allNodes.add(sw);
            switches.add(sw);
            edges.add(edge(sw, gateway, GuiNetworkMap.EdgeType.UPLINK));
            return sw;
        }
        if (existing.type == GuiNetworkMap.NodeType.HOST) {
            existing.type = GuiNetworkMap.NodeType.SWITCH;
            switches.add(existing);
            edges.add(edge(existing, gateway, GuiNetworkMap.EdgeType.UPLINK));
        }
        return existing;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static List<GuiNetworkMap.Node> collectSwitches(List<GuiNetworkMap.Node> nodes) {
        return nodes.stream()
                .filter(n -> n.type == GuiNetworkMap.NodeType.SWITCH)
                .collect(Collectors.toList());
    }

    private static GuiNetworkMap.Edge edge(GuiNetworkMap.Node from, GuiNetworkMap.Node to,
                                           GuiNetworkMap.EdgeType type) {
        return new GuiNetworkMap.Edge(from, to, type);
    }
}