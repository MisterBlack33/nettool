package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.map.MapHeatmapSettings;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class GuiWorkstreamCActionsTest {

    @TempDir Path tmp;
    GuiOutputPanel output;

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach void setup() { output = new GuiOutputPanel(); }

    @AfterEach void teardown() {
        MapHeatmapSettings.getInstance().setEnabled(false);
        ScanHistory.getInstance().clear();
    }

    @Test void heatmapToggle_flipsSetting() {
        GuiMapHeatmapActions.toggle(output);
        assertTrue(MapHeatmapSettings.getInstance().isEnabled());
        GuiMapHeatmapActions.toggle(output);
        assertFalse(MapHeatmapSettings.getInstance().isEnabled());
    }

    @Test void exportSvg_writesSvgFile() throws IOException {
        Path f = GuiMapExportActions.exportSvg(tmp, output);
        assertTrue(Files.readString(f).startsWith("<svg"));
    }

    @Test void exportGraphml_writesGraphmlFile() throws IOException {
        Path f = GuiMapExportActions.exportGraphml(tmp, output);
        assertTrue(Files.readString(f).contains("<graphml"));
    }

    @Test void pdfExport_writesPdf() throws IOException {
        Path f = GuiPdfExportActions.export(tmp, List.of(new HostResult("1.1.1.1", "h", "Linux")), output);
        assertTrue(new String(Files.readAllBytes(f), java.nio.charset.StandardCharsets.ISO_8859_1)
                .startsWith("%PDF-1.4"));
    }

    @Test void timeline_emptyHistory_doesNotThrow() {
        ScanHistory.getInstance().clear();
        assertDoesNotThrow(() -> GuiScanTimelineActions.show(output));
    }

    @Test void timeline_withHistory_doesNotThrow() {
        ScanHistory.getInstance().add("s", List.of(new ScanResult("1.1.1.1", "h", new HashMap<>(), "Linux")));
        assertDoesNotThrow(() -> GuiScanTimelineActions.show(output));
    }
}
