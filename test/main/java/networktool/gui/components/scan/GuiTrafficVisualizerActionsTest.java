package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.visualize.TrafficVisualizer;
import org.junit.jupiter.api.*;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiTrafficVisualizerActionsTest {

    GuiOutputPanel output;
    GuiInputPanel input;

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach void setup() {
        output = new GuiOutputPanel();
        input  = new GuiInputPanel(new JLabel(), output);
        TrafficVisualizer.getInstance().stop();
    }

    @AfterEach void teardown() {
        TrafficVisualizer.getInstance().stop();
    }

    @Test void toggle_whenInactive_doesNotThrow_andDoesNotAutoStart() {
        assertDoesNotThrow(() -> GuiTrafficVisualizerActions.toggle(input, output));
        assertFalse(TrafficVisualizer.getInstance().isActive());
    }

    @Test void toggle_whenActive_stopsVisualizer() {
        TrafficVisualizer.getInstance().start("eth0");
        assertTrue(TrafficVisualizer.getInstance().isActive());
        GuiTrafficVisualizerActions.toggle(input, output);
        assertFalse(TrafficVisualizer.getInstance().isActive());
    }

    @Test void toggle_whenActive_doesNotThrow() {
        TrafficVisualizer.getInstance().start("wlan0");
        assertDoesNotThrow(() -> GuiTrafficVisualizerActions.toggle(input, output));
    }

    @Test void startWithInterface_setsActiveAndInterface() {
        GuiTrafficVisualizerActions.startWithInterface(output, "eth1");
        assertTrue(TrafficVisualizer.getInstance().isActive());
        assertEquals("eth1", TrafficVisualizer.getInstance().getActiveInterface());
    }

    @Test void startWithInterface_doesNotThrow() {
        assertDoesNotThrow(() -> GuiTrafficVisualizerActions.startWithInterface(output, "eth2"));
    }

    @Test void startWithInterface_embedsPanel_doesNotThrowEvenTwice() {
        assertDoesNotThrow(() -> {
            GuiTrafficVisualizerActions.startWithInterface(output, "eth3");
            GuiTrafficVisualizerActions.startWithInterface(output, "eth3");
        });
    }
}