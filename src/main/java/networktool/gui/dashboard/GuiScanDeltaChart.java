package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanDelta;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Zeigt {@link GuiScanDeltaChartRenderer}-Balken als eingebettetes Panel (analog TrafficVisualizerPanel). */
public final class GuiScanDeltaChart extends JPanel {

    private final Color bg;
    private volatile List<ScanDelta.DeltaEntry> entries;

    public GuiScanDeltaChart(Color bg, List<ScanDelta.DeltaEntry> entries) {
        this.bg = bg;
        this.entries = entries != null ? entries : List.of();
        setBackground(bg);
        setPreferredSize(new Dimension(0, 140));
    }

    public void update(List<ScanDelta.DeltaEntry> newEntries) {
        this.entries = newEntries != null ? newEntries : List.of();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GuiScanDeltaChartRenderer.paint(g2, bg, entries, getWidth(), getHeight());
        g2.dispose();
    }
}
