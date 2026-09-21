package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanHistory;

import java.awt.*;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Zeichnet Host-Anzahl je {@link ScanHistory.Entry} als Balkendiagramm (ältester links).
 * Bei mehr Einträgen als Platz vorhanden ist, werden die neuesten gezeigt.
 */
public final class GuiScanTimelineChartRenderer {

    private static final int MIN_BAR_WIDTH   = 4;
    private static final int LABEL_AREA      = 20;
    private static final int LABEL_BASELINE  = 6;
    private static final int LABEL_FONT_SIZE = 9;

    private GuiScanTimelineChartRenderer() {}

    /** @param entries neueste zuerst, wie {@code ScanHistory.getAll()} */
    public static void paint(Graphics2D g2, Color bg, List<ScanHistory.Entry> entries, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        if (entries.isEmpty() || w <= 0 || h <= 0) return;

        List<ScanHistory.Entry> visible = entries.subList(0, Math.min(entries.size(), maxBars(w)));
        int max = Math.max(1, visible.stream().mapToInt(e -> e.results.size()).max().orElse(1));
        int n = visible.size();
        int barW = Math.max(MIN_BAR_WIDTH, w / n);
        for (int i = 0; i < n; i++) {
            drawBar(g2, i * barW, barW, h, visible.get(n - 1 - i).results.size(), max);
        }
    }

    static int maxBars(int width) { return Math.max(1, width / MIN_BAR_WIDTH); }

    private static void drawBar(Graphics2D g2, int x, int barW, int h, int value, int max) {
        int pad    = Math.max(1, barW / 6);
        int innerW = Math.max(1, barW - pad * 2);
        int barH   = (int) Math.round((h - LABEL_AREA) * (value / (double) max));
        g2.setColor(ACCENT2);
        g2.fillRect(x + pad, h - LABEL_AREA - barH, innerW, barH);
        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, LABEL_FONT_SIZE));
        g2.setColor(FG_DIM);
        g2.drawString(String.valueOf(value), x + pad, h - LABEL_BASELINE);
    }
}
