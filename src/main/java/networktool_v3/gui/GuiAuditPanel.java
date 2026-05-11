package main.java.networktool_v3.gui;

import main.java.networktool_v3.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;
import static main.java.networktool_v3.gui.TableConfig.ROW_BG_EVEN;
import static main.java.networktool_v3.gui.TableConfig.ROW_BG_ODD;

/**
 * Audit-Log-Viewer Panel.
 *
 * Wird als Menüpunkt ins GUI eingebettet (Menü-ID "23").
 * Zeigt alle Audit-Log-Einträge in einer filterbaren Tabelle:
 *  - Spalten: Zeitstempel | Benutzer | Aktion | Detail
 *  - Filterfeld: Live-Suche über alle Spalten
 *  - Schaltfläche "Aktualisieren" zum Neuladen
 *  - Nur für eingeloggte Benutzer sichtbar
 */
public final class GuiAuditPanel {

    private GuiAuditPanel() {}

    private static final String[] COLS    = {"Zeitstempel", "Benutzer", "Aktion", "Detail"};
    private static final int[]    WIDTHS  = {150, 100, 160, 280};

    /**
     * Bettet den Audit-Log-Viewer in das Output-Panel ein.
     */
    public static void show(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> {
            output.appendText("\n★ Audit-Log\n\n", ACCENT);
            embedPanel(output);
        });
    }

    private static void embedPanel(GuiOutputPanel output) {
        Color bg    = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF4, 0xF2, 0xEE);
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xE8, 0xE6, 0xE0);

        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(bg);

        // ── Toolbar ───────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(panBg);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 0, 1, BORDER),
                new EmptyBorder(6, 10, 6, 10)));

        JTextField filterField = new JTextField();
        filterField.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        filterField.setForeground(FG);
        filterField.setBackground(bg);
        filterField.setCaretColor(ACCENT);
        filterField.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(3, 6, 3, 6)));
        filterField.putClientProperty("JTextField.placeholderText", "Filter…");

        JButton refreshBtn = toolBtn("↻ Laden",     ACCENT);
        JButton clearBtn   = toolBtn("🗑 Log leeren", WARN);

        JLabel countLbl = new JLabel("0 Einträge");
        countLbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        countLbl.setForeground(FG_DIM);

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.setOpaque(false);
        left.add(new JLabel("  🔍 ") {{
            setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12)); setForeground(FG_DIM);
        }}, BorderLayout.WEST);
        left.add(filterField, BorderLayout.CENTER);
        left.add(countLbl,   BorderLayout.EAST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        right.add(clearBtn);

        toolbar.add(left,  BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);
        outer.add(toolbar, BorderLayout.NORTH);

        // ── Tabelle ───────────────────────────────────────────────────────
        DefaultTableModel model = new DefaultTableModel(new Object[0][], COLS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN);
        sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, 400));
        outer.add(sp, BorderLayout.CENTER);

        // ── Hint ──────────────────────────────────────────────────────────
        JLabel hint = new JLabel("  Alle Benutzeraktionen werden protokolliert.  |  "
                + "Einträge werden automatisch rotiert ab 10.000 Zeilen.");
        hint.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        hint.setForeground(FG_DIM);
        hint.setBorder(new EmptyBorder(4, 8, 4, 8));
        outer.add(hint, BorderLayout.SOUTH);

        // ── Live-Filter ───────────────────────────────────────────────────
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            private void applyFilter() {
                String q = filterField.getText().trim();
                if (q.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + q));
                countLbl.setText(table.getRowCount() + " Einträge");
            }
        });

        // ── Aktionen ──────────────────────────────────────────────────────
        Runnable reload = () -> {
            List<AuditLogger.LogEntry> entries = AuditLogger.getInstance().readRecent(2000);
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (AuditLogger.LogEntry e : entries) {
                    model.addRow(new Object[]{e.timestamp, e.user, e.action, e.detail});
                }
                countLbl.setText(model.getRowCount() + " Einträge");
            });
        };

        refreshBtn.addActionListener(e -> new Thread(reload, "AuditLoad").start());
        clearBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(null,
                    "Audit-Log wirklich leeren?", "Bestätigung",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                AuditLogger.getInstance().clear();
                model.setRowCount(0);
                countLbl.setText("0 Einträge");
            }
        });

        // Beim Einbetten sofort laden
        new Thread(reload, "AuditLoad").start();

        // In Output einbetten
        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    // ── Tabellen-Styling ──────────────────────────────────────────────────

    private static JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    // Aktion-Spalte (2) einfärben je nach Typ
                    if (col == 2) {
                        Object val = getValueAt(row, col);
                        String action = val != null ? val.toString() : "";
                        c.setForeground(actionColor(action));
                    } else if (col == 1) {
                        c.setForeground(ACCENT);
                    } else if (col == 0) {
                        c.setForeground(FG_DIM);
                    } else {
                        c.setForeground(FG);
                    }
                }
                c.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
                return c;
            }
        };
        TableConfig.applyBaseStyle(table, WIDTHS);
        table.setRowHeight(22);
        return table;
    }

    private static Color actionColor(String action) {
        if (action == null) return FG;
        String a = action.toUpperCase();
        if (a.startsWith("LOGIN") && !a.contains("FAIL") && !a.contains("BLOCK"))
            return ACCENT2;
        if (a.contains("FAIL") || a.contains("BLOCK") || a.contains("ALERT")
                || a.contains("SPOOF") || a.contains("POISON") || a.contains("ROGUE"))
            return WARN;
        if (a.startsWith("SECURITY") || a.startsWith("ARP") || a.startsWith("PORT_MONITOR"))
            return new Color(0xFF, 0xA0, 0x30);
        if (a.startsWith("SCAN") || a.startsWith("DIAGNOSE") || a.startsWith("CIDR"))
            return INFO;
        if (a.startsWith("EXPORT") || a.startsWith("IMPORT") || a.startsWith("RESTORE"))
            return new Color(0xD0, 0xC0, 0x60);
        if (a.startsWith("USER") || a.startsWith("APP_START"))
            return ACCENT;
        return FG;
    }

    // ── Button-Styling ────────────────────────────────────────────────────

    private static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(
                new LineBorder(fg.darker(), 1), new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        return b;
    }
}