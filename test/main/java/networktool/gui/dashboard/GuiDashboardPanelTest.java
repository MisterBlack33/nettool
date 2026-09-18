package main.java.networktool.gui.dashboard;

import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiDashboardPanelTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void show_doesNotThrow() {
        GuiOutputPanel output = new GuiOutputPanel();
        assertDoesNotThrow(() -> GuiDashboardPanel.show(output));
    }
}
