package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GuiTagFilterActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private final GuiOutputPanel output = new GuiOutputPanel();
    private final GuiTableRenderer tables = new GuiTableRenderer(output);

    @Test void apply_tagAndNull_doNotThrow() {
        assertDoesNotThrow(() -> GuiTagFilterActions.apply("x", tables));
        assertDoesNotThrow(() -> GuiTagFilterActions.apply(null, tables));
    }

    @Test void handle_registersPrompt_doesNotThrow() {
        assertDoesNotThrow(() -> GuiTagFilterActions.handle(new GuiInputPanel(new JLabel(), output), tables));
    }
}
