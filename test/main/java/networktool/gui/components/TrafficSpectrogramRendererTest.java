package main.java.networktool.gui.components;

import main.java.networktool.logic.visualize.TrafficSample;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrafficSpectrogramRendererTest {

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    private Graphics2D graphics() {
        return new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    @Test void paint_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), List.of(), 200, 100));
    }

    @Test void paint_singleSample_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 10, 5));
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 200, 100));
    }

    @Test void paint_multipleSamples_doesNotThrow() {
        List<TrafficSample> samples = List.of(
                new TrafficSample(1000L, 10, 5),
                new TrafficSample(2000L, 40, 60),
                new TrafficSample(3000L, 0, 0));
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 200, 100));
    }

    @Test void paint_zeroWidthHeight_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 10, 5));
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 0, 0));
    }

    @Test void paint_allZeroDeltas_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 0, 0));
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 200, 100));
    }

    @Test void paint_highTrafficLevel_doesNotThrow() {
        List<TrafficSample> samples = List.of(new TrafficSample(1000L, 100_000, 100_000));
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 200, 100));
    }

    @Test void paint_largeSampleList_doesNotThrow() {
        List<TrafficSample> samples = java.util.stream.IntStream.range(0, 300)
                .mapToObj(i -> new TrafficSample(i * 1000L, i, i * 2L))
                .toList();
        assertDoesNotThrow(() -> TrafficSpectrogramRenderer.paint(graphics(), samples, 200, 100));
    }
}