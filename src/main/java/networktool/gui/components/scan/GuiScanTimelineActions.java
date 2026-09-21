package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.dashboard.GuiScanTimelineChart;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.schedule.ScanHistory;

import javax.swing.*;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "Scan-Zeitreihe": Hosts je Scan der Session (Menü-ID "41"). */
public final class GuiScanTimelineActions {

    private GuiScanTimelineActions() {}

    public static void show(GuiOutputPanel output) {
        if (ScanHistory.getInstance().size() == 0) {
            output.appendText("  Kein Scan in dieser Session.\n", FG_DIM);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            output.appendText("\nScan-Zeitreihe (Hosts je Scan)\n\n", ACCENT);
            GuiScanTimelineChart chart = new GuiScanTimelineChart(BG, ScanHistory.getInstance().getAll());
            JTextPane pane = output.getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(chart);
            output.appendText("\n", FG);
        });
    }
}
