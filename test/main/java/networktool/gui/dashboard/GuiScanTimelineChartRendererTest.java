package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiScanTimelineChartRendererTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private Graphics2D graphics() {
        return new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    private ScanResult scanResult(String ip) {
        return new ScanResult(ip, ip, new HashMap<>(), "Linux");
    }

    @Test void paint_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> GuiScanTimelineChartRenderer.paint(graphics(), Color.BLACK, List.of(), 200, 100));
    }

    @Test void paint_zeroSize_doesNotThrow() {
        assertDoesNotThrow(() -> GuiScanTimelineChartRenderer.paint(graphics(), Color.BLACK, List.of(), 0, 0));
    }

    @Test void paint_withEntries_doesNotThrow() {
        ScanHistory.getInstance().clear();
        ScanHistory.getInstance().add("test1", List.of(scanResult("1.1.1.1")));
        ScanHistory.getInstance().add("test2", List.of(scanResult("1.1.1.2"), scanResult("1.1.1.3")));
        List<ScanHistory.Entry> entries = ScanHistory.getInstance().getAll();
        assertDoesNotThrow(() -> GuiScanTimelineChartRenderer.paint(graphics(), Color.BLACK, entries, 200, 100));
        ScanHistory.getInstance().clear();
    }

    @Test void paint_allEmptyResultEntries_doesNotThrow() {
        ScanHistory.getInstance().clear();
        ScanHistory.getInstance().add("empty", List.of());
        assertDoesNotThrow(() -> GuiScanTimelineChartRenderer.paint(
                graphics(), Color.BLACK, ScanHistory.getInstance().getAll(), 200, 100));
        ScanHistory.getInstance().clear();
    }

    @Test void chart_construct_and_update_doesNotThrow() {
        GuiScanTimelineChart chart = new GuiScanTimelineChart(Color.BLACK, List.of());
        assertDoesNotThrow(() -> chart.update(List.of()));
    }

    @Test void chart_nullEntries_doesNotThrow() {
        assertDoesNotThrow(() -> new GuiScanTimelineChart(Color.BLACK, null));
    }

    @Test void chart_updateNull_doesNotThrow() {
        GuiScanTimelineChart chart = new GuiScanTimelineChart(Color.BLACK, List.of());
        assertDoesNotThrow(() -> chart.update(null));
    }

    @Test void chart_hasPreferredSize() {
        GuiScanTimelineChart chart = new GuiScanTimelineChart(Color.BLACK, List.of());
        assertTrue(chart.getPreferredSize().height > 0);
    }

    @Test void maxBars_scalesWithWidth() {
        assertEquals(50, GuiScanTimelineChartRenderer.maxBars(200));
        assertEquals(1, GuiScanTimelineChartRenderer.maxBars(0));
    }

    @Test void paint_moreEntriesThanFit_staysWithinWidth() {
        ScanHistory.getInstance().clear();
        for (int i = 0; i < ScanHistory.MAX_HISTORY; i++)
            ScanHistory.getInstance().add("s" + i, List.of(scanResult("1.1.1." + i)));
        BufferedImage img = new BufferedImage(20, 100, BufferedImage.TYPE_INT_ARGB);
        assertDoesNotThrow(() -> GuiScanTimelineChartRenderer.paint(img.createGraphics(), Color.BLACK,
                ScanHistory.getInstance().getAll(), 20, 100));
        ScanHistory.getInstance().clear();
    }
}
