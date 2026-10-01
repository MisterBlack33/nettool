package main.java.networktool.gui.core;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.theme.GuiTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiStatusReporterTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void report_usesTagAndThemeColor() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiStatusReporter.error(output, "Aktion fehlgeschlagen");
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals("  [FEHLER] Aktion fehlgeschlagen\n", output.doc.getText(0, output.doc.getLength()));
        assertEquals(GuiTheme.WARN,
                StyleConstants.getForeground(output.doc.getCharacterElement(0).getAttributes()));
    }
}
