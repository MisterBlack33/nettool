package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

import static main.java.networktool.gui.GuiTheme.*;

final class GuiAuditTable {

    private GuiAuditTable() {}

    private static final int[] WIDTHS = {130, 70, 140, 260};

    static DefaultTableModel createModel() {
        return new DefaultTableModel(new Object[0][],
                new String[]{"Zeit", "User", "Aktion", "Detail", "_full"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint());
                if (col == 3 && row >= 0) {
                    Object full = getModel().getValueAt(convertRowIndexToModel(row), 4);
                    if (full != null && !full.toString().isBlank())
                        return "<html><pre style='font-family:monospace'>" +
                                full.toString().replace("&","&amp;").replace("<","&lt;") + "</pre></html>";
                }
                return super.getToolTipText(e);
            }

            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? GuiTheme.rowEven() : GuiTheme.rowOdd());
                    c.setForeground(colColor(col, getValueAt(row, col)));
                }
                c.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
                return c;
            }
        };
        applyStyle(table);
        return table;
    }

    private static void applyStyle(JTable table) {
        table.setBackground(GuiTheme.rowEven());
        table.setForeground(FG);
        table.setFont(MONO_XS);
        table.setRowHeight(19);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < 4 && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(WIDTHS[i]);
            if (i < 3) { cm.getColumn(i).setMinWidth(WIDTHS[i]); cm.getColumn(i).setMaxWidth(WIDTHS[i]); }
        }
        if (cm.getColumnCount() > 4) {
            TableColumn hidden = cm.getColumn(4);
            hidden.setMinWidth(0); hidden.setMaxWidth(0); hidden.setWidth(0);
        }
        TableConfig.styleHeader(table.getTableHeader());
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static Color colColor(int col, Object val) {
        return switch (col) {
            case 0 -> FG_DIM;
            case 1 -> ACCENT;
            case 2 -> actionColor(val != null ? val.toString() : "");
            default -> FG;
        };
    }

    static Color actionColor(String action) {
        if (action == null) return FG;
        String a = action.toUpperCase().trim();

        // Angriffsereignisse → rot
        if (a.contains("SPOOF") || a.contains("POISON") || a.contains("ROGUE")) return WARN;
        if (a.contains("FAIL")  || a.contains("BLOCK"))                          return WARN;
        if (a.contains("ALERT"))                                                  return WARN;

        // Monitor-Ereignisse → orange (vor generischen Prefix-Checks)
        if (a.startsWith("SECURITY_MONITOR") || a.startsWith("ARP_MONITOR")
                || a.startsWith("PORT_MONITOR"))                        return new Color(0xFF, 0xA0, 0x30);

        // Generische Security/ARP → orange
        if (a.startsWith("SECURITY") || a.startsWith("ARP"))           return new Color(0xFF, 0xA0, 0x30);

        if (a.startsWith("SCAN") || a.startsWith("DIAGNOSE") || a.startsWith("CIDR")) return INFO;
        if (a.startsWith("LOGIN"))                                       return ACCENT2;
        if (a.startsWith("EXPORT") || a.startsWith("IMPORT"))           return new Color(0xD0, 0xC0, 0x60);
        if (a.startsWith("USER") || a.startsWith("APP_START"))          return ACCENT;
        return FG;
    }
}