// src/main/java/networktool/gui/MapTopology.java
package main.java.networktool.gui;

import main.java.networktool.logic.scan.MapTrafficObserver;
import main.java.networktool.storage.NetworkStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Baut Kanten zwischen Netzknoten auf.
 * Knotenklassifizierung delegiert an MapNodeClassifier.
 * Legacy-Signaturen (ip,os,hn) bleiben für Tests erhalten.
 */
final class MapTopology {

    private MapTopology() {}

    private static final Set<Integer> ENDDEVICE_PORTS = Set.of(3389, 445, 5985, 5986, 9100, 515, 631);

    private static final Set<String> SWITCH_OUIS = Set.of(
            "00:00:0C","00:1A:A1","00:1B:54","00:1C:57","00:1D:70","00:1E:BD",
            "00:1F:CA","00:21:A0","00:22:90","00:23:AC","00:24:14","00:25:84",
            "00:17:A4","00:18:71","00:1A:4B","00:1C:2E","00:1F:FE","00:21:5A",
            "3C:D9:2B","40:B0:34","50:65:F3","5C:8A:38","6C:C2:17","78:AC:C0",
            "80:C1:6E","84:34:97","9C:8E:99","A8:97:DC","B4:39:D6","C4:34:6B",
            "D8:C7:C8","F0:92:1C","F4:CE:46","CC:46:D6","E8:40:F2","FC:FB:FB",
            "50:C7:BF","AC:84:C9","C4:6E:1F","F8:1A:67"
    );

    // ── Public classify entry points ──────────────────────────────────────

    static void classifyNodes(List<GuiNetworkMap.Node> nodes) {
        classifyNodes(nodes, new MapTrafficObserver());
    }

    static void classifyNodes(List<GuiNetworkMap.Node> nodes, MapTrafficObserver observer) {
        MapNodeClassifier.classify(nodes, observer);
    }

    // ── Edge building ─────────────────────────────────────────────────────

    static List<GuiNetworkMap.Edge> buildEdges(
            List<GuiNetworkMap.Node> nodes,
            GuiNetworkMap.Node gateway,
            GuiNetworkMap.Node self,
            Map<String, String> hopParent) {

        if (gateway == null) return Collections.emptyList();
        List<GuiNetworkMap.Edge> edges    = new ArrayList<>();
        List<GuiNetworkMap.Node> switches = collectSwitches(nodes);

        switches.forEach(sw -> edges.add(edge(sw, gateway, GuiNetworkMap.EdgeType.UPLINK)));
        if (self != null) edges.add(edge(self, gateway, GuiNetworkMap.EdgeType.SELF_LINK));

        for (GuiNetworkMap.Node node : nodes) {
            if (node.type != GuiNetworkMap.NodeType.HOST) continue;
            GuiNetworkMap.Node parent = resolveParent(node, switches, gateway, hopParent, nodes, edges);
            edges.add(edge(node, parent, GuiNetworkMap.EdgeType.NORMAL));
        }
        return edges;
    }

    // ── Legacy delegates (kept for test compatibility) ────────────────────

    /** @deprecated Nutze MapNodeClassifier.isEndDevice(Node) */
    static boolean isEndDevice(String ip, String os, String hostname) {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node(ip, hostname, os, GuiNetworkMap.NodeType.HOST);
        return MapNodeClassifier.isEndDevice(n);
    }

    /** @deprecated Nutze MapNodeClassifier.isSwitchByKeyword(Node) */
    static boolean isSwitchByKeyword(String os, String hostname) {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("", hostname, os, GuiNetworkMap.NodeType.HOST);
        return MapNodeClassifier.isSwitchByKeyword(n);
    }

    // ── Package-private detection helpers ────────────────────────────────

    static boolean isSwitchByOui(String hostname) {
        if (hostname == null) return false;
        int start = hostname.indexOf('['), end = hostname.indexOf(']');
        if (start < 0 || end <= start + 7) return false;
        String mac = hostname.substring(start + 1, end).trim().toUpperCase().replace("-", ":");
        return mac.length() >= 8 && SWITCH_OUIS.contains(mac.substring(0, 8));
    }

    static boolean isSwitchByPorts(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> (h.ports.containsKey(161) || h.ports.containsKey(23))
                        && h.ports.keySet().stream().noneMatch(ENDDEVICE_PORTS::contains))
                .orElse(false);
    }

    static String subnet24(String ip) {
        if (ip == null) return null;
        int lastDot = ip.lastIndexOf('.');
        return lastDot > 0 ? ip.substring(0, lastDot) : null;
    }

    static int lastOctet(String ip) {
        if (ip == null) return 999;
        int lastDot = ip.lastIndexOf('.');
        try { return Integer.parseInt(ip.substring(lastDot + 1)); }
        catch (NumberFormatException e) { return 999; }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static GuiNetworkMap.Node resolveParent(
            GuiNetworkMap.Node node,
            List<GuiNetworkMap.Node> switches,
            GuiNetworkMap.Node gateway,
            Map<String, String> hopParent,
            List<GuiNetworkMap.Node> allNodes,
            List<GuiNetworkMap.Edge> edges) {

        String hopIp = hopParent.get(node.ip);
        if (hopIp != null) return findOrCreateSwitch(hopIp, allNodes, gateway, switches, edges);

        String subnet = subnet24(node.ip);
        if (subnet == null) return gateway;

        return switches.stream()
                .filter(sw -> MapSwitchStore.contains(sw.ip) && subnet.equals(subnet24(sw.ip)))
                .findFirst()
                .orElseGet(() -> switches.stream()
                        .filter(sw -> subnet.equals(subnet24(sw.ip)))
                        .min(Comparator.comparingInt(sw -> Math.abs(lastOctet(sw.ip) - lastOctet(node.ip))))
                        .orElse(gateway));
    }

    private static GuiNetworkMap.Node findOrCreateSwitch(
            String ip, List<GuiNetworkMap.Node> allNodes,
            GuiNetworkMap.Node gateway,
            List<GuiNetworkMap.Node> switches,
            List<GuiNetworkMap.Edge> edges) {

        GuiNetworkMap.Node existing = allNodes.stream()
                .filter(n -> n.ip.equals(ip)).findFirst().orElse(null);
        if (existing == null) {
            GuiNetworkMap.Node sw = new GuiNetworkMap.Node(ip, ip, "Router / Switch", GuiNetworkMap.NodeType.SWITCH);
            allNodes.add(sw); switches.add(sw);
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