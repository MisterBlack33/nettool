package networktool.gui.map;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.storage.NetworkStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static networktool.gui.map.MapDeviceSignatures.*;

/**
 * Klassifiziert Netzknoten und leitet Verbindungen ab.
 *
 * Switch-Erkennung (Priorität):
 *  1. Manuell markiert (MapSwitchStore)
 *  2. OS/Hostname enthält Switch-Keywords
 *  3. MAC-OUI bekannter Switch-Hersteller
 *  4. Ports: SNMP/Telnet/LLDP/STP ohne Endgeräte-Ports
 *  5. DNS/DHCP-Server-Ports ohne SSH/SMB
 *
 * Signaturdaten siehe {@link MapDeviceSignatures}, Kantenaufbau siehe {@link MapEdgeBuilder}.
 */
final class MapTopology {

    private MapTopology() {}

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
        return MapEdgeBuilder.build(nodes, gateway, self, hopParent);
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

    /** Alias für MapNodeClassifier-Kompatibilität. */
    static boolean isSwitchByPorts(String ip) {
        return isSwitchByInfraPorts(ip);
    }

    /** Port 53 (DNS) oder 67 (DHCP) ohne typische Endgeräte-Ports → Infra-Node */
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

    private static void promoteToSwitch(GuiNetworkMap.Node node) {
        node.type = GuiNetworkMap.NodeType.SWITCH;
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
