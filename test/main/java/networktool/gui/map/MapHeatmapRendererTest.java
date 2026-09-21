package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapHeatmapRendererTest {

    private final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

    private GuiNetworkMap.Edge edge() {
        GuiNetworkMap.Node a = new GuiNetworkMap.Node("1.1.1.1", "a", "", GuiNetworkMap.NodeType.HOST);
        GuiNetworkMap.Node b = new GuiNetworkMap.Node("2.2.2.2", "b", "", GuiNetworkMap.NodeType.HOST);
        a.x = 10; a.y = 50; b.x = 90; b.y = 50;
        return new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.NORMAL);
    }

    @Test void overlay_withLoad_paintsHeatColor() {
        Graphics2D g = img.createGraphics();
        MapHeatmapRenderer.overlay(g, edge(), Map.of("1.1.1.1", 10L));
        g.dispose();
        assertEquals(MapTrafficHeatmap.colorFor(1.0).getRGB(), img.getRGB(50, 50));
    }

    @Test void overlay_emptyOrNullLoad_paintsNothing() {
        Graphics2D g = img.createGraphics();
        MapHeatmapRenderer.overlay(g, edge(), Map.of());
        MapHeatmapRenderer.overlay(g, edge(), null);
        g.dispose();
        assertEquals(0, img.getRGB(50, 50));
    }

    @Test void overlay_unrelatedLoad_paintsNothing() {
        Graphics2D g = img.createGraphics();
        MapHeatmapRenderer.overlay(g, edge(), Map.of("9.9.9.9", 5L));
        g.dispose();
        assertEquals(0, img.getRGB(50, 50));
    }
}
