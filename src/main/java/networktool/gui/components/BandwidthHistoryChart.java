package main.java.networktool.gui.components;

import main.java.networktool.logic.analysis.probe.BandwidthHistoryEntry;

import java.awt.*;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Zeichnet den Down-/Up-Mbps-Verlauf einer {@link BandwidthHistoryEntry}-Liste.
 * Kein State, keine Swing-Komponente — Stil analog {@code PingGraphRenderer}.
 */
public final class BandwidthHistoryChart {

    private static final double MIN_SCALE_MBPS = 1.0;

    private BandwidthHistoryChart() {}

    public static void paint(Graphics2D g2, Color bg, List<BandwidthHistoryEntry> history, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        drawGrid(g2, w, h);
        if (history.isEmpty()) return;

        double max = maxMbps(history);
        drawLine(g2, history, w, h, max, ACCENT2, BandwidthHistoryEntry::downMbps);
        drawLine(g2, history, w, h, max, ACCENT, BandwidthHistoryEntry::upMbps);
    }

    private static void drawLine(Graphics2D g2, List<BandwidthHistoryEntry> history, int w, int h,
                                 double max, Color col, java.util.function.ToDoubleFunction<BandwidthHistoryEntry> value) {
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.5f));
        int stepX = Math.max(1, w / Math.max(1, history.size() - 1));
        int prevX = -1, prevY = -1;
        for (int i = 0; i < history.size(); i++) {
            double v = value.applyAsDouble(history.get(i));
            int cx = i * stepX;
            int cy = h - 4 - (int) (Math.min(v, max) * (h - 8) / max);
            if (prevX >= 0) g2.drawLine(prevX, prevY, cx, cy);
            g2.fillOval(cx - 2, cy - 2, 4, 4);
            prevX = cx; prevY = cy;
        }
    }

    private static double maxMbps(List<BandwidthHistoryEntry> history) {
        double max = MIN_SCALE_MBPS;
        for (BandwidthHistoryEntry e : history)
            max = Math.max(max, Math.max(e.downMbps(), e.upMbps()));
        return max;
    }

    private static void drawGrid(Graphics2D g2, int w, int h) {
        g2.setColor(isDark() ? new Color(0x1E, 0x2D, 0x3D) : new Color(0xE0, 0xDE, 0xD8));
        for (int y = h / 4; y < h; y += h / 4) g2.drawLine(0, y, w, y);
    }
}
