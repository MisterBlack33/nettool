package main.java.networktool.gui.map;

import main.java.networktool.gui.components.GuiNetworkMap;

import java.awt.Color;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Reine Lookup-Logik für das Aussehen eines Knotens (Farbe, Icon, Radius,
 * Rollen-Label) – keine Graphics2D-Abhängigkeit, daher isoliert testbar.
 */
final class MapNodeStyle {

    private MapNodeStyle() {}

    static Color nodeColor(GuiNetworkMap.Node n) {
        // Infra-Rollen haben eigene Farben
        if (n.os != null) {
            String os = n.os.toLowerCase();
            if (os.contains("dns-server"))  return new Color(0x60, 0xD0, 0xFF);
            if (os.contains("dhcp-server")) return new Color(0xFF, 0xC0, 0x40);
            if (os.contains("ntp-server"))  return new Color(0xA0, 0xFF, 0xC0);
        }
        return switch (n.type) {
            case SELF    -> ACCENT2;
            case GATEWAY -> NET_COL;
            case SWITCH  -> new Color(0xFF, 0xA0, 0x30);
            default      -> osColor(n.os);
        };
    }

    static int nodeRadius(GuiNetworkMap.Node n) {
        return switch (n.type) {
            case GATEWAY, SWITCH -> 20;
            case SELF            -> 17;
            default              -> 13;
        };
    }

    static String nodeIcon(GuiNetworkMap.Node n) {
        // Infra-Rollen-Icons zuerst prüfen
        if (n.os != null) {
            String os = n.os.toLowerCase();
            if (os.contains("dns-server"))  return "D";
            if (os.contains("dhcp-server")) return "H";
            if (os.contains("ntp-server"))  return "N";
        }
        return switch (n.type) {
            case GATEWAY -> "G";
            case SELF    -> "*";
            case SWITCH  -> "S";
            default      -> osIcon(n.os);
        };
    }

    private static String osIcon(String os) {
        if (os == null) return "?";
        String l = os.toLowerCase();
        if (l.contains("windows"))                            return "W";
        if (l.contains("linux") || l.contains("unix"))        return "L";
        if (l.contains("mac")   || l.contains("ios"))         return "M";
        if (l.contains("android"))                            return "A";
        if (l.contains("drucker") || l.contains("printer"))   return "P";
        if (l.contains("router") || l.contains("switch"))     return "R";
        if (l.contains("raspberry"))                          return "π";
        return "?";
    }

    static boolean isInfraSwitch(GuiNetworkMap.Node n) {
        if (n.type != GuiNetworkMap.NodeType.SWITCH || n.os == null) return false;
        String os = n.os.toLowerCase();
        return os.contains("dns-server") || os.contains("dhcp-server") || os.contains("ntp-server");
    }

    static String cleanHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip)) return null;
        if (hostname.startsWith("host-"))            return null;
        String h = hostname;
        int bracket = h.indexOf(" [");
        if (bracket > 0) h = h.substring(0, bracket).trim();
        if (h.length() > 18) h = h.substring(0, 17) + "…";
        return h.equals(ip) ? null : h;
    }

    /** Extrahiert das erste Rollen-Label aus dem os-Feld (z.B. "DNS-Server"). */
    static String extractRoleLabel(String os) {
        if (os == null) return null;
        if (os.contains("DNS-Server"))  return "DNS";
        if (os.contains("DHCP-Server")) return "DHCP";
        if (os.contains("NTP-Server"))  return "NTP";
        return null;
    }

    static Color roleColor(String role) {
        return switch (role) {
            case "DNS"  -> new Color(0x60, 0xD0, 0xFF);
            case "DHCP" -> new Color(0xFF, 0xC0, 0x40);
            case "NTP"  -> new Color(0xA0, 0xFF, 0xC0);
            default     -> FG_DIM;
        };
    }
}
