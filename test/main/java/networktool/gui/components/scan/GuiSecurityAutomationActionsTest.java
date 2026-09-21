package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.security.RogueDhcpDetector;
import main.java.networktool.logic.analysis.security.ScanSecurityHook;
import main.java.networktool.logic.analysis.security.TlsCertScheduler;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GuiSecurityAutomationActionsTest {

    GuiOutputPanel output;
    GuiInputPanel input;

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach void setup() {
        output = new GuiOutputPanel();
        input  = new GuiInputPanel(new JLabel(), output);
        ScanSecurityHook.getInstance().setEnabled(false);
    }

    @AfterEach void teardown() {
        TlsCertScheduler.getInstance().stop();
        ScanSecurityHook.getInstance().setEnabled(false);
    }

    @Test void parseOr_valid()        { assertEquals(5, GuiSecurityAutomationActions.parseOr(" 5 ", 9)); }
    @Test void parseOr_blank()        { assertEquals(9, GuiSecurityAutomationActions.parseOr("", 9)); }
    @Test void parseOr_negative()     { assertEquals(9, GuiSecurityAutomationActions.parseOr("-3", 9)); }
    @Test void parseOr_nonNumeric()   { assertEquals(9, GuiSecurityAutomationActions.parseOr("abc", 9)); }

    @Test void toggleScanHook_twice_restoresState() {
        GuiSecurityAutomationActions.toggleScanHook(output);
        assertTrue(ScanSecurityHook.getInstance().isEnabled());
        GuiSecurityAutomationActions.toggleScanHook(output);
        assertFalse(ScanSecurityHook.getInstance().isEnabled());
    }

    @Test void toggleTlsScheduler_inactive_onlyAsksInterval() {
        assertDoesNotThrow(() -> GuiSecurityAutomationActions.toggleTlsScheduler(input, output));
        assertFalse(TlsCertScheduler.getInstance().isActive());
    }

    @Test void startTlsScheduler_thenToggle_stops() {
        GuiSecurityAutomationActions.startTlsScheduler(output, "1");
        assertTrue(TlsCertScheduler.getInstance().isActive());
        GuiSecurityAutomationActions.toggleTlsScheduler(input, output);
        assertFalse(TlsCertScheduler.getInstance().isActive());
    }

    @Test void toggleRogueDhcp_inactive_onlyAsksInterval() {
        assertDoesNotThrow(() -> GuiSecurityAutomationActions.toggleRogueDhcp(input, output));
    }

    @Test void resolveTrusted_explicitList_filtersInvalid() {
        assertEquals(Set.of("10.0.0.1", "10.0.0.2"),
                GuiSecurityAutomationActions.resolveTrusted("10.0.0.1, x, 10.0.0.2", () -> "9.9.9.9"));
    }

    @Test void resolveTrusted_blank_usesGateway() {
        assertEquals(Set.of("192.168.1.1"),
                GuiSecurityAutomationActions.resolveTrusted("  ", () -> "192.168.1.1"));
    }

    @Test void resolveTrusted_nullAndNoGateway_empty() {
        assertTrue(GuiSecurityAutomationActions.resolveTrusted(null, () -> null).isEmpty());
    }

    @Test void startRogueDhcp_noTrusted_failsAndStaysInactive() {
        assertFalse(GuiSecurityAutomationActions.startRogueDhcp(output, 60, Set.of()));
        assertFalse(RogueDhcpDetector.getInstance().isActive());
    }
}
