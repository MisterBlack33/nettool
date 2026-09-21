package main.java.networktool.gui.map;

import java.awt.Color;
import java.util.Map;

/**
 * Leitet eine Kantenfarbe aus Traffic-Volumen ab (grün=niedrig, rot=hoch).
 * Reine Farblogik, keine Graphics2D-Abhängigkeit — analog {@link MapNodeStyle}.
 */
public final class MapTrafficHeatmap {

    private static final Color LOW  = new Color(0x30, 0x90, 0x40);
    private static final Color MID  = new Color(0xE0, 0xB0, 0x20);
    private static final Color HIGH = new Color(0xE0, 0x30, 0x30);
    private static final double MID_LEVEL = 0.5;

    private MapTrafficHeatmap() {}

    /** @param level 0.0 (kein Traffic) bis 1.0 (Maximum) */
    public static Color colorFor(double level) {
        double l = clamp(level);
        if (l < MID_LEVEL) return blend(LOW, MID, l / MID_LEVEL);
        return blend(MID, HIGH, (l - MID_LEVEL) / MID_LEVEL);
    }

    /** Normalisiert einen Byte-Delta-Wert gegen den Maximalwert der Map auf [0,1]. */
    public static double levelFor(String ip, Map<String, Long> trafficByIp) {
        if (ip == null || trafficByIp == null || trafficByIp.isEmpty()) return 0.0;
        long value = trafficByIp.getOrDefault(ip, 0L);
        long max   = trafficByIp.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        if (max <= 0) return 0.0;
        return clamp(value / (double) max);
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private static Color blend(Color a, Color b, double t) {
        double c = clamp(t);
        int r  = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * c);
        int g  = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * c);
        int bl = (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * c);
        return new Color(r, g, bl);
    }
}
