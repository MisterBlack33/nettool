package main.java.networktool.gui.panels;

import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SavedHostsBulkActionsTest {

    private static final String[] COLS = {"", "IP", "Hostname"};

    private DefaultTableModel model(Object[]... rows) {
        DefaultTableModel m = new DefaultTableModel(COLS, 0);
        for (Object[] row : rows) m.addRow(row);
        return m;
    }

    @Test void noRowsCheckedYieldsEmptyList() {
        DefaultTableModel m = model(
                new Object[]{Boolean.FALSE, "10.0.0.1", "a"},
                new Object[]{Boolean.FALSE, "10.0.0.2", "b"});
        assertTrue(SavedHostsBulkActions.checkedIps(m).isEmpty());
    }

    @Test void returnsOnlyCheckedIps() {
        DefaultTableModel m = model(
                new Object[]{Boolean.TRUE,  "10.0.0.1", "a"},
                new Object[]{Boolean.FALSE, "10.0.0.2", "b"},
                new Object[]{Boolean.TRUE,  "10.0.0.3", "c"});
        List<String> ips = SavedHostsBulkActions.checkedIps(m);
        assertEquals(List.of("10.0.0.1", "10.0.0.3"), ips);
    }

    @Test void skipsPlaceholderDashRow() {
        DefaultTableModel m = model(new Object[]{Boolean.TRUE, "â€“", "leer"});
        assertTrue(SavedHostsBulkActions.checkedIps(m).isEmpty());
    }

    @Test void nullModelYieldsEmptyList() {
        assertTrue(SavedHostsBulkActions.checkedIps(null).isEmpty());
    }
}
