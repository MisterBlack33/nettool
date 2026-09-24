package main.java.networktool.gui.components;

import org.junit.jupiter.api.*;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class GuiProgressBarTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private GuiProgressBar bar;

    @BeforeEach void setup() { bar = new GuiProgressBar(); }

    @Test void getPanel_returnsNonNull() {
        assertNotNull(bar.getPanel());
    }

    @Test void panel_initiallyInvisible() {
        assertFalse(bar.getPanel().isVisible());
    }

    @Test void showProgress_makesPanelVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> bar.showProgress(10));
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(bar.getPanel().isVisible());
    }

    @Test void showProgress_zeroTotal_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> bar.showProgress(0)));
    }

    @Test void updateProgress_doesNotThrowBeforeShow() {
        assertDoesNotThrow(() -> bar.updateProgress(3));
    }

    @Test void updateProgress_afterShow_doesNotThrow() throws Exception {
        SwingUtilities.invokeAndWait(() -> bar.showProgress(5));
        SwingUtilities.invokeAndWait(() -> {});
        assertDoesNotThrow(() -> bar.updateProgress(5));
        assertTrue(bar.getPanel().isVisible());
    }

    @Test void repeatedShowProgress_resetsState_doesNotThrow() throws Exception {
        SwingUtilities.invokeAndWait(() -> bar.showProgress(10));
        bar.updateProgress(10);
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> bar.showProgress(20)));
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(bar.getPanel().isVisible());
    }
}
