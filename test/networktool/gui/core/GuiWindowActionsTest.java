package networktool.gui.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für GuiWindowActions.
 * confirmQuit()/toggleTheme() rufen JOptionPane bzw. GuiOutputPanel/GuiStatusBar
 * auf und werden hier bewusst NICHT getestet (blockierender Dialog headless nicht
 * automatisierbar ohne UI-Test-Framework, siehe JaCoCo-Exclude in pom.xml).
 */

@Isolated
class GuiWindowActionsTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test void enterFullscreen_nullMonitor_doesNotThrow() {
        JFrame f = new JFrame();
        assertDoesNotThrow(() -> GuiWindowActions.enterFullscreen(f, null));
        f.dispose();
    }

    @Test void enterFullscreen_setsUndecorated() {
        JFrame f = new JFrame();
        GuiWindowActions.enterFullscreen(f, null);
        assertTrue(f.isUndecorated());
        f.dispose();
    }

    @Test void installWindowClose_doesNotThrow() {
        JFrame f = new JFrame();
        assertDoesNotThrow(() -> GuiWindowActions.installWindowClose(f, () -> {}));
        f.dispose();
    }

    @Test void installWindowClose_registersListener() {
        JFrame f = new JFrame();
        GuiWindowActions.installWindowClose(f, () -> {});
        assertTrue(f.getWindowListeners().length > 0);
        f.dispose();
    }

    @Test void installKeyboardShortcuts_doesNotThrow() {
        JFrame f = new JFrame();
        GuiMenuHandler handler = new GuiMenuHandler(
                new networktool.gui.panels.GuiInputPanel(new JLabel(), null),
                null, null, null);
        assertDoesNotThrow(() -> GuiWindowActions.installKeyboardShortcuts(
                f, handler, () -> {}, () -> {}, () -> {}));
        f.dispose();
    }
}