package main.java.networktool.gui.core;

import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GuiMenuHandlerAsyncTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void runAsync_exceptionReportsSafeErrorAndDoesNotSayFinished() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiStatusBar status = new GuiStatusBar();
        GuiMenuHandler handler = new GuiMenuHandler(new GuiInputPanel(new JLabel(), output), output, null, status);
        CountDownLatch taskReturned = new CountDownLatch(1);

        handler.runAsync(() -> {
            try {
                throw new IllegalStateException("https://host.test/hook?token=secret");
            } finally {
                taskReturned.countDown();
            }
        });

        assertTrue(taskReturned.await(5, TimeUnit.SECONDS));
        awaitStopped(handler);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Fehler", status.getLabel().getText());
        String rendered = output.doc.getText(0, output.doc.getLength());
        assertTrue(rendered.contains("[FEHLER] Ein unerwarteter Fehler ist aufgetreten."));
        assertFalse(rendered.contains("secret"));
        assertFalse(rendered.contains("host.test"));
    }

    @Test void runAsync_successReportsFinished() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiStatusBar status = new GuiStatusBar();
        GuiMenuHandler handler = new GuiMenuHandler(new GuiInputPanel(new JLabel(), output), output, null, status);
        CountDownLatch taskReturned = new CountDownLatch(1);

        handler.runAsync(taskReturned::countDown);

        assertTrue(taskReturned.await(5, TimeUnit.SECONDS));
        awaitStopped(handler);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Fertig", status.getLabel().getText());
        assertEquals(0, output.doc.getLength());
    }

    @Test void runAsync_interruptionReportsWarningAndDoesNotSayFinished() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiStatusBar status = new GuiStatusBar();
        GuiMenuHandler handler = new GuiMenuHandler(new GuiInputPanel(new JLabel(), output), output, null, status);
        CountDownLatch taskReturned = new CountDownLatch(1);

        handler.runAsync(() -> {
            try {
                throw new InterruptedException();
            } finally {
                taskReturned.countDown();
            }
        });

        assertTrue(taskReturned.await(5, TimeUnit.SECONDS));
        awaitStopped(handler);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Abgebrochen", status.getLabel().getText());
        assertTrue(output.doc.getText(0, output.doc.getLength()).contains("[WARN] Aktion abgebrochen."));
    }

    private static void awaitStopped(GuiMenuHandler handler) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (handler.isRunning() && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertFalse(handler.isRunning());
    }
}
