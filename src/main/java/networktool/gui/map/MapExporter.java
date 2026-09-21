package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;

import java.awt.Color;
import java.util.List;

/**
 * Exportiert die Netzwerk-Topologie als SVG- oder GraphML-String.
 * Reiner String-Bauer, keine Datei-I/O — Aufrufer entscheidet über Speicherort.
 */
public final class MapExporter {

    private static final int MARGIN         = 60;
    private static final int DEFAULT_WIDTH  = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int LABEL_OFFSET   = 12;
    private static final String EDGE_NORMAL = "#888888";
    private static final String EDGE_UPLINK = "#ffa030";
    private static final String EDGE_SELF   = "#4cc260";

    private MapExporter() {}

    public static String toSvg(List<GuiNetworkMap.Node> nodes, List<GuiNetworkMap.Edge> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"").append(viewBox(nodes)).append("\">\n");
        for (GuiNetworkMap.Edge e : edges) {
            sb.append("  <line x1=\"").append(e.from.x).append("\" y1=\"").append(e.from.y)
              .append("\" x2=\"").append(e.to.x).append("\" y2=\"").append(e.to.y)
              .append("\" stroke=\"").append(edgeColor(e.type)).append("\" />\n");
        }
        for (GuiNetworkMap.Node n : nodes) {
            int r = MapNodeStyle.nodeRadius(n);
            sb.append("  <circle cx=\"").append(n.x).append("\" cy=\"").append(n.y)
              .append("\" r=\"").append(r).append("\" fill=\"").append(hex(MapNodeStyle.nodeColor(n))).append("\" />\n")
              .append("  <text x=\"").append(n.x).append("\" y=\"").append(n.y + r + LABEL_OFFSET)
              .append("\" text-anchor=\"middle\" font-size=\"10\">").append(escXml(n.ip)).append("</text>\n");
        }
        return sb.append("</svg>").toString();
    }

    public static String toGraphml(List<GuiNetworkMap.Node> nodes, List<GuiNetworkMap.Edge> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n")
          .append("  <key id=\"type\" for=\"node\" attr.name=\"type\" attr.type=\"string\"/>\n")
          .append("  <key id=\"hostname\" for=\"node\" attr.name=\"hostname\" attr.type=\"string\"/>\n")
          .append("  <key id=\"kind\" for=\"edge\" attr.name=\"kind\" attr.type=\"string\"/>\n")
          .append("  <graph id=\"nettool\" edgedefault=\"undirected\">\n");
        for (GuiNetworkMap.Node n : nodes) {
            sb.append("    <node id=\"").append(escXml(n.ip)).append("\">")
              .append("<data key=\"type\">").append(n.type).append("</data>")
              .append("<data key=\"hostname\">").append(escXml(n.hostname)).append("</data></node>\n");
        }
        for (GuiNetworkMap.Edge e : edges) {
            sb.append("    <edge source=\"").append(escXml(e.from.ip))
              .append("\" target=\"").append(escXml(e.to.ip)).append("\">")
              .append("<data key=\"kind\">").append(e.type).append("</data></edge>\n");
        }
        return sb.append("  </graph>\n</graphml>").toString();
    }

    /** Umschließt alle Knoten samt Rand, damit nichts abgeschnitten wird. */
    static String viewBox(List<GuiNetworkMap.Node> nodes) {
        if (nodes.isEmpty()) return "0 0 " + DEFAULT_WIDTH + " " + DEFAULT_HEIGHT;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (GuiNetworkMap.Node n : nodes) {
            minX = Math.min(minX, n.x); maxX = Math.max(maxX, n.x);
            minY = Math.min(minY, n.y); maxY = Math.max(maxY, n.y);
        }
        return (minX - MARGIN) + " " + (minY - MARGIN) + " "
                + (maxX - minX + 2 * MARGIN) + " " + (maxY - minY + 2 * MARGIN);
    }

    private static String edgeColor(GuiNetworkMap.EdgeType type) {
        return switch (type) {
            case UPLINK    -> EDGE_UPLINK;
            case SELF_LINK -> EDGE_SELF;
            default        -> EDGE_NORMAL;
        };
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
