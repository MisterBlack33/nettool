package main.java.networktool.gui.panels.bandwidth;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.probe.BandwidthHistoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiBandwidthHistoryPanelTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void show_unreachableHost_doesNotThrow() {
        assertDoesNotThrow(() -> GuiBandwidthHistoryPanel.show(new GuiOutputPanel(), "192.0.2.1"));
    }

    @Test void show_unreachableHost_doesNotRecordHistory() {
        BandwidthHistoryStore.getInstance().clear("192.0.2.2");
        GuiBandwidthHistoryPanel.show(new GuiOutputPanel(), "192.0.2.2");
        assertTrue(BandwidthHistoryStore.getInstance().getHistory("192.0.2.2").isEmpty());
    }
}
