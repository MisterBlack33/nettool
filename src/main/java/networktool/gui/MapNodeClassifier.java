// src/main/java/networktool/gui/MapNodeClassifier.java
package main.java.networktool.gui;

import main.java.networktool.logic.scan.MapTrafficObserver;
import java.util.List;

/**
 * Klassifiziert Netzknoten anhand von OS, Hostname, OUI, Ports und Traffic-Rolle.
 * Ausgelagert aus MapTopology für Single-Responsibility.
 */
final class MapNodeClassifier {

    private MapNodeClassifier() {}

    private static final List<String> SWITCH_OS_KEYWORDS = List.of(
            "switch","hub","router","fritz","unifi","mikrotik","cisco",
            "netgear","procurve","aruba","tp-link","openwrt"
    );
    private static final List<String> SWITCH_HN_KEYWORDS = List.of(
            "switch","hub","router","sw-","-sw","sg-","gs-","fritz","unifi",
            "mikrotik","ap-","procurve"
    );
    private static final List<String> ENDDEVICE_OS = List.of(
            "windows","android","ios","ipad","ipados","macos","apple","linux","unix",
            "raspberry","samsung","xiaomi","huawei","oppo","realme","oneplus",
            "drucker","printer","jetdirect","ipp","lpd","cups"
    );
    private static final List<String> ENDDEVICE_HN = List.of(
            "desktop","laptop","phone","mobile","tablet","pad",
            "iphone","ipad","galaxy","pixel","a21","a31","a51","a52",
            "s20","s21","s22","s23","s24","redmi","poco","macbook","imac",
            "printer","drucker","epson","canon","brother","kyocera"
    );

    static void classify(List<GuiNetworkMap.Node> nodes, MapTrafficObserver observer) {
        for (GuiNetworkMap.Node node : nodes) {
            if (node.type != GuiNetworkMap.NodeType.HOST) continue;
            if (MapSwitchStore.contains(node.ip))       { promote(node); continue; }
            if (isEndDevice(node))                       continue;
            if (isSwitchByKeyword(node))                { promote(node); continue; }
            if (isSwitchByOui(node.hostname))           { promote(node); continue; }
            if (isSwitchByTrafficRole(node.ip, observer)){ promote(node); continue; }
            if (isSwitchByPorts(node.ip))                promote(node);
        }
    }

    static boolean isEndDevice(GuiNetworkMap.Node node) {
        String os = node.os != null ? node.os.toLowerCase() : "";
        String hn = node.hostname != null ? node.hostname.toLowerCase() : "";
        for (String kw : ENDDEVICE_OS) if (os.contains(kw)) return true;
        for (String kw : ENDDEVICE_HN) if (hn.contains(kw)) return true;
        return false;
    }

    static boolean isSwitchByKeyword(GuiNetworkMap.Node node) {
        String os = node.os != null ? node.os.toLowerCase() : "";
        String hn = node.hostname != null ? node.hostname.toLowerCase() : "";
        for (String kw : SWITCH_OS_KEYWORDS) if (os.contains(kw)) return true;
        for (String kw : SWITCH_HN_KEYWORDS) if (hn.contains(kw)) return true;
        return false;
    }

    static boolean isSwitchByOui(String hostname) {
        return MapTopology.isSwitchByOui(hostname); // OUI-Set bleibt in MapTopology
    }

    static boolean isSwitchByTrafficRole(String ip, MapTrafficObserver observer) {
        MapTrafficObserver.NodeRole role = observer.getRole(ip);
        return role == MapTrafficObserver.NodeRole.DNS_SERVER
                || role == MapTrafficObserver.NodeRole.DHCP_SERVER;
    }

    static boolean isSwitchByPorts(String ip) {
        return MapTopology.isSwitchByPorts(ip);
    }

    private static void promote(GuiNetworkMap.Node node) {
        node.type = GuiNetworkMap.NodeType.SWITCH;
    }
}