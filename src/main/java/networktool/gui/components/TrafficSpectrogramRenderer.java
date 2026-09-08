package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficSample;

import java.awt.*;
import java.util.List;

/**
 * Zeichnet den Traffic als Spektrogramm-artige Wasserfall-Grafik: eine Spalte
 * je Sample, Farbintensität nach Traffic-Volumen (dunkelblau → magenta).
 * Keine echte Frequenzanalyse — Bins werden deterministisch aus dem
 * Sample-Wert abgeleitet, rein zur bildlichen Darstellung des Datenstroms.
 */
public final class TrafficSpectrogramRenderer {

    private static final int  BINS      = 48;
    private static final long MIN_SCALE = 50L;

    private TrafficSpectrogramRenderer() {}

    public static void paint(Graphics2D g2, List<TrafficSample> samples, int w, int h) {
        g2.setColor(new Color(0x05, 0x02, 0x30));
        g2.fillRect(0, 0, w, h);
        if (samples.isEmpty() || w <= 0 || h <= 0) return;

        long max  = maxTotal(samples);
        int colW  = Math.max(1, w / samples.size());
        int binH  = Math.max(1, h / BINS);

        for (int i = 0; i < samples.size(); i++) {
            double level = levelOf(samples.get(i), max);
            int x = i * colW;
            for (int b = 0; b < BINS; b++) {
                g2.setColor(colorFor(binIntensity(level, b, i)));
                g2.fillRect(x, h - (b + 1) * binH, colW, binH);
            }
        }
    }

    private static double levelOf(TrafficSample s, long max) {
        return Math.min(1.0, (s.rxDelta() + s.txDelta()) / (double) max);
    }

    /** Deterministische Pseudo-Textur: höhere Bins fallen mit Traffic-Level und Position ab. */
    private static double binIntensity(double level, int bin, int sampleIndex) {
        double falloff = Math.max(0, level - (bin / (double) BINS));
        double wobble  = 0.5 + 0.5 * Math.sin(bin * 0.7 + sampleIndex * 0.35);
        return Math.min(1.0, falloff * wobble * 1.6);
    }

    private static Color colorFor(double intensity) {
        float hue        = (float) (0.75 - intensity * 0.35); // 0.75=blau .. 0.40=magenta
        float saturation = (float) Math.min(1.0, 0.5 + intensity * 0.6);
        float brightness = (float) Math.min(1.0, 0.15 + intensity * 0.85);
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private static long maxTotal(List<TrafficSample> samples) {
        long max = 0;
        for (TrafficSample s : samples) max = Math.max(max, s.rxDelta() + s.txDelta());
        return Math.max(max, MIN_SCALE);
    }
}