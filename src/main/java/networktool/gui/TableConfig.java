package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

public final class TableConfig {

    private TableConfig() {}

    public static final int ROW_HEIGHT = 26;

    public static final int[] WIDTHS_HOST      = {120, 280, 160};
    public static final int[] WIDTHS_SCAN      = {120, 240, 150, 120};
    public static final int[] WIDTHS_SAVED     = {115, 210, 130, 110, 145, 160};
    public static final int[] WIDTHS_SAVED_ALL = {105, 190, 120, 110, 138, 100, 150};
    public static final int[] WIDTHS_TRACE     = {38, 120, 200, 150};

    public static final Color ROW_BG_EVEN   = GuiTheme.ROW_EVEN;
    public static final Color ROW_BG_ODD    = GuiTheme.ROW_ODD;
    public static final Color HEADER_BG     = new Color(0x0A, 0x16, 0x24);
    public static final Color HEADER_BORDER = GuiTheme.ACCENT;

    public static final Color NOTES_FG      = new Color(0xFF, 0xE0, 0x82);
    public static final Color NOTES_BG_EVEN = new Color(0x13, 0x11, 0x08);
    public static final Color NOTES_BG_ODD  = new Color(0x19, 0x16, 0x0B);

    public static final int SAVED_COL_NOTES = 5;

    public static JTable buildTable(DefaultTableModel model, int[] widths) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    Object val = getValueAt(convertRowIndexToModel(row), 2);
                    String os  = val != null ? val.toString() : "";
                    c.setForeground(col == 2 ? osColor(os) : col >= 3 ? FG_DIM : FG);
                }
                c.setFont(MONO_S);
                return c;
            }
        };
        applyBaseStyle(table, widths);
        return table;
    }

    public static JTable buildSavedTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int col) { return col == SAVED_COL_NOTES; }

            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else if (col == SAVED_COL_NOTES) {
                    c.setBackground(row % 2 == 0 ? NOTES_BG_EVEN : NOTES_BG_ODD);
                    c.setForeground(NOTES_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    Object val = getValueAt(convertRowIndexToModel(row), 2);
                    c.setForeground(col == 2 ? osColor(val != null ? val.toString() : "") : col == 3 ? FG_DIM : FG);
                }
                c.setFont(col == SAVED_COL_NOTES
                        ? new Font("JetBrains Mono", Font.ITALIC, 12) : MONO_S);
                return c;
            }
        };
        JTextField notesEditor = styledEditor(NOTES_BG_EVEN, NOTES_FG, Font.ITALIC);
        table.getColumnModel().getColumn(SAVED_COL_NOTES).setCellEditor(new DefaultCellEditor(notesEditor));
        JTextField osEditor = styledEditor(new Color(0x0D,0x11,0x17), new Color(0xC8,0xD8,0xE8), Font.PLAIN);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(osEditor));
        applyBaseStyle(table, WIDTHS_SAVED);
        return table;
    }

    public static JTable buildSavedTableAll(DefaultTableModel model) {
        final int COL_NOTES_ALL = 6;
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int col) { return col == COL_NOTES_ALL || col == 2; }

            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else if (col == COL_NOTES_ALL) {
                    c.setBackground(row % 2 == 0 ? NOTES_BG_EVEN : NOTES_BG_ODD);
                    c.setForeground(NOTES_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    Object val = getValueAt(convertRowIndexToModel(row), 2);
                    c.setForeground(col == 2 ? osColor(val != null ? val.toString() : "") : col == 5 ? ACCENT : col >= 3 ? FG_DIM : FG);
                }
                c.setFont(col == COL_NOTES_ALL ? new Font("JetBrains Mono", Font.ITALIC, 12) : MONO_S);
                return c;
            }
        };
        JTextField notesEditor = styledEditor(NOTES_BG_EVEN, NOTES_FG, Font.ITALIC);
        table.getColumnModel().getColumn(COL_NOTES_ALL).setCellEditor(new DefaultCellEditor(notesEditor));
        JTextField osEditor = styledEditor(new Color(0x0D,0x11,0x17), new Color(0xC8,0xD8,0xE8), Font.PLAIN);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(osEditor));
        applyBaseStyle(table, WIDTHS_SAVED_ALL);
        return table;
    }

    public static void applyBaseStyle(JTable table, int[] widths) {
        table.setBackground(ROW_BG_EVEN);
        table.setForeground(FG);
        table.setFont(MONO_S);
        table.setRowHeight(ROW_HEIGHT);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        applyColumnWidths(table, widths);
        styleHeader(table.getTableHeader());
    }

    public static void applyColumnWidths(JTable table, int[] widths) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(widths[i]);
            if (i < widths.length - 1) col.setMaxWidth(widths[i]);
            else                        col.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    public static void styleHeader(JTableHeader header) {
        header.setBackground(HEADER_BG);
        header.setForeground(ACCENT);
        header.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, HEADER_BORDER));
    }

    public static int preferredHeight(JTable table) {
        return table.getRowHeight() * table.getRowCount()
                + table.getTableHeader().getPreferredSize().height;
    }

    private static JTextField styledEditor(Color bg, Color fg, int style) {
        JTextField f = new JTextField();
        f.setBackground(bg);
        f.setForeground(fg);
        f.setCaretColor(fg);
        f.setFont(new Font("JetBrains Mono", style, 12));
        f.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        return f;
    }
}