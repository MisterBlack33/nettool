package main.java.networktool.gui.components;

import main.java.networktool.logic.analysis.probe.BandwidthHistoryEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BandwidthHistoryChartTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private Graphics2D graphics() {
        return new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    @Test void paint_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> BandwidthHistoryChart.paint(graphics(), Color.BLACK, List.of(), 200, 100));
    }

    @Test void paint_singleEntry_doesNotThrow() {
        List<BandwidthHistoryEntry> h = List.of(new BandwidthHistoryEntry(1, "1.1.1.1", 5, 2));
        assertDoesNotThrow(() -> BandwidthHistoryChart.paint(graphics(), Color.BLACK, h, 200, 100));
    }

    @Test void paint_multipleEntries_doesNotThrow() {
        List<BandwidthHistoryEntry> h = List.of(
                new BandwidthHistoryEntry(1, "1.1.1.1", 5, 2),
                new BandwidthHistoryEntry(2, "1.1.1.1", 50, 20),
                new BandwidthHistoryEntry(3, "1.1.1.1", 0, 0));
        assertDoesNotThrow(() -> BandwidthHistoryChart.paint(graphics(), Color.BLACK, h, 200, 100));
    }

    @Test void paint_zeroSize_doesNotThrow() {
        List<BandwidthHistoryEntry> h = List.of(new BandwidthHistoryEntry(1, "1.1.1.1", 1, 1));
        assertDoesNotThrow(() -> BandwidthHistoryChart.paint(graphics(), Color.BLACK, h, 0, 0));
    }

    @Test void paint_allZero_doesNotThrow() {
        List<BandwidthHistoryEntry> h = List.of(new BandwidthHistoryEntry(1, "1.1.1.1", 0, 0));
        assertDoesNotThrow(() -> BandwidthHistoryChart.paint(graphics(), Color.BLACK, h, 200, 100));
    }
}
