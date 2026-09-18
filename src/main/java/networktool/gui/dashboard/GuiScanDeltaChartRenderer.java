package main.java.networktool.gui.dashboard;

import main.java.networktool.logic.scan.schedule.ScanDelta;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

import static main.java.networktool.theme.GuiTheme.*;

/** Zeichnet {@link ScanDelta.DeltaEntry}-Zählungen als einfaches Balkendiagramm. */
public final class GuiScanDeltaChartRenderer {

    private GuiScanDeltaChartRenderer() {}

    public static void paint(Graphics2D g2, Color bg, List<ScanDelta.DeltaEntry> entries, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        if (w <= 0 || h <= 0) return;

        Map<ScanDelta.ChangeType, Long> counts = countByType(entries);
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        if (max == 0) max = 1;

        ScanDelta.ChangeType[] types = {
                ScanDelta.ChangeType.NEU, ScanDelta.ChangeType.WEG,
                ScanDelta.ChangeType.OS_WECHSEL, ScanDelta.ChangeType.PORT_AENDERUNG
        };
        int barW = w / types.length;
        for (int i = 0; i < types.length; i++) {
            drawBar(g2, i * barW, barW, h, counts.getOrDefault(types[i], 0L), max, colorFor(types[i]));
        }
    }

    private static Map<ScanDelta.ChangeType, Long> countByType(List<ScanDelta.DeltaEntry> entries) {
        Map<ScanDelta.ChangeType, Long> counts = new EnumMap<>(ScanDelta.ChangeType.class);
        for (ScanDelta.DeltaEntry e : entries)
            counts.merge(e.type, 1L, Long::sum);
        return counts;
    }

    private static void drawBar(Graphics2D g2, int x, int barW, int h, long value, long max, Color col) {
        int pad = Math.max(2, barW / 8);
        int innerW = Math.max(1, barW - pad * 2);
        int barH = (int) Math.round((h - 20) * (value / (double) max));
        g2.setColor(col);
        g2.fillRect(x + pad, h - 20 - barH, innerW, barH);
        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        g2.setColor(FG_DIM);
        g2.drawString(String.valueOf(value), x + pad, h - 6);
    }

    private static Color colorFor(ScanDelta.ChangeType type) {
        return switch (type) {
            case NEU            -> ACCENT2;
            case WEG            -> WARN;
            case OS_WECHSEL     -> INFO;
            case PORT_AENDERUNG -> ACCENT;
            default             -> FG_DIM;
        };
    }
}
