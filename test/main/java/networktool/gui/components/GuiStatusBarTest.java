package main.java.networktool.gui.components;

import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class GuiStatusBarTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private GuiStatusBar bar;

    @BeforeEach void setup() { bar = new GuiStatusBar(); }

    @Test void initialLabelText_isBereit() {
        assertEquals("Bereit", bar.getLabel().getText());
    }

    @Test void buildPanel_returnsNonNull() {
        assertNotNull(bar.buildPanel());
    }

    @Test void buildPanel_containsLabelAndBrand() {
        JPanel panel = bar.buildPanel();
        assertTrue(panel.getComponentCount() >= 2);
    }

    @Test void set_updatesLabelTextAndColor() throws Exception {
        bar.set("Läuft…", Color.RED);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Läuft…", bar.getLabel().getText());
        assertEquals(Color.RED, bar.getLabel().getForeground());
    }

    @Test void set_calledTwice_lastValueWins() throws Exception {
        bar.set("Erstens", Color.BLUE);
        bar.set("Zweitens", Color.GREEN);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Zweitens", bar.getLabel().getText());
        assertEquals(Color.GREEN, bar.getLabel().getForeground());
    }
}
