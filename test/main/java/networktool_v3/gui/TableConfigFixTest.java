package main.java.networktool_v3.gui;

import main.java.networktool.gui.TableConfig;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für TableConfig-Fix: totalWidth() entfernt. */
class TableConfigFixTest {

    @Test
    void totalWidth_methodDoesNotExist() {
        boolean found = false;
        for (Method m : TableConfig.class.getDeclaredMethods()) {
            if (m.getName().equals("totalWidth")) { found = true; break; }
        }
        assertFalse(found, "totalWidth() sollte entfernt sein");
    }

    @Test
    void preferredHeight_positive() {
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(
                        new Object[][]{{"1.1.1.1", "host", "Linux"}},
                        new String[]{"IP", "Hostname", "OS"});
        javax.swing.JTable table = TableConfig.buildTable(model, TableConfig.WIDTHS_HOST);
        assertTrue(TableConfig.preferredHeight(table) > 0);
    }

    @Test
    void widthConstants_notEmpty() {
        assertTrue(TableConfig.WIDTHS_HOST.length > 0);
        assertTrue(TableConfig.WIDTHS_SCAN.length > 0);
        assertTrue(TableConfig.WIDTHS_SAVED.length > 0);
        assertTrue(TableConfig.WIDTHS_SAVED_ALL.length > 0);
        assertTrue(TableConfig.WIDTHS_TRACE.length > 0);
    }

    @Test
    void savedColNotes_inRange() {
        assertTrue(TableConfig.SAVED_COL_NOTES < TableConfig.WIDTHS_SAVED.length);
    }

    @Test
    void rowHeight_positive() {
        assertTrue(TableConfig.ROW_HEIGHT > 0);
    }
}