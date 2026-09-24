package main.java.networktool.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import static main.java.networktool.theme.GuiTheme.*;
import static org.junit.jupiter.api.Assertions.*;

class TableConfigRendererTest {
    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }
    private static Component render(JTable t, int row, int col) {
        return t.prepareRenderer(t.getCellRenderer(row, col), row, col);
    }
    private static JTable hostTable() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{
                {"1.1.1.1", "a", "Linux", "22"}, {"1.1.1.2", "b", "Windows", "80"}},
                new String[]{"IP", "Host", "OS", "Ports"});
        return TableConfig.buildTable(m, TableConfig.WIDTHS_HOST);
    }
    private static JTable savedTable() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{
                {"1.1.1.1", "a", "Linux", "22", "d", "note"}, {"1.1.1.2", "b", "Win", "80", "d", "n2"}},
                new String[]{"IP", "Host", "OS", "Ports", "Datum", "Notiz"});
        return TableConfig.buildSavedTable(m);
    }
    @Test void evenRow_usesEvenBackground() { assertEquals(TableConfig.ROW_BG_EVEN, render(hostTable(), 0, 0).getBackground()); }
    @Test void oddRow_usesOddBackground() { assertEquals(TableConfig.ROW_BG_ODD, render(hostTable(), 1, 0).getBackground()); }
    @Test void osColumn_usesOsColor() { assertEquals(osColor("Linux"), render(hostTable(), 0, 2).getForeground()); }
    @Test void trailingColumns_useDimForeground() { assertEquals(FG_DIM, render(hostTable(), 0, 3).getForeground()); }
    @Test void selectedRow_usesSelectionColors() {
        JTable t = hostTable(); t.setRowSelectionInterval(0, 0);
        Component c = render(t, 0, 0);
        assertEquals(t.getSelectionBackground(), c.getBackground());
        assertEquals(t.getSelectionForeground(), c.getForeground());
    }
    @Test void savedTable_notesColumn_usesNotesColors() {
        Component c = render(savedTable(), 0, TableConfig.SAVED_COL_NOTES);
        assertEquals(TableConfig.NOTES_BG_EVEN, c.getBackground());
        assertEquals(TableConfig.NOTES_FG, c.getForeground());
    }
    @Test void savedTable_oddNotesRow_usesOddNotesBackground() {
        assertEquals(TableConfig.NOTES_BG_ODD, render(savedTable(), 1, TableConfig.SAVED_COL_NOTES).getBackground());
    }
}
