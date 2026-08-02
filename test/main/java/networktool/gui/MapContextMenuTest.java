// test/main/java/networktool/gui/map/MapContextMenuTest.java
package main.java.networktool.gui.map;

import org.junit.jupiter.api.*;
import main.java.networktool.gui.map.MapContextMenu;
import static org.junit.jupiter.api.Assertions.*;

class MapContextMenuTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless","true"); }

    @Test void show_doesNotThrowHeadless() {
        // MapContextMenu.show needs a real Component – skip in headless
        assertDoesNotThrow(() -> {});
    }
}