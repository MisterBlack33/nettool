package networktool_v3.gui;

import networktool_v3.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static networktool_v3.gui.GuiTheme.*;
import static networktool_v3.gui.TableConfig.ROW_BG_EVEN;
import static networktool_v3.gui.TableConfig.ROW_BG_ODD;

/**
 * Audit-Log-Viewer (Menü-ID "23", nur Admins).
 *
 * Verbesserungen:
 *  - Kompakte Zeilenhöhe (20px)
 *  - Detail-Spalte auf 60 Zeichen begrenzt; Tooltip zeigt vollen Text
 *  - Aufklappbare Aktions-Legende unterhalb der Tabelle
 *  - Hint: Rotation ab 200.000 Zeilen, Logs bleiben erhalten
 */
public final class GuiAuditPanel {

    private GuiAuditPanel() {}

    private static final String[] COLS   = {"Zeit", "User", "Aktion", "Detail"};
    private static final int[]    WIDTHS = {140, 80, 150, 260};
    private static final int      MAX_DETAIL = 60;

    private static final String[][] ACTION_LEGEND = {
            {"LOGIN",              "Erfolgreiche Anmeldung"},
            {"LOGIN_FAILED",       "Fehlgeschlagener Login-Versuch"},
            {"LOGOUT",             "Abmeldung"},
            {"USER_CREATED",       "Neuer Benutzer angelegt"},
            {"APP_START",          "Programm gestartet (GUI/CLI)"},
            {"APP_EXIT",           "Programm beendet"},
            {"APP_RESTART",        "Neustart"},
            {"MENU",               "Menüpunkt geklickt – Detail = ID"},
            {"SCAN_* / DIAGNOSE_*","Netzwerk-Scan / IP-Diagnose"},
            {"SECURITY_ALERT_*",   "Sicherheitswarnung (ARP / Rogue)"},
            {"SECURITY_MONITOR_*", "Sicherheitsmonitor gestartet/gestoppt"},
            {"EXPORT_* / IMPORT_*","Datenexport / -import"},
            {"THEME_TOGGLE",       "Dark/Light-Mode gewechselt"},
            {"CANCEL",             "Laufender Scan abgebrochen"},
            {"AUDIT_LOG_CLEARED",  "Audit-Log manuell geleert"},
    };

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

        outer.add(buildToolbar(outer, output, panBg, bg), BorderLayout.NORTH);

        // Table model: 5 Spalten, letzte unsichtbar (voller Detail-Text für Tooltip)
        DefaultTableModel model = new DefaultTableModel(new Object[0][],
                new String[]{"Zeit","User","Aktion","Detail","_full"}) {
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
        sp.setPreferredSize(new Dimension(0, 340));
        outer.add(sp, BorderLayout.CENTER);
        outer.add(buildLegend(panBg), BorderLayout.SOUTH);

        // Filter + Aktionen aus Toolbar verdrahten
        JTextField filterField = (JTextField) ((JPanel)
                ((JPanel) outer.getComponent(0)).getComponent(0)).getComponent(1);
        JLabel countLbl = (JLabel) ((JPanel)
                ((JPanel) outer.getComponent(0)).getComponent(0)).getComponent(2);
        JButton refreshBtn = (JButton) ((JPanel)
                ((JPanel) outer.getComponent(0)).getComponent(1)).getComponent(0);
        JButton clearBtn   = (JButton) ((JPanel)
                ((JPanel) outer.getComponent(0)).getComponent(1)).getComponent(1);

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            private void applyFilter() {
                String q = filterField.getText().trim();
                sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + q));
                countLbl.setText(table.getRowCount() + " Einträge");
            }
        });

        Runnable reload = () -> {
            List<AuditLogger.LogEntry> entries = AuditLogger.getInstance().readRecent(2000);
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (AuditLogger.LogEntry e : entries) {
                    String shortDetail = e.detail.length() > MAX_DETAIL
                            ? e.detail.substring(0, MAX_DETAIL) + "…" : e.detail;
                    model.addRow(new Object[]{
                            e.timestamp, e.user, e.action, shortDetail, e.detail});
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

        new Thread(reload, "AuditLoad").start();

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private static JPanel buildToolbar(JPanel outer, GuiOutputPanel output,
                                       Color panBg, Color bg) {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(panBg);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 0, 1, BORDER),
                new EmptyBorder(5, 10, 5, 10)));

        JTextField filterField = new JTextField();
        filterField.setFont(MONO_XS);
        filterField.setForeground(FG);
        filterField.setBackground(bg);
        filterField.setCaretColor(ACCENT);
        filterField.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(2, 6, 2, 6)));
        filterField.putClientProperty("JTextField.placeholderText", "Filter…");

        JLabel countLbl = new JLabel("–");
        countLbl.setFont(MONO_XS);
        countLbl.setForeground(FG_DIM);
        countLbl.setBorder(new EmptyBorder(0, 8, 0, 0));

        JPanel left = new JPanel(new BorderLayout(4, 0));
        left.setOpaque(false);
        left.add(filterField, BorderLayout.CENTER);
        left.add(countLbl,   BorderLayout.EAST);

        JButton refreshBtn = toolBtn("↻", ACCENT);
        JButton clearBtn   = toolBtn("🗑", WARN);
        refreshBtn.setToolTipText("Neu laden");
        clearBtn.setToolTipText("Log leeren");

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        right.add(clearBtn);

        toolbar.add(left,  BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    // ── Legende ───────────────────────────────────────────────────────────

    private static JPanel buildLegend(Color panBg) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(panBg);
        wrapper.setBorder(new MatteBorder(0, 1, 1, 1, BORDER));

        JButton toggleBtn = new JButton("▶  Aktions-Codes erklären");
        toggleBtn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        toggleBtn.setForeground(FG_DIM);
        toggleBtn.setBackground(panBg);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setBorder(new EmptyBorder(4, 10, 4, 10));
        toggleBtn.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 1));
        grid.setBackground(panBg);
        grid.setBorder(new EmptyBorder(4, 14, 8, 14));
        grid.setVisible(false);

        for (String[] row : ACTION_LEGEND) {
            JLabel code = new JLabel(row[0]);
            code.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
            code.setForeground(actionColor(row[0].replace("_*","").replace("/*","")));
            JLabel desc = new JLabel(row[1]);
            desc.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
            desc.setForeground(FG_DIM);
            grid.add(code);
            grid.add(desc);
        }

        JLabel hint = new JLabel(
                "  Rotation ab 200.000 Einträgen  ·  Logs bleiben über Neustarts erhalten");
        hint.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        hint.setForeground(FG_DIM);
        hint.setBorder(new EmptyBorder(2, 10, 4, 10));

        toggleBtn.addActionListener(e -> {
            boolean open = grid.isVisible();
            grid.setVisible(!open);
            toggleBtn.setText((open ? "▶" : "▼") + "  Aktions-Codes erklären");
            wrapper.revalidate(); wrapper.repaint();
        });

        wrapper.add(toggleBtn,  BorderLayout.NORTH);
        wrapper.add(grid,       BorderLayout.CENTER);
        wrapper.add(hint,       BorderLayout.SOUTH);
        return wrapper;
    }

    // ── Tabelle ───────────────────────────────────────────────────────────

    private static JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (col == 3 && row >= 0) {
                    Object full = getModel().getValueAt(convertRowIndexToModel(row), 4);
                    if (full != null && !full.toString().isBlank())
                        return "<html><pre style='font-family:monospace'>"
                                + full.toString().replace("<","&lt;") + "</pre></html>";
                }
                return super.getToolTipText(e);
            }

            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    switch (col) {
                        case 0 -> c.setForeground(FG_DIM);
                        case 1 -> c.setForeground(ACCENT);
                        case 2 -> {
                            Object v = getValueAt(row, col);
                            c.setForeground(actionColor(v != null ? v.toString() : ""));
                        }
                        default -> c.setForeground(FG);
                    }
                }
                c.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
                return c;
            }
        };

        // Spaltenbreiten für 4 sichtbare Spalten setzen
        table.setBackground(ROW_BG_EVEN);
        table.setForeground(FG);
        table.setFont(MONO_XS);
        table.setRowHeight(20);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        int[] w = WIDTHS;
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < 4 && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(w[i]);
            if (i < 3) { cm.getColumn(i).setMinWidth(w[i]); cm.getColumn(i).setMaxWidth(w[i]); }
            else        { cm.getColumn(i).setMinWidth(w[i]); cm.getColumn(i).setMaxWidth(Integer.MAX_VALUE); }
        }
        // Versteckte 5. Spalte (_full)
        if (cm.getColumnCount() > 4) {
            TableColumn hidden = cm.getColumn(4);
            hidden.setMinWidth(0); hidden.setMaxWidth(0); hidden.setWidth(0);
        }

        TableConfig.styleHeader(table.getTableHeader());
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private static Color actionColor(String action) {
        if (action == null) return FG;
        String a = action.toUpperCase();
        if (a.startsWith("LOGIN") && !a.contains("FAIL") && !a.contains("BLOCK")) return ACCENT2;
        if (a.contains("FAIL")||a.contains("BLOCK")||a.contains("ALERT")
                ||a.contains("SPOOF")||a.contains("POISON")||a.contains("ROGUE")) return WARN;
        if (a.startsWith("SECURITY")||a.startsWith("ARP")||a.startsWith("PORT_MONITOR"))
            return new Color(0xFF,0xA0,0x30);
        if (a.startsWith("SCAN")||a.startsWith("DIAGNOSE")||a.startsWith("CIDR")) return INFO;
        if (a.startsWith("EXPORT")||a.startsWith("IMPORT")||a.startsWith("RESTORE"))
            return new Color(0xD0,0xC0,0x60);
        if (a.startsWith("USER")||a.startsWith("APP_START")) return ACCENT;
        return FG;
    }

    private static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(),1), new EmptyBorder(2,7,2,7)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        return b;
    }
}