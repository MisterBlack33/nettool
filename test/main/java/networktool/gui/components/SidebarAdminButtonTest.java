package main.java.networktool.gui.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * showPasswordDialog() öffnet einen blockierenden modalen JDialog und wird
 * hier bewusst NICHT automatisiert getestet (analog GuiWindowActionsTest —
 * kein UI-Test-Framework für blockierende Swing-Dialoge vorhanden).
 */
class SidebarAdminButtonTest {

    @BeforeAll
    static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void build_returnsButtonWithLockedLabel() {
        JButton btn = SidebarAdminButton.build(() -> {});
        assertEquals("GET ADMIN", btn.getText());
    }

    @Test void build_buttonIsInitiallyEnabled() {
        JButton btn = SidebarAdminButton.build(() -> {});
        assertTrue(btn.isEnabled());
    }

    @Test void build_hasActionListenerRegistered() {
        JButton btn = SidebarAdminButton.build(() -> {});
        assertTrue(btn.getActionListeners().length > 0);
    }

    @Test void build_doesNotThrow() {
        assertDoesNotThrow(() -> SidebarAdminButton.build(() -> {}));
    }
}