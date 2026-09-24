package main.java.networktool.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import static org.junit.jupiter.api.Assertions.*;

class TableConfigExtTest {
    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }
    private DefaultTableModel savedModel() {
        return new DefaultTableModel(new Object[][]{{"1.1.1.1", "host", "Linux", "", "", "note"}},
                new String[]{"IP", "Hostname", "OS", "Ports", "Datum", "Notiz"});
    }
    private DefaultTableModel savedAllModel() {
        return new DefaultTableModel(new Object[][]{{"1.1.1.1", "host", "Linux", "", "", "Cat", "note"}},
                new String[]{"IP", "Hostname", "OS", "Ports", "Datum", "Kategorie", "Notiz"});
    }
    @Test void buildSavedTable_onlyNotesColumnEditable() {
        JTable t = TableConfig.buildSavedTable(savedModel());
        assertTrue(t.isCellEditable(0, TableConfig.SAVED_COL_NOTES));
        assertFalse(t.isCellEditable(0, 0));
    }
    @Test void buildSavedTable_appliesWidths() {
        JTable t = TableConfig.buildSavedTable(savedModel());
        assertEquals(TableConfig.WIDTHS_SAVED[0], t.getColumnModel().getColumn(0).getPreferredWidth());
    }
    @Test void buildSavedTableAll_notesAndOsEditable() {
        JTable t = TableConfig.buildSavedTableAll(savedAllModel());
        assertTrue(t.isCellEditable(0, 6));
        assertTrue(t.isCellEditable(0, 2));
        assertFalse(t.isCellEditable(0, 1));
    }
    @Test void applyColumnWidths_lastColumnUnbounded() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"a", "b"}}, new String[]{"A", "B"}));
        TableConfig.applyColumnWidths(table, new int[]{50, 80});
        TableColumn last = table.getColumnModel().getColumn(1);
        assertEquals(Integer.MAX_VALUE, last.getMaxWidth());
        assertEquals(80, last.getPreferredWidth());
    }
    @Test void applyColumnWidths_shorterThanColumns_doesNotThrow() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"a", "b", "c"}}, new String[]{"A", "B", "C"}));
        assertDoesNotThrow(() -> TableConfig.applyColumnWidths(table, new int[]{40}));
    }
    @Test void styleHeader_disablesReordering() {
        JTable table = new JTable(savedModel());
        TableConfig.styleHeader(table.getTableHeader());
        assertFalse(table.getTableHeader().getReorderingAllowed());
    }
    @Test void preferredHeight_scalesWithRowCount() {
        JTable single = TableConfig.buildTable(new DefaultTableModel(new Object[][]{{"1.1.1.1", "h", "Linux"}},
                new String[]{"IP", "Host", "OS"}), TableConfig.WIDTHS_HOST);
        JTable empty = TableConfig.buildTable(new DefaultTableModel(new Object[0][], new String[]{"IP", "Host", "OS"}),
                TableConfig.WIDTHS_HOST);
        assertTrue(TableConfig.preferredHeight(single) > TableConfig.preferredHeight(empty));
    }
}
