package main.java.networktool_v3.logic.analysis;

import main.java.networktool_v3.gui.GUI;
import main.java.networktool_v3.gui.TableConfig;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;
import static main.java.networktool_v3.gui.TableConfig.*;

/**
 * Gibt Traceroute-Ergebnisse aus:
 *  - GUI-Modus: eingebettete JTable im Output-Panel
 *  - CLI-Modus: formatierter Text auf System.out
 */
public final class TracerouteRenderer {

    private TracerouteRenderer() {}

    // ── GUI ───────────────────────────────────────────────────────────────

    public static void renderGui(List<TracerouteRunner.HopInfo> hops) {
        String[] cols = {"Hop", "IP", "Hostname", "Latenz (min/avg/max)"};
        Object[][] data = hops.stream()
                .map(h -> new Object[]{h.number, h.timeout ? "*" : h.ip,
                        h.timeout ? "Timeout" : h.hostname,
                        h.timeout ? "–" : h.latencyFormatted()})
                .toArray(Object[][]::new);

        long totalMs  = hops.stream().filter(h -> !h.timeout && !h.msValues.isEmpty())
                .mapToLong(h -> h.msValues.get(0)).sum();
        int reachable = (int) hops.stream().filter(h -> !h.timeout).count();
        String summary = String.format(
                "  Hops: %d  |  erreichbar: %d  |  Timeout: %d  |  ~%d ms",
                hops.size(), reachable, hops.size() - reachable, totalMs);

        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = new DefaultTableModel(data, cols) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public Class<?> getColumnClass(int c) {
                    return c == 0 ? Integer.class : String.class;
                }
            };
            JTable table = new JTable(model) {
                @Override public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                    Component c = super.prepareRenderer(tcr, row, col);
                    boolean to = "Timeout".equals(getValueAt(row, 2));
                    if (!isRowSelected(row)) {
                        c.setBackground(row % 2 == 0 ? ROW_BG_EVEN : ROW_BG_ODD);
                        c.setForeground(col == 0 ? ACCENT : col == 3
                                ? (to ? WARN : ACCENT2) : (to ? FG_DIM : FG));
                    }
                    c.setFont(MONO_S); return c;
                }
            };
            TableConfig.applyBaseStyle(table, WIDTHS_TRACE);
            table.setFillsViewportHeight(false);

            JScrollPane sp = new JScrollPane(table,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBackground(ROW_BG_EVEN);
            sp.getViewport().setBackground(ROW_BG_EVEN);
            sp.setBorder(new LineBorder(BORDER, 1));
            sp.setPreferredSize(new Dimension(
                    0,  // volle Breite
                    Math.min(TableConfig.preferredHeight(table), 500)));

            GUI.instance().appendText("\n", FG);
            JTextPane pane = GUI.instance().getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(sp);
            GUI.instance().appendText("\n" + summary + "\n", FG_DIM);
        });
    }

    // ── CLI ───────────────────────────────────────────────────────────────

    public static void renderCli(List<TracerouteRunner.HopInfo> hops) {
        System.out.printf("  %-4s  %-18s  %-28s  %s%n", "Hop", "IP", "Hostname", "Latenz");
        System.out.println("  " + "─".repeat(72));
        int reachable = 0; long totalMs = 0;
        for (TracerouteRunner.HopInfo hop : hops) {
            if (hop.timeout) {
                System.out.printf("  %-4d  %-18s  %-28s  Timeout%n", hop.number, "*", "*");
            } else {
                System.out.printf("  %-4d  %-18s  %-28s  %s%n",
                        hop.number, trunc(hop.ip, 18),
                        trunc(hop.hostname, 28), hop.latencyFormatted());
                if (!hop.msValues.isEmpty()) { totalMs += hop.msValues.get(0); reachable++; }
            }
        }
        System.out.println("  " + "─".repeat(72));
        System.out.printf("  Hops: %d | erreichbar: %d | Timeout: %d | ~%d ms%n",
                hops.size(), reachable, hops.size() - reachable, totalMs);
    }

    private static String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}