package main.java.networktool.gui.panels.bandwidth;

import main.java.networktool.gui.components.BandwidthHistoryChart;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.probe.BandwidthHistoryStore;
import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.util.StatusTags;

import javax.swing.*;
import java.awt.*;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Test-Suite-Panel (Menü-ID 28): misst Bandbreite über {@link BandwidthTester}
 * (unverändert) und persistiert das Ergebnis in {@link BandwidthHistoryStore},
 * dann zeigt den Verlauf über {@link BandwidthHistoryChart}.
 */
public final class GuiBandwidthHistoryPanel {

    private GuiBandwidthHistoryPanel() {}

    public static void show(GuiOutputPanel output, String ip) {
        double down = BandwidthTester.testDownload(ip);
        double up   = BandwidthTester.testUpload(ip);
        if (down >= 0 && up >= 0) BandwidthHistoryStore.getInstance().record(ip, down, up);

        SwingUtilities.invokeLater(() -> {
            output.appendText("\n  Bandbreiten-Verlauf – " + ip + "\n", ACCENT);
            output.appendText(down >= 0 && up >= 0
                    ? "  " + StatusTags.OK + String.format("  ↓ %.2f Mbps  ↑ %.2f Mbps%n", down, up)
                    : "  " + StatusTags.FEHLER + " Messung fehlgeschlagen\n",
                    down >= 0 && up >= 0 ? ACCENT2 : WARN);

            JPanel chart = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    BandwidthHistoryChart.paint((Graphics2D) g, getBackground(),
                            BandwidthHistoryStore.getInstance().getHistory(ip), getWidth(), getHeight());
                }
            };
            chart.setBackground(BG);
            chart.setPreferredSize(new Dimension(0, 140));

            JTextPane pane = output.getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(chart);
            output.appendText("\n", FG);
        });
    }
}
