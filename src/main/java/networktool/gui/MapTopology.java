package main.java.networktool.gui;

import main.java.networktool.storage.NetworkStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Klassifiziert Netzknoten und leitet Verbindungen ab.
 *
 * Switch-Erkennung (Priorität):
 *  1. Manuell markiert (MapSwitchStore)
 *  2. OS/Hostname enthält Switch-Keywords
 *  3. MAC-OUI bekannter Switch-Hersteller
 *  4. Ports: SNMP/Telnet/LLDP/STP ohne Endgeräte-Ports
 *  5. DNS/DHCP-Server-Ports ohne SSH/SMB
 */
final class MapTopology {

    private MapTopology() {}

    private static final List<String> ENDDEVICE_OS = List.of(
            "windows", "android", "ios", "ipad", "ipados", "macos", "apple", "linux", "unix",
            "raspberry", "samsung", "xiaomi", "huawei", "oppo", "realme", "oneplus",
            "drucker", "printer", "jetdirect", "ipp", "lpd", "cups"
    );

    private static final List<String> ENDDEVICE_HN = List.of(
            "desktop", "laptop", "phone", "mobile", "tablet", "pad",
            "iphone", "ipad", "galaxy", "pixel", "redmi", "poco",
            // Samsung Galaxy S-Serie (alle Generationen)
            "s10", "s11", "s20", "s21", "s22", "s23", "s24",
            // Samsung Galaxy A-Serie (alle Generationen)
            "a10", "a11", "a12", "a13", "a14", "a15",
            "a20", "a21", "a22", "a23", "a24", "a25",
            "a30", "a31", "a32", "a33", "a34", "a35",
            "a40", "a41", "a42", "a43", "a44", "a45",
            "a50", "a51", "a52", "a53", "a54", "a55",
            "a60", "a70", "a71", "a72", "a73", "a80",
            "a90", "a91",
            // Samsung Galaxy Z-Serie (Foldables)
            "z-flip", "z-fold",
            // Samsung Galaxy Note-Serie
            "note", "sm-a", "sm-g", "sm-s", "sm-n",
            // Drucker und Scanner
            "printer", "drucker", "epson", "canon", "brother", "kyocera", "xerox", "ricoh",
            "macbook", "imac", "workstation"
    );

    private static final Set<Integer> ENDDEVICE_PORTS = Set.of(3389, 445, 5985, 5986, 9100, 515, 631);

    // Ports die auf ein Netzwerkinfrastruktur-Gerät hindeuten
    private static final Set<Integer> NETWORK_INFRA_PORTS = Set.of(
            161,  // SNMP
            162,  // SNMP trap
            23,   // Telnet (Managed Switches)
            179,  // BGP
            646,  // LDP
            830,  // NETCONF
            4786  // Cisco Smart Install
    );

    // DNS/DHCP-Server = Infrastruktur (kein Endgerät)
    private static final Set<Integer> DNS_DHCP_PORTS = Set.of(53, 67, 68);

    private static final Set<String> SWITCH_OUIS = Set.of(
            // Cisco
            "00:00:0C", "00:1A:A1", "00:1B:54", "00:1C:57", "00:1D:70", "00:1E:BD",
            "00:1F:CA", "00:21:A0", "00:22:90", "00:23:AC", "00:24:14", "00:25:84",
            "00:26:CB", "00:90:BF", "C8:9C:1D", "D0:72:DC",
            // HP/Aruba/ProCurve
            "00:17:A4", "00:18:71", "00:1A:4B", "00:1C:2E", "00:1F:FE", "00:21:5A",
            "00:22:64", "00:23:47", "00:24:81", "00:25:B3", "00:26:55", "00:30:C1",
            // Juniper
            "3C:D9:2B", "40:B0:34", "50:65:F3", "5C:8A:38", "6C:C2:17", "78:AC:C0",
            "80:C1:6E", "84:34:97", "9C:8E:99", "A8:97:DC", "B4:39:D6", "C4:34:6B",
            "D8:C7:C8", "F0:92:1C", "F4:CE:46",
            // Netgear
            "00:14:6A", "00:1C:10", "00:60:2F", "A0:E0:AF", "CC:46:D6", "E8:40:F2",
            "FC:FB:FB",
            // Ubiquiti
            "50:C7:BF", "AC:84:C9", "C4:6E:1F", "F8:1A:67",
            // MikroTik
            "00:09:5B", "00:0F:B5", "00:14:6C", "00:18:4D",
            "20:E5:2A", "44:94:FC", "60:38:E0", "B0:7F:B9", "C0:3F:0E", "E0:91:F5",
            // TP-Link
            "6C:5A:B0", "A0:F3:C1", "B0:BE:76", "E8:DE:27",
            // D-Link
            "00:05:5D", "00:17:9A", "00:1B:11", "00:26:5A", "1C:7E:E5",
            // Zyxel
            "00:13:49", "00:19:CB", "00:A0:C5", "A4:2B:8C",
            // Fritz!Box / AVM
            "C8:0E:14", "DC:9F:DB", "E0:28:6D"
    );

    // ── Public API ────────────────────────────────────────────────────────

    static void classifyNodes(List<GuiNetworkMap.Node> nodes) {
        for (GuiNetworkMap.Node node : nodes) {
            if (node.type != GuiNetworkMap.NodeType.HOST) continue;
            if (MapSwitchStore.contains(node.ip))               { promoteToSwitch(node); continue; }
            if (isEndDevice(node.ip, node.os, node.hostname))   continue;
            if (isSwitchByKeyword(node.os, node.hostname))      { promoteToSwitch(node); continue; }
            if (isSwitchByOui(node.hostname))                   { promoteToSwitch(node); continue; }
            if (isSwitchByInfraPorts(node.ip))                  { promoteToSwitch(node); continue; }
            if (isDnsOrDhcpServer(node.ip))                       promoteToSwitch(node);
        }
    }

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

    // ── Detection helpers ─────────────────────────────────────────────────

    static boolean isEndDevice(String ip, String os, String hostname) {
        String osL = os       != null ? os.toLowerCase()       : "";
        String hnL = hostname != null ? hostname.toLowerCase() : "";
        for (String kw : ENDDEVICE_OS) if (osL.contains(kw)) return true;
        for (String kw : ENDDEVICE_HN) if (hnL.contains(kw)) return true;
        return hasEndDevicePorts(ip);
    }

    static boolean isSwitchByKeyword(String os, String hostname) {
        String osL = os       != null ? os.toLowerCase()       : "";
        String hnL = hostname != null ? hostname.toLowerCase() : "";
        return osL.contains("switch") || osL.contains("hub")   || osL.contains("router")
                || osL.contains("fritz")  || osL.contains("unifi") || osL.contains("mikrotik")
                || osL.contains("cisco")  || osL.contains("netgear") || osL.contains("procurve")
                || osL.contains("aruba")  || osL.contains("tp-link") || osL.contains("zyxel")
                || osL.contains("dns-server") || osL.contains("dhcp-server")
                || hnL.contains("router") || hnL.contains("sw-")    || hnL.contains("-sw")
                || hnL.contains("sg-")    || hnL.contains("gs-")    || hnL.contains("fritz")
                || hnL.contains("unifi")  || hnL.contains("mikrotik") || hnL.contains("ap-")
                || hnL.contains("gateway") || hnL.contains("dns")   || hnL.contains("dhcp");
    }

    static boolean isSwitchByOui(String hostname) {
        if (hostname == null) return false;
        int s = hostname.indexOf('['), e = hostname.indexOf(']');
        if (s < 0 || e <= s + 7) return false;
        String mac = hostname.substring(s + 1, e).trim().toUpperCase().replace("-", ":");
        return mac.length() >= 8 && SWITCH_OUIS.contains(mac.substring(0, 8));
    }

    /** SNMP, Telnet, BGP, NETCONF → Managed Switch / Router */
    static boolean isSwitchByInfraPorts(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> {
                    Set<Integer> ports = h.ports.keySet();
                    boolean hasInfra = ports.stream().anyMatch(NETWORK_INFRA_PORTS::contains);
                    boolean hasEndDevice = ports.stream().anyMatch(ENDDEVICE_PORTS::contains);
                    return hasInfra && !hasEndDevice;
                })
                .orElse(false);
    }

    /** Port 53 (DNS) oder 67 (DHCP) ohne typische Endgeräte-Ports → Infra-Node */
    /** Alias für MapNodeClassifier-Kompatibilität. */
    static boolean isSwitchByPorts(String ip) {
        return isSwitchByInfraPorts(ip);
    }

    static boolean isDnsOrDhcpServer(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> {
                    Set<Integer> ports = h.ports.keySet();
                    boolean hasDnsDhcp = ports.stream().anyMatch(DNS_DHCP_PORTS::contains);
                    boolean hasEndDevice = ports.stream().anyMatch(ENDDEVICE_PORTS::contains);
                    boolean hasSsh = ports.contains(22);
                    // SSH + DNS = Linux-Server (kein Switch), nur DHCP/DNS allein = Router/AP
                    return hasDnsDhcp && !hasEndDevice && !hasSsh;
                })
                .orElse(false);
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
        if (hopIp != null)
            return findOrCreateSwitch(hopIp, allNodes, gateway, switches, edges);

        String subnet = subnet24(node.ip);
        if (subnet == null) return gateway;

        GuiNetworkMap.Node manualSwitch = switches.stream()
                .filter(sw -> MapSwitchStore.contains(sw.ip) && subnet.equals(subnet24(sw.ip)))
                .findFirst().orElse(null);
        if (manualSwitch != null) return manualSwitch;

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
            GuiNetworkMap.Node sw = new GuiNetworkMap.Node(ip, ip, "Router / Switch", GuiNetworkMap.NodeType.SWITCH);
            allNodes.add(sw);
            switches.add(sw);
            edges.add(edge(sw, gateway, GuiNetworkMap.EdgeType.UPLINK));
            return sw;
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

    private static GuiNetworkMap.Edge edge(GuiNetworkMap.Node from, GuiNetworkMap.Node to,
                                           GuiNetworkMap.EdgeType type) {
        return new GuiNetworkMap.Edge(from, to, type);
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
        int last = ip.lastIndexOf('.');
        return last > 0 ? ip.substring(0, last) : null;
    }

    static int lastOctet(String ip) {
        if (ip == null) return 999;
        int last = ip.lastIndexOf('.');
        try { return Integer.parseInt(ip.substring(last + 1)); }
        catch (NumberFormatException e) { return 999; }
    }
}