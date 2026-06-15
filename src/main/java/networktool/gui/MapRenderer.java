package main.java.networktool.gui;

import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Zeichnet Knoten und Kanten der Netzwerkkarte.
 * Keine Zustandshaltung — alle Methoden statisch.
 *
 * Node-Icons:
 *  G  = Gateway       * = Ich (lokal)    S = Switch/Hub
 *  W  = Windows       L = Linux          M = macOS/iOS
 *  A  = Android       P = Drucker        D = DNS-Server
 *  H  = DHCP-Server   N = NTP-Server     ? = Unbekannt
 */
final class MapRenderer {

    private MapRenderer() {}

    static void drawBackground(Graphics2D g2, Color bg, int w, int h) {
        g2.setColor(bg);
        g2.fillRect(-10000, -10000, 30000, 30000);
        g2.setColor(GuiTheme.isDark()
                ? new Color(0x12, 0x18, 0x12) : new Color(0xE4, 0xE2, 0xDC));
        for (int x = -3000; x < 6000; x += 60) g2.drawLine(x, -3000, x, 6000);
        for (int y = -3000; y < 6000; y += 60) g2.drawLine(-3000, y, 6000, y);
    }

    static void drawEdge(Graphics2D g2, GuiNetworkMap.Edge e) {
        switch (e.type) {
            case SELF_LINK -> {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(ACCENT2);
            }
            case UPLINK -> {
                g2.setStroke(new BasicStroke(1.8f));
                g2.setColor(new Color(0xFF, 0xA0, 0x30, 200));
            }
            default -> {
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
                g2.setColor(GuiTheme.isDark()
                        ? new Color(0x30, 0x45, 0x30) : new Color(0xC0, 0xBC, 0xB4));
            }
        }
        g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);
    }

    static void drawNode(Graphics2D g2, GuiNetworkMap.Node n) {
        Color col = nodeColor(n);
        int   r   = nodeRadius(n);
        drawGlow(g2, n, col, r);
        drawBody(g2, n, col, r);
        drawIcon(g2, n, col, r);
        drawLabels(g2, n, col, r);
    }

    // ── Node parts ────────────────────────────────────────────────────────

    private static void drawGlow(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
        g2.fillOval(n.x - r - 6, n.y - r - 6, (r + 6) * 2, (r + 6) * 2);
    }

    private static void drawBody(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        Color bodyBg = GuiTheme.isDark()
                ? new Color(0x08, 0x0C, 0x08) : new Color(0xF4, 0xF2, 0xEE);
        g2.setColor(bodyBg);
        g2.fillOval(n.x - r, n.y - r, r * 2, r * 2);
        g2.setColor(col);
        float strokeW = n.type == GuiNetworkMap.NodeType.SWITCH ? 2.5f : 1.5f;
        g2.setStroke(new BasicStroke(strokeW));
        g2.drawOval(n.x - r, n.y - r, r * 2, r * 2);

        // Doppelring für Switch-Typ (visuell erkennbar)
        if (n.type == GuiNetworkMap.NodeType.SWITCH) {
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(n.x - r + 4, n.y - r + 4, (r - 4) * 2, (r - 4) * 2);
        }

        // Infra-Knoten (DNS/DHCP/NTP): gestrichelter Doppelring
        if (isInfraSwitch(n)) {
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f));
            g2.drawOval(n.x - r - 3, n.y - r - 3, (r + 3) * 2, (r + 3) * 2);
        }
    }

    private static void drawIcon(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        String icon = nodeIcon(n);
        g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 11 : 9));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1f));
        g2.drawString(icon, n.x - fm.stringWidth(icon) / 2, n.y + fm.getAscent() / 2 - 1);
    }

    private static void drawLabels(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        // IP-Label
        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();
        Color dimColor = GuiTheme.isDark()
                ? new Color(0xA0, 0x9C, 0x90) : new Color(0x40, 0x42, 0x3E);
        g2.setColor(dimColor);
        g2.drawString(n.ip, n.x - fm.stringWidth(n.ip) / 2, n.y + r + 13);

        // Hostname (ohne IP-Duplikat und ohne MAC)
        String hn = cleanHostname(n.hostname, n.ip);
        if (hn != null) {
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
            fm = g2.getFontMetrics();
            g2.setColor(n.type == GuiNetworkMap.NodeType.SELF ? ACCENT2 : FG_DIM);
            g2.drawString(hn, n.x - fm.stringWidth(hn) / 2, n.y + r + 23);
        }

        // Rollen-Label (DNS/DHCP/NTP) unter Hostname
        String roleStr = extractRoleLabel(n.os);
        if (roleStr != null) {
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, 8));
            fm = g2.getFontMetrics();
            g2.setColor(roleColor(roleStr));
            g2.drawString(roleStr, n.x - fm.stringWidth(roleStr) / 2, n.y + r + 34);
        }
    }

    // ── Color / Icon helpers ──────────────────────────────────────────────

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

    private static int nodeRadius(GuiNetworkMap.Node n) {
        return switch (n.type) {
            case GATEWAY, SWITCH -> 20;
            case SELF            -> 17;
            default              -> 13;
        };
    }

    private static String nodeIcon(GuiNetworkMap.Node n) {
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

    // ── Helpers ───────────────────────────────────────────────────────────

    private static boolean isInfraSwitch(GuiNetworkMap.Node n) {
        if (n.type != GuiNetworkMap.NodeType.SWITCH || n.os == null) return false;
        String os = n.os.toLowerCase();
        return os.contains("dns-server") || os.contains("dhcp-server") || os.contains("ntp-server");
    }

    private static String cleanHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip)) return null;
        if (hostname.startsWith("host-"))            return null;
        String h = hostname;
        int bracket = h.indexOf(" [");
        if (bracket > 0) h = h.substring(0, bracket).trim();
        if (h.length() > 18) h = h.substring(0, 17) + "…";
        return h.equals(ip) ? null : h;
    }

    /** Extrahiert das erste Rollen-Label aus dem os-Feld (z.B. "DNS-Server"). */
    private static String extractRoleLabel(String os) {
        if (os == null) return null;
        if (os.contains("DNS-Server"))  return "DNS";
        if (os.contains("DHCP-Server")) return "DHCP";
        if (os.contains("NTP-Server"))  return "NTP";
        return null;
    }

    private static Color roleColor(String role) {
        return switch (role) {
            case "DNS"  -> new Color(0x60, 0xD0, 0xFF);
            case "DHCP" -> new Color(0xFF, 0xC0, 0x40);
            case "NTP"  -> new Color(0xA0, 0xFF, 0xC0);
            default     -> FG_DIM;
        };
    }
}