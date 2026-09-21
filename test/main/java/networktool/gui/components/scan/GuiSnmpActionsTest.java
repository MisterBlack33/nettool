package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.snmp.SnmpPortTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiSnmpActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private static String print(List<SnmpPortTable.Row> rows) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GuiSnmpActions.printRows(rows, new PrintStream(buffer, true));
        return buffer.toString();
    }

    @Test void community_defaultsWhenBlank() {
        assertEquals("public", GuiSnmpActions.communityOrDefault(null));
        assertEquals("public", GuiSnmpActions.communityOrDefault("  "));
    }

    @Test void community_trimsInput() { assertEquals("secret", GuiSnmpActions.communityOrDefault(" secret ")); }

    @Test void printRows_empty_showsHint() { assertTrue(print(List.of()).contains("Keine SNMP-Antwort")); }

    @Test void printRows_listsEveryPort() {
        String out = print(List.of(new SnmpPortTable.Row(1, "Port1", true, 0),
                new SnmpPortTable.Row(2, "Port2", false, 0)));
        assertTrue(out.contains("Port1") && out.contains("Port2"));
    }

    @Test void handleWalk_registersPrompt_withoutThrowing() {
        GuiOutputPanel output = new GuiOutputPanel();
        assertDoesNotThrow(() -> GuiSnmpActions.handleWalk(new GuiInputPanel(new JLabel(), output), output, null));
    }
}
