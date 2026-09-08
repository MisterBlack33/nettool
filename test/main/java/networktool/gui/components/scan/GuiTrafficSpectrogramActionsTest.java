package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.visualize.TrafficVisualizer;
import org.junit.jupiter.api.*;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiTrafficSpectrogramActionsTest {

    GuiOutputPanel output;
    GuiInputPanel input;

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach
    void setup() {
        output = new GuiOutputPanel();
        input  = new GuiInputPanel(new JLabel(), output);
        TrafficVisualizer.getInstance().stop();
    }

    @AfterEach
    void teardown() { TrafficVisualizer.getInstance().stop(); }

    @Test void toggle_whenInactive_doesNotThrow_andDoesNotAutoStart() {
        assertDoesNotThrow(() -> GuiTrafficSpectrogramActions.toggle(input, output));
        assertFalse(TrafficVisualizer.getInstance().isActive());
    }

    @Test void toggle_whenActive_stopsVisualizer() {
        TrafficVisualizer.getInstance().start("eth0");
        assertTrue(TrafficVisualizer.getInstance().isActive());
        GuiTrafficSpectrogramActions.toggle(input, output);
        assertFalse(TrafficVisualizer.getInstance().isActive());
    }

    @Test void toggle_whenActive_doesNotThrow() {
        TrafficVisualizer.getInstance().start("wlan0");
        assertDoesNotThrow(() -> GuiTrafficSpectrogramActions.toggle(input, output));
    }

    @Test void startWithInterface_setsActiveAndInterface() {
        GuiTrafficSpectrogramActions.startWithInterface(output, "eth1");
        assertTrue(TrafficVisualizer.getInstance().isActive());
        assertEquals("eth1", TrafficVisualizer.getInstance().getActiveInterface());
    }

    @Test void startWithInterface_doesNotThrow() {
        assertDoesNotThrow(() -> GuiTrafficSpectrogramActions.startWithInterface(output, "eth2"));
    }

    @Test void startWithInterface_embedsPanel_doesNotThrowEvenTwice() {
        assertDoesNotThrow(() -> {
            GuiTrafficSpectrogramActions.startWithInterface(output, "eth3");
            GuiTrafficSpectrogramActions.startWithInterface(output, "eth3");
        });
    }
}