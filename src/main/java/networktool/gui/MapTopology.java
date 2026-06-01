package main.java.networktool.gui;

import main.java.networktool.storage.NetworkStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Klassifiziert Netzknoten als Switch und leitet Verbindungen ab.
 *
 * Switch-Erkennung (Priorität):
 *  1. Manuell markiert (MapSwitchStore)
 *  2. OS/Hostname enthält Switch-Keywords
 *  3. MAC-OUI bekannter Switch-Hersteller
 *  4. Ports: SNMP(161)/Telnet(23) ohne PC/Drucker-Ports
 *
 * isEndDevice() verhindert Fehlklassifikation als Gate vor Pass 2–4.
 */
final class MapTopology {

    private MapTopology() {}

    private static final List<String> ENDDEVICE_OS = List.of(
            "windows","android","ios","ipad","ipados","macos","apple","linux","unix",
            "raspberry","samsung","xiaomi","huawei","oppo","realme","oneplus",
            "drucker","printer","jetdirect","ipp","lpd","cups"
    );

    private static final List<String> ENDDEVICE_HN = List.of(
            "desktop","laptop","phone","mobile","tablet","pad",
            "iphone","ipad","galaxy","pixel","a21","a31","a51","a52","a71","a72",
            "s20","s21","s22","s23","s24","note","redmi","poco","huawei","honor",
            "printer","drucker","epson","canon","brother","kyocera","macbook","imac"
    );

    private static final Set<Integer> ENDDEVICE_PORTS = Set.of(3389, 445, 5985, 5986, 9100, 515, 631);

    private static final Set<String> SWITCH_OUIS = Set.of(
            "00:00:0C","00:1A:A1","00:1B:54","00:1C:57","00:1D:70","00:1E:BD",
            "00:1F:CA","00:21:A0","00:22:90","00:23:AC","00:24:14","00:25:84",
            "00:26:CB","00:90:BF","C8:9C:1D","D0:72:DC",
            "00:17:A4","00:18:71","00:1A:4B","00:1C:2E","00:1F:FE","00:21:5A",
            "00:22:64","00:23:47","00:24:81","00:25:B3","00:26:55","00:30:C1",
            "3C:D9:2B","40:B0:34","50:65:F3","5C:8A:38","6C:C2:17","78:AC:C0",
            "80:C1:6E","84:34:97","9C:8E:99","A8:97:DC","B4:39:D6","C4:34:6B",
            "D8:C7:C8","F0:92:1C","F4:CE:46","00:14:6A","00:1C:10","00:60:2F",
            "A0:E0:AF","CC:46:D6","E8:40:F2","FC:FB:FB","50:C7:BF","AC:84:C9",
            "C4:6E:1F","F8:1A:67","00:09:5B","00:0F:B5","00:14:6C","00:18:4D",
            "20:E5:2A","44:94:FC","60:38:E0","B0:7F:B9","C0:3F:0E","E0:91:F5"
    );

    // ── Public API ────────────────────────────────────────────────────────

    static void classifyNodes(List<GuiNetworkMap.Node> nodes) {
        for (GuiNetworkMap.Node node : nodes) {
            if (node.type != GuiNetworkMap.NodeType.HOST) continue;
            if (MapSwitchStore.contains(node.ip))          { promoteToSwitch(node); continue; }
            if (isEndDevice(node.ip, node.os, node.hostname)) continue;
            if (isSwitchByKeyword(node.os, node.hostname))  { promoteToSwitch(node); continue; }
            if (isSwitchByOui(node.hostname))               { promoteToSwitch(node); continue; }
            if (isSwitchByPorts(node.ip))                     promoteToSwitch(node);
        }
    }

    /**
     * Leitet Verbindungen ab. Reihenfolge pro Host:
     *  1. Hop-Daten (Traceroute-Ergebnis)
     *  2. Manuell markierter Switch im gleichen /24
     *  3. Nächster Switch im gleichen /24
     *  4. Gateway
     */
    static List<GuiNetworkMap.Edge> buildEdges(
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

    // ── Private ───────────────────────────────────────────────────────────

    private static GuiNetworkMap.Node resolveParent(
            GuiNetworkMap.Node node,
            List<GuiNetworkMap.Node> switches,
            GuiNetworkMap.Node gateway,
            Map<String, String> hopParent,
            List<GuiNetworkMap.Node> allNodes,
            List<GuiNetworkMap.Edge> edges) {

        // Hop-Daten haben höchste Priorität
        String hopIp = hopParent.get(node.ip);
        if (hopIp != null) {
            GuiNetworkMap.Node hopNode = findOrCreateSwitch(hopIp, allNodes, gateway, switches, edges);
            return hopNode;
        }

        String subnet = subnet24(node.ip);
        if (subnet == null) return gateway;

        // Manuell markierter Switch im /24
        GuiNetworkMap.Node manualSwitch = switches.stream()
                .filter(sw -> MapSwitchStore.contains(sw.ip) && subnet.equals(subnet24(sw.ip)))
                .findFirst().orElse(null);
        if (manualSwitch != null) return manualSwitch;

        // Nächster Switch im /24
        return switches.stream()
                .filter(sw -> subnet.equals(subnet24(sw.ip)))
                .min(Comparator.comparingInt(sw -> Math.abs(lastOctet(sw.ip) - lastOctet(node.ip))))
                .orElse(gateway);
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
            GuiNetworkMap.Node newSwitch = new GuiNetworkMap.Node(ip, ip, "Router / Switch", GuiNetworkMap.NodeType.SWITCH);
            allNodes.add(newSwitch);
            switches.add(newSwitch);
            edges.add(edge(newSwitch, gateway, GuiNetworkMap.EdgeType.UPLINK));
            return newSwitch;
        }
        if (existing.type == GuiNetworkMap.NodeType.HOST) {
            promoteToSwitch(existing);
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

    private static void promoteToSwitch(GuiNetworkMap.Node node) {
        node.type = GuiNetworkMap.NodeType.SWITCH;
    }

    private static GuiNetworkMap.Edge edge(GuiNetworkMap.Node from, GuiNetworkMap.Node to, GuiNetworkMap.EdgeType type) {
        return new GuiNetworkMap.Edge(from, to, type);
    }

    // ── Detection helpers ─────────────────────────────────────────────────

    static boolean isEndDevice(String ip, String os, String hostname) {
        String osLower = os       != null ? os.toLowerCase()       : "";
        String hnLower = hostname != null ? hostname.toLowerCase() : "";
        for (String kw : ENDDEVICE_OS) if (osLower.contains(kw)) return true;
        for (String kw : ENDDEVICE_HN) if (hnLower.contains(kw)) return true;
        return hasEndDevicePorts(ip);
    }

    static boolean isSwitchByKeyword(String os, String hostname) {
        String osLower = os       != null ? os.toLowerCase()       : "";
        String hnLower = hostname != null ? hostname.toLowerCase() : "";
        return osLower.contains("switch") || osLower.contains("hub")   || osLower.contains("router")
                || osLower.contains("fritz")  || osLower.contains("unifi") || osLower.contains("mikrotik")
                || osLower.contains("cisco")  || osLower.contains("netgear") || osLower.contains("procurve")
                || osLower.contains("aruba")  || osLower.contains("tp-link")
                || hnLower.contains("switch") || hnLower.contains("hub")  || hnLower.contains("router")
                || hnLower.contains("sw-")    || hnLower.contains("-sw")  || hnLower.contains("sg-")
                || hnLower.contains("gs-")    || hnLower.contains("fritz") || hnLower.contains("unifi")
                || hnLower.contains("mikrotik") || hnLower.contains("ap-");
    }

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

    private static boolean hasEndDevicePorts(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> h.ports.keySet().stream().anyMatch(ENDDEVICE_PORTS::contains))
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
}