package main.java.networktool.gui;

import main.java.networktool.logic.analysis.TracerouteRunner;
import main.java.networktool.filter.JsonExporter;
import main.java.networktool.filter.ScanFilter;
import main.java.networktool.logic.scan.NetworkHostScanner;
import main.java.networktool.logic.scan.NetworkInfo;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.SubnetDetector;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;

import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Mehrstufige Scan-Dialoge für {@link GuiMenuHandler}.
 * Jede Methode startet eine Input-Kaskade und führt dann den Scan asynchron aus.
 */
public final class GuiScanActions {

    private GuiScanActions() {}

    // ── CIDR-Scan ─────────────────────────────────────────────────────────

    public static void handleCidrScan(GuiInputPanel input, GuiOutputPanel output,
                                      GuiTableRenderer tables, GuiMenuHandler handler) {
        input.ask("CIDR (z.B. 192.168.1.0/24):", cidr -> handler.runAsync(() -> {
            List<ScanResult> results = NetworkScanner.scanCIDR(cidr);
            tables.showScanTable(results);
            input.ask("Hostname-Filter Regex (leer = überspringen):", regex -> {
                List<ScanResult> f1 = applyRegex(results, regex, tables);
                input.ask("OS + Port-Filter (z.B. linux 22, leer = überspringen):", filter -> {
                    List<ScanResult> f2 = applyOsPort(f1, filter, tables, output);
                    input.ask("Als JSON speichern? (j/n):", yn -> {
                        if (yn.equalsIgnoreCase("j") || yn.equalsIgnoreCase("y"))
                            JsonExporter.save(f2, "scan_result.json");
                    });
                });
            });
        }));
    }

    // ── Filter-Scan ───────────────────────────────────────────────────────

    public static void handleFilterScan(GuiInputPanel input, GuiMenuHandler handler) {
        input.ask("OS-Filter (windows/linux/android/apple/alle):", os ->
                input.ask("Hostname-Filter (leer = alle):", hn ->
                        handler.runAsync(() -> NetworkInfo.scanWithFilter(os, hn))));
    }

    // ── Netzwerkinfo + Hop-Analyse ────────────────────────────────────────

    /**
     * Scannt alle Hosts und zeigt dann EINE Hop-Übersichtstabelle.
     * Spalten: Start-IP | Ziel-IP | Hops | Latenz min/avg/max
     */
    public static void runNetworkInfoWithHops(GuiTableRenderer tables) throws Exception {
        List<String> subnets = SubnetDetector.getAllSubnets();
        if (subnets.isEmpty()) { System.out.println("Kein Subnetz gefunden."); return; }

        List<HostResult> hosts = NetworkHostScanner.scan(subnets);
        tables.showHostTable(hosts, "Netzwerkinfo");

        if (hosts.isEmpty()) { System.out.println("Keine Hosts gefunden."); return; }

        System.out.println("\n=== Hop-Analyse (wird gesammelt...) ===");

        // Hop-Daten für alle Hosts sammeln
        java.util.List<Object[]> hopRows = new java.util.ArrayList<>();
        String localIp = "lokal";
        try { localIp = java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception ignored) {}

        for (HostResult host : hosts) {
            if (Thread.currentThread().isInterrupted()) break;
            try {
                List<TracerouteRunner.HopInfo> hops =
                        TracerouteRunner.run(host.ip, 0);
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
            } catch (Exception ignored) {}
        }

        // Eine gemeinsame Tabelle ausgeben
        if (hopRows.isEmpty()) {
            System.out.println("  Keine Traceroute-Daten verfügbar.");
            return;
        }
        showHopSummaryTable(hopRows);
    }

    /** Gibt die gesammelten Hop-Daten als eine JTable im GUI aus (oder CLI-Text). */
    private static void showHopSummaryTable(java.util.List<Object[]> rows) {
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

                GUI.instance().appendText("\n=== Hop-Analyse ===\n\n",
                        GuiTheme.ACCENT);
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

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static List<ScanResult> applyRegex(List<ScanResult> r, String regex,
                                               GuiTableRenderer tables) {
        if (regex.isEmpty()) return r;
        List<ScanResult> f = ScanFilter.filterByHostnameRegex(r, regex);
        tables.showScanTable(f);
        return f;
    }

    private static List<ScanResult> applyOsPort(List<ScanResult> r, String filter,
                                                GuiTableRenderer tables, GuiOutputPanel output) {
        if (filter.isEmpty()) return r;
        String[] parts = filter.split(" ");
        if (parts.length < 2) return r;
        try {
            List<ScanResult> f = ScanFilter.filterCombined(r, parts[0], Integer.parseInt(parts[1]));
            tables.showScanTable(f);
            return f;
        } catch (NumberFormatException e) {
            output.appendText("Ungültiger Port: " + parts[1] + "\n", WARN);
            return r;
        }
    }
}