package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficSample;

import java.awt.*;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Zeichnet eine Traffic-Wellenform (rx/tx) auf ein {@link Graphics2D}.
 * Kein State, keine Swing-Komponente — analog {@code PingGraphRenderer}.
 */
public final class TrafficWaveformRenderer {

    private enum BarDirection { RX_UP, TX_DOWN }

    private static final long MIN_SCALE = 50L;

    private TrafficWaveformRenderer() {}

    public static void paint(Graphics2D g2, Color bg, List<TrafficSample> samples, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        drawGrid(g2, w, h);

        if (samples.isEmpty()) return;

        long max = maxDelta(samples);
        int barW = Math.max(1, w / samples.size());

        for (int i = 0; i < samples.size(); i++) {
            TrafficSample s = samples.get(i);
            int cx = i * barW + barW / 2;
            drawBar(g2, cx, barW, h, s.rxDelta(), max, ACCENT2, BarDirection.RX_UP);
            drawBar(g2, cx, barW, h, s.txDelta(), max, ACCENT, BarDirection.TX_DOWN);
        }
    }

    /** Mindestskala verhindert winzige Balken bei geringem Traffic. */
    private static long maxDelta(List<TrafficSample> samples) {
        long max = 0;
        for (TrafficSample s : samples) {
            max = Math.max(max, Math.max(s.rxDelta(), s.txDelta()));
        }
        return Math.max(max, MIN_SCALE);
    }

    private static void drawBar(Graphics2D g2, int cx, int barW, int h,
                                long value, long max, Color col, BarDirection direction) {
        if (value <= 0) return;
        int barH = (int) Math.min(h / 2.0, value * (h / 2.0) / max);
        int half = barW / 2;
        g2.setColor(col);
        if (direction == BarDirection.RX_UP) g2.fillRect(cx - half, h / 2 - barH, barW, barH);
        else                                 g2.fillRect(cx - half, h / 2, barW, barH);
    }

    private static void drawGrid(Graphics2D g2, int w, int h) {
        g2.setColor(gridColor());
        g2.drawLine(0, h / 2, w, h / 2);
        for (int y = h / 4; y < h; y += h / 4) g2.drawLine(0, y, w, y);
    }

    private static Color gridColor() {
        return isDark() ? new Color(0x1E, 0x2D, 0x3D) : new Color(0xE0, 0xDE, 0xD8);
    }
}
