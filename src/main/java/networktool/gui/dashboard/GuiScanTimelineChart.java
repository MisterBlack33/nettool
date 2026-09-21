package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanHistory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Zeigt {@link GuiScanTimelineChartRenderer}-Balken als eingebettetes Panel (analog GuiScanDeltaChart). */
public final class GuiScanTimelineChart extends JPanel {

    private static final int PREFERRED_HEIGHT = 140;

    private final Color bg;
    private volatile List<ScanHistory.Entry> entries;

    public GuiScanTimelineChart(Color bg, List<ScanHistory.Entry> entries) {
        this.bg = bg;
        this.entries = entries != null ? entries : List.of();
        setBackground(bg);
        setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));
    }

    public void update(List<ScanHistory.Entry> newEntries) {
        this.entries = newEntries != null ? newEntries : List.of();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GuiScanTimelineChartRenderer.paint(g2, bg, entries, getWidth(), getHeight());
        g2.dispose();
    }
}
