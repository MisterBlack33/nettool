package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanDelta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiScanDeltaChartRendererTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private Graphics2D graphics() {
        return new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    private ScanDelta.DeltaEntry entry(ScanDelta.ChangeType type) {
        try {
            Constructor<ScanDelta.DeltaEntry> c = ScanDelta.DeltaEntry.class.getDeclaredConstructor(
                    String.class, String.class, ScanDelta.ChangeType.class, String.class);
            c.setAccessible(true);
            return c.newInstance("1.1.1.1", "host", type, "detail");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test void paint_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> GuiScanDeltaChartRenderer.paint(graphics(), Color.BLACK, List.of(), 200, 100));
    }

    @Test void paint_zeroSize_doesNotThrow() {
        assertDoesNotThrow(() -> GuiScanDeltaChartRenderer.paint(graphics(), Color.BLACK, List.of(), 0, 0));
    }

    @Test void paint_mixedEntries_doesNotThrow() {
        List<ScanDelta.DeltaEntry> entries = List.of(
                entry(ScanDelta.ChangeType.NEU),
                entry(ScanDelta.ChangeType.NEU),
                entry(ScanDelta.ChangeType.WEG),
                entry(ScanDelta.ChangeType.OS_WECHSEL),
                entry(ScanDelta.ChangeType.PORT_AENDERUNG));
        assertDoesNotThrow(() -> GuiScanDeltaChartRenderer.paint(graphics(), Color.BLACK, entries, 200, 100));
    }

    @Test void paint_singleType_doesNotThrow() {
        assertDoesNotThrow(() -> GuiScanDeltaChartRenderer.paint(
                graphics(), Color.BLACK, List.of(entry(ScanDelta.ChangeType.NEU)), 200, 100));
    }

    @Test void chart_construct_and_update_doesNotThrow() {
        GuiScanDeltaChart chart = new GuiScanDeltaChart(Color.BLACK, List.of());
        assertDoesNotThrow(() -> chart.update(List.of(entry(ScanDelta.ChangeType.NEU))));
    }

    @Test void chart_nullEntries_doesNotThrow() {
        assertDoesNotThrow(() -> new GuiScanDeltaChart(Color.BLACK, null));
    }

    @Test void chart_updateNull_doesNotThrow() {
        GuiScanDeltaChart chart = new GuiScanDeltaChart(Color.BLACK, List.of());
        assertDoesNotThrow(() -> chart.update((List<ScanDelta.DeltaEntry>) null));
    }

    @Test void chart_hasPreferredSize() {
        GuiScanDeltaChart chart = new GuiScanDeltaChart(Color.BLACK, List.of());
        assertTrue(chart.getPreferredSize().height > 0);
    }
}
