package main.java.networktool.gui;

import java.awt.*;
import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;

/** Zeichnet die Latenz-Verlaufskurve für {@link HostPingTab}. */
final class PingGraphRenderer {

    private PingGraphRenderer() {}

    static void paint(Graphics2D g2, Color bg, List<Long> history, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);

        g2.setColor(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x1A) : new Color(0xE0, 0xDE, 0xD8));
        for (int y = h / 4; y < h; y += h / 4) g2.drawLine(0, y, w, y);

        long max = history.stream().filter(v -> v >= 0).mapToLong(v -> v).max().orElse(100);
        if (max < 50) max = 50;

        int barW = Math.max(2, w / history.size());
        int prevX = -1, prevY = -1;

        for (int i = 0; i < history.size(); i++) {
            long v = history.get(i);
            int cx = i * barW + barW / 2;

            if (v < 0) {
                g2.setColor(WARN);
                g2.fillOval(cx - 3, h - 10, 6, 6);
                prevX = prevY = -1;
                continue;
            }

            int cy = Math.max(4, h - 4 - (int) (v * (h - 8) / max));
            Color col = v < 20 ? ACCENT2 : v < 100 ? ACCENT : v < 300 ? new Color(0xFF, 0xD0, 0x50) : WARN;
            g2.setColor(col);
            if (prevX >= 0) {
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(prevX, prevY, cx, cy);
            }
            g2.fillOval(cx - 3, cy - 3, 6, 6);
            prevX = cx; prevY = cy;
        }
    }
}