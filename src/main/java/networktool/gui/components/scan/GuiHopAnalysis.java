package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.logic.analysis.probe.TracerouteRunner;
import main.java.networktool.logic.scan.host.NetworkHostScanner;
import main.java.networktool.logic.scan.host.SubnetDetector;
import main.java.networktool.model.HostResult;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.java.networktool.theme.GuiTheme;
import main.java.networktool.util.TableConfig;

/**
 * Sammelt für alle Hosts eines Netzes die Traceroute-Hop-Anzahl und Latenz
 * und stellt sie als eine gemeinsame Übersichtstabelle dar (Menüpunkt "10").
 */
final class GuiHopAnalysis {

    private static final Logger LOG = Logger.getLogger(GuiHopAnalysis.class.getName());

    private GuiHopAnalysis() {}

    /**
     * Scannt alle Hosts und zeigt dann EINE Hop-Übersichtstabelle.
     * Spalten: Start-IP | Ziel-IP | Hops | Latenz min/avg/max
     */
    static void runWithHops(GuiTableRenderer tables) throws Exception {
        List<String> subnets = SubnetDetector.getAllSubnets();
        if (subnets.isEmpty()) { System.out.println("Kein Subnetz gefunden."); return; }

        List<HostResult> hosts = NetworkHostScanner.scan(subnets);
        tables.showHostTable(hosts, "Netzwerkinfo");

        if (hosts.isEmpty()) { System.out.println("Keine Hosts gefunden."); return; }

        System.out.println("\n=== Hop-Analyse (wird gesammelt...) ===");

        List<Object[]> hopRows = new ArrayList<>();
        String localIp = "lokal";
        try { localIp = java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { LOG.log(Level.FINE, "Lokale IP konnte nicht ermittelt werden, nutze Fallback", e); }

        for (HostResult host : hosts) {
            if (Thread.currentThread().isInterrupted()) break;
            try {
                List<TracerouteRunner.HopInfo> hops = TracerouteRunner.run(host.ip, 0);
                if (hops.isEmpty()) continue;

                int totalHops = hops.size();
                long minMs = Long.MAX_VALUE, maxMs = 0, sumMs = 0;
                int count = 0;
                for (var hop : hops) {
                    if (!hop.timeout && !hop.msValues.isEmpty()) {
                        long v = hop.msValues.get(0);
                        if (v < minMs) minMs = v;
                        if (v > maxMs) maxMs = v;
                        sumMs += v; count++;
                    }
                }
                String latency = count == 0 ? "–"
                        : minMs + " / " + (sumMs / count) + " / " + maxMs + " ms";

                hopRows.add(new Object[]{
                        localIp,
                        host.ip + (host.hostname.equals(host.ip) ? "" : "  (" + host.hostname + ")"),
                        totalHops,
                        latency
                });
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Hop-Analyse für Host " + host.ip + " fehlgeschlagen", e);
            }
        }

        if (hopRows.isEmpty()) {
            System.out.println("  Keine Traceroute-Daten verfügbar.");
            return;
        }
        showSummaryTable(hopRows);
    }

    /** Gibt die gesammelten Hop-Daten als eine JTable im GUI aus (oder CLI-Text). */
    private static void showSummaryTable(List<Object[]> rows) {
        if (GUI.isGuiActive()) {
            String[] cols = {"Start-IP", "Ziel-IP / Hostname", "Hops", "Latenz min/avg/max"};
            int[] widths  = {120, 300, 50, 200};
            Object[][] data = rows.toArray(new Object[0][]);

            javax.swing.SwingUtilities.invokeLater(() -> {
                javax.swing.table.DefaultTableModel model =
                        new javax.swing.table.DefaultTableModel(data, cols) {
                            @Override public boolean isCellEditable(int r, int c) { return false; }
                        };
                javax.swing.JTable table = TableConfig.buildTable(model, widths);
                int totalH = TableConfig.preferredHeight(table);

                javax.swing.JScrollPane sp = new javax.swing.JScrollPane(table,
                        javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                sp.setBackground(TableConfig.ROW_BG_EVEN);
                sp.getViewport().setBackground(TableConfig.ROW_BG_EVEN);
                sp.setBorder(new javax.swing.border.LineBorder(GuiTheme.BORDER, 1));
                sp.setPreferredSize(new java.awt.Dimension(0, Math.min(totalH, 400)));

                GUI.instance().appendText("\n=== Hop-Analyse ===\n\n", GuiTheme.ACCENT);
                javax.swing.JTextPane pane = GUI.instance().getOutputPane();
                pane.setCaretPosition(pane.getDocument().getLength());
                pane.insertComponent(sp);
                GUI.instance().appendText(
                        "\n  " + rows.size() + " Host(s) analysiert.\n",
                        GuiTheme.ACCENT2);
            });
        } else {
            System.out.println("\n=== Hop-Analyse ===");
            System.out.printf("  %-16s  %-36s  %-5s  %s%n", "Start-IP", "Ziel-IP", "Hops", "Latenz min/avg/max");
            System.out.println("  " + "─".repeat(75));
            for (Object[] row : rows)
                System.out.printf("  %-16s  %-36s  %-5s  %s%n", row[0], row[1], row[2], row[3]);
        }
    }
}
