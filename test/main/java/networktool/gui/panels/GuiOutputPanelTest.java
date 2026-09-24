package main.java.networktool.gui.panels;

import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.Color;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class GuiOutputPanelTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private GuiOutputPanel panel;

    @BeforeEach void setup() throws Exception {
        SwingUtilities.invokeAndWait(() -> panel = new GuiOutputPanel());
    }

    @Test void appendText_onEdt_addsToDocument() throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.appendText("hello\n", Color.WHITE));
        assertTrue(panel.doc.getLength() >= 6);
    }

    @Test void appendText_fromBackgroundThread_stillApplied() throws Exception {
        panel.appendText("async\n", Color.WHITE);
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(panel.doc.getLength() >= 5);
    }

    @Test void appendText_beyondMaxChars_trimsOldestContent() throws Exception {
        String chunk = "x".repeat(10_000);
        for (int i = 0; i < 7; i++) SwingUtilities.invokeAndWait(() -> panel.appendText(chunk, Color.WHITE));
        assertTrue(panel.doc.getLength() < 70_000);
    }

    @Test void printBanner_addsNonEmptyText() throws Exception {
        SwingUtilities.invokeAndWait(panel::printBanner);
        assertTrue(panel.doc.getLength() > 0);
    }

    @Test void buildScrollPane_returnsNonNull() {
        assertNotNull(panel.buildScrollPane());
    }

    @Test void buildTopBar_returnsNonNull() {
        assertNotNull(panel.buildTopBar());
    }

    @Test void getOutputPane_returnsNonNull() {
        assertNotNull(panel.getOutputPane());
    }

    @Test void redirectStreams_doesNotThrow_andRestoresAfterward() {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        try {
            assertDoesNotThrow(panel::redirectStreams);
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
        }
    }
}
