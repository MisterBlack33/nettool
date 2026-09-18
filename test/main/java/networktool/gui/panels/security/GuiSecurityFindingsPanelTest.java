package main.java.networktool.gui.panels.security;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.security.SecurityFindingsCollector;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiSecurityFindingsPanelTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @AfterEach void clear() { SecurityFindingsCollector.getInstance().clear(); }

    @Test void show_doesNotThrow() {
        GuiOutputPanel output = new GuiOutputPanel();
        assertDoesNotThrow(() -> GuiSecurityFindingsPanel.show(output));
    }
}
