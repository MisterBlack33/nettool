package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;
import main.java.networktool.gui.components.*;
import main.java.networktool.logic.scan.schedule.MapTrafficObserver;
import main.java.networktool.logic.scan.remote.RemoteNetScanner;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.storage.network.NetworkStore;

import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sammelt Knoten aus allen Datenquellen (Gateway, eigener Host, ScanHistory,
 * gespeicherte Hosts, Hop-Discovery) und klassifiziert bekannte Traffic-Rollen
 * (DNS-/DHCP-Server etc.) für {@link MapCanvas}.
 */
final class MapNodeCollector {

    private static final Logger LOG = Logger.getLogger(MapNodeCollector.class.getName());

    private MapNodeCollector() {}

    static GuiNetworkMap.Node collectInto(List<GuiNetworkMap.Node> nodes) {
        Set<String> seen = new LinkedHashSet<>();
        GuiNetworkMap.Node gwNode = null;

        String gw = RemoteNetScanner.detectDefaultGateway();
        if (gw != null && seen.add(gw))
            gwNode = addNode(nodes, gw, "Gateway", "Router / Netzwerkgeraet", GuiNetworkMap.NodeType.GATEWAY);

        try {
            InetAddress self = InetAddress.getLocalHost();
            if (seen.add(self.getHostAddress()))
                addNode(nodes, self.getHostAddress(), self.getHostName() + " (ich)", localOs(), GuiNetworkMap.NodeType.SELF);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Eigener Host konnte nicht zur Karte hinzugefügt werden", e);
        }

        for (ScanHistory.Entry entry : ScanHistory.getInstance().getAll())
            for (ScanResult r : entry.results)
                if (seen.add(r.getIp()))
                    addNode(nodes, r.getIp(), r.getHostname(), r.getOsGuess(), GuiNetworkMap.NodeType.HOST);

        for (HostResult h : NetworkStore.getInstance().getAllHosts())
            if (seen.add(h.ip))
                addNode(nodes, h.ip, cleanHostname(h.hostname), h.os, GuiNetworkMap.NodeType.HOST);

        // Zwischenknoten aus Hop-Discovery als Switch-Knoten einblenden
        for (String hopIp : GuiNetworkMap.HOP_PARENT.values()) {
            if (seen.add(hopIp))
                addNode(nodes, hopIp, hopIp, "Router / Switch", GuiNetworkMap.NodeType.SWITCH);
        }

        return gwNode;
    }

    static GuiNetworkMap.Node addNode(List<GuiNetworkMap.Node> nodes, String ip, String hostname,
                                       String os, GuiNetworkMap.NodeType type) {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node(ip, hostname, os, type);
        nodes.add(n);
        return n;
    }

    /**
     * Wendet bekannte Traffic-Rollen auf Nodes an.
     * DNS/DHCP-Server → SWITCH-Typ (Infra-Knoten).
     */
    static void applyTrafficRoles(List<GuiNetworkMap.Node> nodes, MapTrafficObserver trafficObserver) {
        for (GuiNetworkMap.Node node : nodes) {
            MapTrafficObserver.NodeRole role = trafficObserver.getRole(node.ip);
            if (role == MapTrafficObserver.NodeRole.DNS_SERVER
                    || role == MapTrafficObserver.NodeRole.DHCP_SERVER) {
                if (node.type == GuiNetworkMap.NodeType.HOST)
                    node.type = GuiNetworkMap.NodeType.SWITCH;
                // Rolle im OS-Feld anzeigen (wird von Renderer als Label verwendet)
                node.os = roleLabel(role) + (node.os != null && !node.os.isBlank()
                        ? " / " + node.os : "");
            }
        }
    }

    private static String roleLabel(MapTrafficObserver.NodeRole role) {
        return switch (role) {
            case DNS_SERVER  -> "DNS-Server";
            case DHCP_SERVER -> "DHCP-Server";
            case MDNS_NODE   -> "mDNS";
            case NTP_SERVER  -> "NTP-Server";
            default          -> "";
        };
    }

    private static String cleanHostname(String hostname) {
        if (hostname == null) return "";
        int i = hostname.indexOf(" [");
        return i < 0 ? hostname : hostname.substring(0, i).trim();
    }

    private static String localOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "Windows";
        if (os.contains("mac")) return "macOS";
        return "Linux/Unix";
    }
}
