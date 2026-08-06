package main.java.networktool.gui.components;

import main.java.networktool.filter.ClipboardUtil;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.LastScanCache;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.TableConfig;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.java.networktool.theme.GuiTheme.*;
import static main.java.networktool.util.TableConfig.*;

public class GuiTableRenderer {

    private static final Logger LOG = Logger.getLogger(GuiTableRenderer.class.getName());

    private final GuiOutputPanel outputPanel;
    private GuiContextMenu       contextMenu;

    public GuiTableRenderer(GuiOutputPanel outputPanel) {
        this.outputPanel = outputPanel;
    }

    public void setContextMenu(GuiContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    // ── Öffentliche Tabellen-Builder ──────────────────────────────────────

    public void showHostTable(List<HostResult> rows, String title) {
        LastScanCache.updateFromHostResults(rows);
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendText("\n" + title + "\n\n", ACCENT);
            String[]   cols = {"IP", "Hostname", "OS / Gerät"};
            Object[][] data = rows.stream()
                    .sorted(Comparator.comparingInt(r -> ipToInt(r.ip)))
                    .map(r -> new Object[]{r.ip, formatHostname(r.hostname), r.os})
                    .toArray(Object[][]::new);
            embedTable(data, cols, WIDTHS_HOST);
            outputPanel.appendText(rows.size() + " Gerät(e) gefunden.\n", ACCENT2);
        });
    }

    public void showScanTable(List<ScanResult> rows) {
        LastScanCache.updateFromScanResults(rows);
        SwingUtilities.invokeLater(() -> {
            // Sicherstellen dass die Tabelle auf einer neuen Zeile beginnt
            outputPanel.appendText("\n=== Scan-Ergebnisse ===\n\n", ACCENT);
            String[]   cols = {"IP", "Hostname", "OS", "Offene Ports"};
            Object[][] data = rows.stream()
                    .map(r -> new Object[]{
                            r.getIp(), r.getHostname(),
                            r.getOsGuess(), r.getOpenPorts().keySet().toString()})
                    .toArray(Object[][]::new);
            embedTable(data, cols, WIDTHS_SCAN);
        });
    }

    // ── Tabelle einbetten ─────────────────────────────────────────────────

    private void embedTable(Object[][] data, String[] cols, int[] widths) {
        outputPanel.appendText("\n", FG); // neue Zeile vor Tabelle

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = TableConfig.buildTable(model, widths);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(0, Comparator.comparingInt(GuiTableRenderer::ipToInt));
        sorter.setComparator(2, Comparator.nullsLast(String::compareToIgnoreCase));
        table.setRowSorter(sorter);

        if (contextMenu != null) contextMenu.attach(table);
        installDoubleClickCopy(table);

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN);
        sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, Math.min(preferredHeight(table), 400)));

        JTextPane pane = outputPanel.getOutputPane();

        // Sicherstellen dass der Cursor am Zeilenende auf einer neuen Zeile steht
        ensureNewLine(pane);

        pane.setCaretPosition(pane.getDocument().getLength());
        pane.insertComponent(sp);
        outputPanel.appendText("\n\n", FG);
    }

    /**
     * Fügt einen Zeilenumbruch ein wenn das letzte Zeichen im Dokument
     * kein Newline ist — verhindert dass die Tabelle inline in Text erscheint.
     */
    private void ensureNewLine(JTextPane pane) {
        try {
            int len = pane.getDocument().getLength();
            if (len > 0) {
                String lastChar = pane.getDocument().getText(len - 1, 1);
                if (!"\n".equals(lastChar)) {
                    outputPanel.appendText("\n", FG);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Konnte Zeilenumbruch am Textende nicht prüfen", e);
        }
    }

    // ── Doppelklick-Handler ───────────────────────────────────────────────

    public static void installDoubleClickCopy(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2) return;
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || (col != 0 && col != 1)) return;
                Object val = table.getValueAt(row, col);
                if (val == null) return;
                if (col == 0) {
                    String ip = val.toString().trim();
                    if (ip.isEmpty() || ip.equals("-")) return;
                    String os = table.getColumnCount() > 2
                            ? String.valueOf(table.getValueAt(row, 2)) : "";
                    GuiRemoteActions.openInBrowser(ip, os);
                } else {
                    String text = val.toString();
                    int bracket = text.indexOf(" [");
                    if (bracket > 0) text = text.substring(0, bracket).trim();
                    ClipboardUtil.copy(text);
                }
            }
        });
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static String formatHostname(String hostname) {
        if (hostname == null) return "";
        int i = hostname.indexOf(" [");
        return i < 0 ? hostname : hostname.substring(0, i) + "  " + hostname.substring(i);
    }

    static int ipToInt(Object ipObj) {
        if (ipObj == null) return 0;
        String[] parts = ipObj.toString().split("\\.");
        int result = 0;
        for (String p : parts) {
            try { result = (result << 8) | Integer.parseInt(p.trim()); }
            catch (NumberFormatException e) { return 0; }
        }
        return result;
    }
}