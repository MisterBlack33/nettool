package main.java.networktool_v3.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Zentrale Konfiguration und Fabrik für alle JTables der Anwendung.
 *
 * Spalten-Presets:
 *   HOST  (3 Sp.): IP | Hostname | OS / Gerät
 *   SCAN  (4 Sp.): IP | Hostname | OS | Offene Ports
 *   SAVED (5 Sp.): IP | Hostname | OS | Gespeichert am | Notiz
 *   TRACE (4 Sp.): Hop | IP | Hostname | Latenz
 *
 * fillsViewportHeight = false → verhindert leere Extra-Zeilen unter der Tabelle.
 */
public final class TableConfig {

    private TableConfig() {}

    // ── Zeilenhöhe ────────────────────────────────────────────────────────
    public static final int ROW_HEIGHT = 26;

    // ── Spaltenbreiten-Presets ────────────────────────────────────────────
    // HOST (3): IP | Hostname | OS/Gerät (letzte Spalte füllt Rest)
    public static final int[] WIDTHS_HOST  = {120, 280, 160};
    // SCAN (4): IP | Hostname | OS | Ports (letzte füllt Rest)
    public static final int[] WIDTHS_SCAN  = {120, 240, 150, 120};
    /** IP | Hostname | OS | Ports | Gespeichert am | Notiz */
    public static final int[] WIDTHS_SAVED     = {115, 210, 130, 110, 145, 160};
    /** IP | Hostname | OS | Ports | Gespeichert am | Kategorie | Notiz */
    public static final int[] WIDTHS_SAVED_ALL = {105, 190, 120, 110, 138, 100, 150};
    // TRACE (4): Hop | IP | Hostname | Latenz (letzte füllt Rest)
    public static final int[] WIDTHS_TRACE = {38, 120, 200, 150};

    // ── Farben ────────────────────────────────────────────────────────────
    public static final Color ROW_BG_EVEN   = GuiTheme.ROW_EVEN;
    public static final Color ROW_BG_ODD    = GuiTheme.ROW_ODD;
    public static final Color HEADER_BG     = new Color(0x0A, 0x16, 0x24);
    public static final Color HEADER_BORDER = GuiTheme.ACCENT;

    // Notiz-Spalte: leicht gelblicher Ton damit sie optisch erkennbar ist
    public static final Color NOTES_FG      = new Color(0xFF, 0xE0, 0x82);
    public static final Color NOTES_BG_EVEN = new Color(0x13, 0x11, 0x08);
    public static final Color NOTES_BG_ODD  = new Color(0x19, 0x16, 0x0B);

    // ── Index der Notiz-Spalte in WIDTHS_SAVED ────────────────────────────
    public static final int SAVED_COL_NOTES = 5;

    // ── Fabrik-Methoden ───────────────────────────────────────────────────

    /**
     * Standard-Tabelle: Spalte 2 bekommt OS-Farbe, Spalte 3 = FG_DIM.
     */
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

    /**
     * Tabelle mit editierbarer Notiz-Spalte (Index {@link #SAVED_COL_NOTES}).
     * Alle anderen Spalten sind schreibgeschützt.
     * Notiz-Spalte bekommt eigene Hintergrund- und Vordergrundfarbe.
     */
    public static JTable buildSavedTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == SAVED_COL_NOTES;
            }

            @Override
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component c = super.prepareRenderer(tcr, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else if (col == SAVED_COL_NOTES) {
                    // Notiz-Spalte: eigene Farben
                    c.setBackground(row % 2 == 0 ? NOTES_BG_EVEN : NOTES_BG_ODD);
                    c.setForeground(NOTES_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                    Object val = getValueAt(convertRowIndexToModel(row), 2);
                    String os  = val != null ? val.toString() : "";
                    c.setForeground(col == 2 ? osColor(os) : col == 3 ? FG_DIM : FG);
                }
                c.setFont(col == SAVED_COL_NOTES
                        ? new Font("JetBrains Mono", Font.ITALIC, 12)
                        : MONO_S);
                return c;
            }
        };

        // Editor für Notiz-Spalte
        JTextField notesEditor = new JTextField();
        notesEditor.setBackground(NOTES_BG_EVEN);
        notesEditor.setForeground(NOTES_FG);
        notesEditor.setCaretColor(NOTES_FG);
        notesEditor.setFont(new Font("JetBrains Mono", Font.ITALIC, 12));
        notesEditor.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        table.getColumnModel().getColumn(SAVED_COL_NOTES)
                .setCellEditor(new DefaultCellEditor(notesEditor));

        // Editor für OS-Spalte (Index 2) – normaler weißer Stil
        JTextField osEditor = new JTextField();
        osEditor.setBackground(new java.awt.Color(0x0D, 0x11, 0x17));
        osEditor.setForeground(new java.awt.Color(0xC8, 0xD8, 0xE8));
        osEditor.setCaretColor(ACCENT);
        osEditor.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        osEditor.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        table.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(osEditor));

        applyBaseStyle(table, WIDTHS_SAVED);
        return table;
    }


    /**
     * Tabelle für die "Alle"-Ansicht mit 7 Spalten (inkl. Kategorie-Spalte).
     * Notiz-Spalte = Index 6 (letzte), editierbar.
     * OS-Spalte = Index 2, editierbar.
     */
    public static JTable buildSavedTableAll(DefaultTableModel model) {
        final int COL_NOTES_ALL = 6; // letzte Spalte der 7-Spalten-Tabelle
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COL_NOTES_ALL || col == 2;
            }

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
                    String os  = val != null ? val.toString() : "";
                    c.setForeground(col == 2 ? osColor(os) : col == 5 ? ACCENT : col >= 3 ? FG_DIM : FG);
                }
                c.setFont(col == COL_NOTES_ALL
                        ? new Font("JetBrains Mono", Font.ITALIC, 12)
                        : MONO_S);
                return c;
            }
        };

        // Editor Notiz-Spalte
        JTextField notesEditor = new JTextField();
        notesEditor.setBackground(NOTES_BG_EVEN);
        notesEditor.setForeground(NOTES_FG);
        notesEditor.setCaretColor(NOTES_FG);
        notesEditor.setFont(new Font("JetBrains Mono", Font.ITALIC, 12));
        notesEditor.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        table.getColumnModel().getColumn(COL_NOTES_ALL)
                .setCellEditor(new DefaultCellEditor(notesEditor));

        // Editor OS-Spalte
        JTextField osEditor = new JTextField();
        osEditor.setBackground(new java.awt.Color(0x0D, 0x11, 0x17));
        osEditor.setForeground(new java.awt.Color(0xC8, 0xD8, 0xE8));
        osEditor.setCaretColor(ACCENT);
        osEditor.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        osEditor.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        table.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(osEditor));

        applyBaseStyle(table, WIDTHS_SAVED_ALL);
        return table;
    }

    // ── Gemeinsames Basis-Styling ─────────────────────────────────────────

    public static void applyBaseStyle(JTable table, int[] widths) {
        table.setBackground(ROW_BG_EVEN);
        table.setForeground(FG);
        table.setFont(MONO_S);
        table.setRowHeight(ROW_HEIGHT);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(false);
        // LAST_COLUMN: alle Spalten außer der letzten haben fixe Breite,
        // die letzte Spalte füllt den restlichen Platz → keine leere Geister-Spalte
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
            // Letzte Spalte: kein MaxWidth setzen → wächst mit Fensterbreite
            // Alle anderen: fixe Breite damit sie nicht verrutschen
            if (i < widths.length - 1) {
                col.setMaxWidth(widths[i]);
            } else {
                col.setMaxWidth(Integer.MAX_VALUE);  // letzte Spalte füllt Rest
            }
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

    public static int totalWidth(int[] widths) {
        int sum = 0;
        for (int w : widths) sum += w;
        return sum;
    }
}