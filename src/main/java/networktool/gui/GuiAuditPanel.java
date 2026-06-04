package main.java.networktool.gui;

import main.java.networktool.security.AuditLogEntry;
import main.java.networktool.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;

/** Audit-Log-Viewer (Menü-ID "23", nur Admins). */
public final class GuiAuditPanel {

    private GuiAuditPanel() {}

    static final int MAX_DETAIL = 55;

    private record ToolbarRefs(
            JPanel panel, JTextField filterField,
            JLabel countLbl, JButton refreshBtn, JButton clearBtn
    ) {}

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

        ToolbarRefs refs = buildToolbar(panBg, bg);
        outer.add(refs.panel(), BorderLayout.NORTH);

        DefaultTableModel model = GuiAuditTable.createModel();
        JTable table = GuiAuditTable.buildTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(GuiTheme.rowEven());
        sp.getViewport().setBackground(GuiTheme.rowEven());
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, 320));
        outer.add(sp, BorderLayout.CENTER);
        outer.add(GuiAuditLegend.build(panBg), BorderLayout.SOUTH);

        wireFilter(refs, sorter, table);
        wireButtons(refs, model, outer);
        new Thread(() -> reload(model, outer, refs), "AuditLoad").start();

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(pane.getStyledDocument().getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    static void reload(DefaultTableModel model, JPanel outer, ToolbarRefs refs) {
        List<AuditLogEntry> entries = AuditLogger.getInstance().readRecent(2000);
        if (!outer.isDisplayable()) return;
        SwingUtilities.invokeLater(() -> {
            if (!outer.isDisplayable()) return;
            model.setRowCount(0);
            for (AuditLogEntry e : entries) {
                String shortDetail = e.detail().length() > MAX_DETAIL
                        ? e.detail().substring(0, MAX_DETAIL) + "…" : e.detail();
                model.addRow(new Object[]{e.timestamp(), e.user(), e.action(), shortDetail, e.detail()});
            }
            refs.countLbl().setText(model.getRowCount() + " Einträge");
        });
    }

    private static void wireFilter(ToolbarRefs refs, TableRowSorter<DefaultTableModel> sorter, JTable table) {
        refs.filterField().getDocument().addDocumentListener(docListener(() -> {
            String q = refs.filterField().getText().trim();
            sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + q));
            refs.countLbl().setText(table.getRowCount() + " Einträge");
        }));
    }

    private static void wireButtons(ToolbarRefs refs, DefaultTableModel model, JPanel outer) {
        refs.refreshBtn().addActionListener(e ->
                new Thread(() -> reload(model, outer, refs), "AuditLoad").start());
        refs.clearBtn().addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(null, "Audit-Log wirklich leeren?",
                    "Bestätigung", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            try {
                AuditLogger.getInstance().clear();
                model.setRowCount(0);
                refs.countLbl().setText("0 Einträge");
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(null, "Keine Berechtigung: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static ToolbarRefs buildToolbar(Color panBg, Color bg) {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(panBg);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 0, 1, BORDER), new EmptyBorder(5, 10, 5, 10)));

        JTextField filterField = new JTextField();
        filterField.setFont(MONO_XS);
        filterField.setForeground(FG);
        filterField.setBackground(bg);
        filterField.setCaretColor(ACCENT);
        filterField.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(2, 6, 2, 6)));
        filterField.putClientProperty("JTextField.placeholderText", "Filter…");

        JLabel countLbl = new JLabel("–");
        countLbl.setFont(MONO_XS);
        countLbl.setForeground(FG_DIM);
        countLbl.setBorder(new EmptyBorder(0, 8, 0, 0));

        JPanel left = new JPanel(new BorderLayout(4, 0));
        left.setOpaque(false);
        left.add(filterField, BorderLayout.CENTER);
        left.add(countLbl,    BorderLayout.EAST);

        JButton refreshBtn = toolBtn("↻", ACCENT);
        JButton clearBtn   = toolBtn("🗑", WARN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        right.add(clearBtn);

        toolbar.add(left,  BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);
        return new ToolbarRefs(toolbar, filterField, countLbl, refreshBtn, clearBtn);
    }

    static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(2, 7, 2, 7)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        return b;
    }

    static javax.swing.event.DocumentListener docListener(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        };
    }
}