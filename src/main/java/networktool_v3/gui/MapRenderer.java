package main.java.networktool_v3.gui;

import java.awt.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Zeichnet Knoten und Kanten der Netzwerkkarte.
 * Keine Zustandshaltung — alle Methoden statisch.
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
        int r = nodeRadius(n);

        drawGlow(g2, n, col, r);
        drawBody(g2, n, col, r);
        drawIcon(g2, n, col, r);
        drawLabels(g2, n, col, r);
    }

    private static void drawGlow(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
        g2.fillOval(n.x - r - 6, n.y - r - 6, (r + 6) * 2, (r + 6) * 2);
    }

    private static void drawBody(Graphics2D g2, GuiNetworkMap.Node n, Color col, int r) {
        g2.setColor(GuiTheme.isDark() ? new Color(0x08, 0x0C, 0x08) : new Color(0xF4, 0xF2, 0xEE));
        g2.fillOval(n.x - r, n.y - r, r * 2, r * 2);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(n.type == GuiNetworkMap.NodeType.SWITCH ? 2.5f : 1.5f));
        g2.drawOval(n.x - r, n.y - r, r * 2, r * 2);
        if (n.type == GuiNetworkMap.NodeType.SWITCH) {
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(n.x - r + 4, n.y - r + 4, (r - 4) * 2, (r - 4) * 2);
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
        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(GuiTheme.isDark() ? new Color(0xA0, 0x9C, 0x90) : new Color(0x40, 0x42, 0x3E));
        g2.drawString(n.ip, n.x - fm.stringWidth(n.ip) / 2, n.y + r + 13);

        if (n.hostname != null && !n.hostname.equals(n.ip)) {
            String hn = n.hostname.length() > 18 ? n.hostname.substring(0, 17) + "." : n.hostname;
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
            fm = g2.getFontMetrics();
            g2.setColor(n.type == GuiNetworkMap.NodeType.SELF ? ACCENT2 : FG_DIM);
            g2.drawString(hn, n.x - fm.stringWidth(hn) / 2, n.y + r + 23);
        }
    }

    private static int nodeRadius(GuiNetworkMap.Node n) {
        return switch (n.type) {
            case GATEWAY, SWITCH -> 20;
            case SELF            -> 17;
            default              -> 13;
        };
    }

    static Color nodeColor(GuiNetworkMap.Node n) {
        return switch (n.type) {
            case SELF    -> ACCENT2;
            case GATEWAY -> NET_COL;
            case SWITCH  -> new Color(0xFF, 0xA0, 0x30);
            default      -> osColor(n.os);
        };
    }

    private static String nodeIcon(GuiNetworkMap.Node n) {
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
        if (l.contains("windows"))                              return "W";
        if (l.contains("linux") || l.contains("unix"))         return "L";
        if (l.contains("mac")   || l.contains("ios"))          return "M";
        if (l.contains("android"))                             return "A";
        if (l.contains("drucker") || l.contains("printer"))   return "P";
        if (l.contains("router") || l.contains("switch"))     return "R";
        return "?";
    }
}