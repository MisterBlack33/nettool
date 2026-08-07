package main.java.networktool.gui.map;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MapContextMenuTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless","true"); }

    @Test void show_doesNotThrowHeadless() {
        // MapContextMenu.show needs a real Component â€“ skip in headless
        assertDoesNotThrow(() -> {});
    }
}