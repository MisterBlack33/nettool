package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.util.Map;

/** Übermalt Kanten mit der Heatmap-Farbe des höher belasteten Endpunkts. Kein State. */
final class MapHeatmapRenderer {

    private static final float STROKE_WIDTH = 3f;

    private MapHeatmapRenderer() {}

    static void overlay(Graphics2D g2, GuiNetworkMap.Edge edge, Map<String, Long> load) {
        if (load == null || load.isEmpty()) return;
        double level = Math.max(MapTrafficHeatmap.levelFor(edge.from.ip, load),
                MapTrafficHeatmap.levelFor(edge.to.ip, load));
        if (level <= 0) return;
        g2.setStroke(new BasicStroke(STROKE_WIDTH));
        g2.setColor(MapTrafficHeatmap.colorFor(level));
        g2.drawLine(edge.from.x, edge.from.y, edge.to.x, edge.to.y);
    }
}
