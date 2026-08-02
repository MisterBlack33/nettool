// test/main/java/networktool/gui/map/MapLegendTest.java
package main.java.networktool.gui.map;

import org.junit.jupiter.api.*;
import main.java.networktool.gui.map.MapLegend;
import static org.junit.jupiter.api.Assertions.*;

class MapLegendTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless","true"); }

    @Test void build_returnsPanel() { assertNotNull(MapLegend.build()); }
    @Test void build_hasChildren()  { assertTrue(MapLegend.build().getComponentCount() > 0); }
}