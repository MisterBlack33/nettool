package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficSample;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrafficWaveformRendererTest {

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    private Graphics2D graphics() {
        return new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    @Test void paint_emptyList_doesNotThrow() {
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, List.of(), 200, 100));
    }

    @Test void paint_singleSample_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 10, 5));
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 200, 100));
    }

    @Test void paint_multipleSamples_doesNotThrow() {
        List<TrafficSample> samples = List.of(
                new TrafficSample(1000L, 10, 5),
                new TrafficSample(2000L, 40, 60),
                new TrafficSample(3000L, 0, 0));
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 200, 100));
    }

    @Test void paint_zeroWidthHeight_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 10, 5));
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 0, 0));
    }

    @Test void paint_belowMinScale_doesNotOverflowBar() {
        // Werte deutlich unter der Mindestskala (50) — Balken darf Panel nicht sprengen
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 2, 1));
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 200, 100));
    }

    @Test void paint_allNegativeOrZeroDeltas_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 0, 0));
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 200, 100));
    }

    @Test void paint_largeSampleList_doesNotThrow() {
        List<TrafficSample> samples = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> new TrafficSample(i * 1000L, i, i * 2L))
                .toList();
        assertDoesNotThrow(() ->
                TrafficWaveformRenderer.paint(graphics(), Color.BLACK, samples, 200, 100));
    }

    @Test void trafficSample_fieldsAccessible() {
        TrafficSample s = new TrafficSample(123L, 4, 5);
        assertEquals(123L, s.timestampMs());
        assertEquals(4L, s.rxDelta());
        assertEquals(5L, s.txDelta());
    }
}