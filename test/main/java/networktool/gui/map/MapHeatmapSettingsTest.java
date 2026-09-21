package main.java.networktool.gui.map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class MapHeatmapSettingsTest {

    MapHeatmapSettings s = MapHeatmapSettings.getInstance();

    @AfterEach void reset() { s.setEnabled(false); }

    @Test void toggle_flipsAndReturnsNewState() {
        s.setEnabled(false);
        assertTrue(s.toggle());
        assertTrue(s.isEnabled());
        assertFalse(s.toggle());
    }

    @Test void singleton() { assertSame(s, MapHeatmapSettings.getInstance()); }
}
