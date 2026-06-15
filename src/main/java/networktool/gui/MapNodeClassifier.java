package main.java.networktool.gui;

import main.java.networktool.logic.scan.MapTrafficObserver;
import main.java.networktool.storage.NetworkStore;
import java.util.List;
import java.util.Set;

/**
 * Klassifiziert Netzknoten nach OS, Hostname, OUI, Ports und Traffic-Rolle.
 * Reihenfolge: manuell → Endgerät → Keyword → OUI → Traffic → Ports
 */
final class MapNodeClassifier {

    private MapNodeClassifier() {}

    private static final List<String> SWITCH_OS = List.of(
            "switch", "hub", "router", "fritz", "unifi", "mikrotik", "cisco",
            "netgear", "procurve", "aruba", "tp-link", "zyxel", "openwrt",
            "dns-server", "dhcp-server", "ntp-server"
    );
    private static final List<String> SWITCH_HN = List.of(
            "router", "sw-", "-sw", "sg-", "gs-", "fritz", "unifi",
            "mikrotik", "ap-", "procurve", "gateway", "dns", "dhcp", "ntp"
    );
    private static final List<String> END_OS = List.of(
            "windows", "android", "ios", "ipad", "macos", "apple", "linux", "unix",
            "raspberry", "samsung", "xiaomi", "huawei", "oppo", "realme", "oneplus",
            "drucker", "printer", "jetdirect", "cups"
    );
    private static final List<String> END_HN = List.of(
            "desktop", "laptop", "phone", "mobile", "tablet",
            "iphone", "ipad", "galaxy", "pixel", "macbook", "imac",
            "sm-a", "sm-g", "sm-s", "sm-n", "a21s", "a21", "a31", "a51", "a52s", "a52", "a53", "a54",
            "printer", "drucker", "epson", "canon", "brother", "kyocera"
    );
    private static final Set<Integer> INFRA_PORTS   = Set.of(161, 162, 23, 179, 830, 4786);
    private static final Set<Integer> END_PORTS      = Set.of(3389, 445, 5985, 9100, 515, 631);
    private static final Set<Integer> DNS_DHCP_PORTS = Set.of(53, 67, 68);

    // ── Public ────────────────────────────────────────────────────────────

    static void classify(List<GuiNetworkMap.Node> nodes, MapTrafficObserver observer) {
        for (GuiNetworkMap.Node n : nodes) {
            if (n.type != GuiNetworkMap.NodeType.HOST) continue;
            if (MapSwitchStore.contains(n.ip))          { promote(n); continue; }
            if (isEndDevice(n))                          continue;
            if (isSwitchByKeyword(n))                   { promote(n); continue; }
            if (MapTopology.isSwitchByOui(n.hostname))  { promote(n); continue; }
            if (isSwitchByTrafficRole(n.ip, observer))  { promote(n); applyRoleLabel(n, observer); continue; }
            if (isSwitchByPorts(n.ip))                    promote(n);
        }
    }

    // ── Detection ─────────────────────────────────────────────────────────

    static boolean isEndDevice(GuiNetworkMap.Node n) {
        String os = lower(n.os), hn = lower(n.hostname);
        return END_OS.stream().anyMatch(os::contains)
                || END_HN.stream().anyMatch(hn::contains)
                || hasAnyPort(n.ip, END_PORTS);
    }

    static boolean isSwitchByKeyword(GuiNetworkMap.Node n) {
        String os = lower(n.os), hn = lower(n.hostname);
        return SWITCH_OS.stream().anyMatch(os::contains)
                || SWITCH_HN.stream().anyMatch(hn::contains);
    }

    static boolean isSwitchByTrafficRole(String ip, MapTrafficObserver observer) {
        MapTrafficObserver.NodeRole role = observer.getRole(ip);
        return role == MapTrafficObserver.NodeRole.DNS_SERVER
                || role == MapTrafficObserver.NodeRole.DHCP_SERVER
                || role == MapTrafficObserver.NodeRole.NTP_SERVER;
    }

    static boolean isSwitchByPorts(String ip) {
        if (hasAnyPort(ip, END_PORTS)) return false;
        if (hasAnyPort(ip, INFRA_PORTS)) return true;
        // DNS/DHCP ohne SSH = Infra
        Set<Integer> ports = portsOf(ip);
        boolean hasDnsDhcp = ports.stream().anyMatch(DNS_DHCP_PORTS::contains);
        return hasDnsDhcp && !ports.contains(22);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static void promote(GuiNetworkMap.Node n) {
        n.type = GuiNetworkMap.NodeType.SWITCH;
    }

    private static void applyRoleLabel(GuiNetworkMap.Node n, MapTrafficObserver observer) {
        String label = switch (observer.getRole(n.ip)) {
            case DNS_SERVER  -> "DNS-Server";
            case DHCP_SERVER -> "DHCP-Server";
            case NTP_SERVER  -> "NTP-Server";
            default          -> null;
        };
        if (label == null) return;
        n.os = (n.os != null && !n.os.isBlank()) ? label + " / " + n.os : label;
    }

    private static boolean hasAnyPort(String ip, Set<Integer> ports) {
        return portsOf(ip).stream().anyMatch(ports::contains);
    }

    private static Set<Integer> portsOf(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> h.ports.keySet())
                .orElse(Set.of());
    }

    private static String lower(String s) { return s != null ? s.toLowerCase() : ""; }
}