package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiDataIOActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void importAndReport_zeroImportsUsesWarning() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiDataIOActions.importAndReport(output, "IMPORT_TEST", 0);
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(output.doc.getText(0, output.doc.getLength()).contains("[WARN] Keine Daten importiert"));
    }

    @Test void importAndReport_positiveImportsUsesSuccess() throws Exception {
        GuiOutputPanel output = new GuiOutputPanel();
        GuiDataIOActions.importAndReport(output, "IMPORT_TEST", 2);
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(output.doc.getText(0, output.doc.getLength()).contains("[OK] 2 importiert"));
    }
}
