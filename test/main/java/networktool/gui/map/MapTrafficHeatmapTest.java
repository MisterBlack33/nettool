package main.java.networktool.gui.map;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapTrafficHeatmapTest {

    @Test void colorFor_zero_isLow() {
        assertEquals(new Color(0x30, 0x90, 0x40), MapTrafficHeatmap.colorFor(0.0));
    }

    @Test void colorFor_max_isHigh() {
        assertEquals(new Color(0xE0, 0x30, 0x30), MapTrafficHeatmap.colorFor(1.0));
    }

    @Test void colorFor_mid_isMidColor() {
        assertEquals(new Color(0xE0, 0xB0, 0x20), MapTrafficHeatmap.colorFor(0.5));
    }

    @Test void colorFor_clampsBelowZero() {
        assertEquals(MapTrafficHeatmap.colorFor(0.0), MapTrafficHeatmap.colorFor(-5.0));
    }

    @Test void colorFor_clampsAboveOne() {
        assertEquals(MapTrafficHeatmap.colorFor(1.0), MapTrafficHeatmap.colorFor(5.0));
    }

    @Test void levelFor_emptyMap_zero() {
        assertEquals(0.0, MapTrafficHeatmap.levelFor("1.1.1.1", Map.of()));
    }

    @Test void levelFor_nullMap_zero() {
        assertEquals(0.0, MapTrafficHeatmap.levelFor("1.1.1.1", null));
    }

    @Test void levelFor_nullIp_zero() {
        assertEquals(0.0, MapTrafficHeatmap.levelFor(null, Map.of("1.1.1.1", 10L)));
    }

    @Test void levelFor_maxEntry_isOne() {
        Map<String, Long> traffic = Map.of("1.1.1.1", 100L, "1.1.1.2", 50L);
        assertEquals(1.0, MapTrafficHeatmap.levelFor("1.1.1.1", traffic));
    }

    @Test void levelFor_halfOfMax() {
        Map<String, Long> traffic = Map.of("1.1.1.1", 100L, "1.1.1.2", 50L);
        assertEquals(0.5, MapTrafficHeatmap.levelFor("1.1.1.2", traffic));
    }

    @Test void levelFor_unknownIp_zero() {
        Map<String, Long> traffic = Map.of("1.1.1.1", 100L);
        assertEquals(0.0, MapTrafficHeatmap.levelFor("9.9.9.9", traffic));
    }
}
