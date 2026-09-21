package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GuiIpv6ScanActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void scanAndReport_validCidr_runsScanner() {
        AtomicReference<String> received = new AtomicReference<>();
        boolean ran = GuiIpv6ScanActions.scanAndReport("2001:db8::/64", cidr -> {
            received.set(cidr);
            return List.of(new ScanResult("2001:db8:0:0:0:0:0:1", "h", Map.of(), "Linux/Unix"));
        }, new GuiOutputPanel());
        assertTrue(ran);
        assertEquals("2001:db8::/64", received.get());
    }

    @Test void scanAndReport_invalidCidr_skipsScanner() {
        AtomicReference<String> received = new AtomicReference<>();
        boolean ran = GuiIpv6ScanActions.scanAndReport("10.0.0.0/24", cidr -> {
            received.set(cidr);
            return List.of();
        }, new GuiOutputPanel());
        assertFalse(ran);
        assertNull(received.get());
    }

    @Test void handleScan_registersPrompt_withoutThrowing() {
        GuiOutputPanel output = new GuiOutputPanel();
        assertDoesNotThrow(() -> GuiIpv6ScanActions.handleScan(new GuiInputPanel(new JLabel(), output), output, null));
    }
}
