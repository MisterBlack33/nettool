package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.host.ArpSighting;
import main.java.networktool.logic.scan.host.ArpSniffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiArpSnifferActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @AfterEach void stop() { ArpSniffer.getInstance().stop(); }

    @Test void describe_newHost() {
        String text = GuiArpSnifferActions.describe(new ArpSighting("10.0.0.1", "AA:BB", null));
        assertTrue(text.contains("Neuer Host") && text.contains("10.0.0.1"));
    }

    @Test void describe_macChange() {
        String text = GuiArpSnifferActions.describe(new ArpSighting("10.0.0.1", "NEW", "OLD"));
        assertTrue(text.contains("MAC-Wechsel") && text.contains("OLD") && text.contains("NEW"));
    }

    @Test void toggle_startsWhenInactive() {
        GuiArpSnifferActions.toggle(new GuiOutputPanel());
        assertTrue(ArpSniffer.getInstance().isActive());
    }

    @Test void toggle_stopsWhenActive() {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiArpSnifferActions.toggle(output);
        GuiArpSnifferActions.toggle(output);
        assertFalse(ArpSniffer.getInstance().isActive());
    }
}
