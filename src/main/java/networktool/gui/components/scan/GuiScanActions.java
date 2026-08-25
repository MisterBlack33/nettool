package main.java.networktool.gui.components.scan;

import main.java.networktool.filter.JsonExporter;
import main.java.networktool.filter.ScanFilter;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.host.NetworkInfo;
import main.java.networktool.logic.scan.host.NetworkScanner;
import main.java.networktool.model.ScanResult;

import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Mehrstufige Scan-Dialoge für {@link GuiMenuHandler}.
 * Jede Methode startet eine Input-Kaskade und führt dann den Scan asynchron aus.
 * Hop-Sammelbericht siehe {@link GuiHopAnalysis}.
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

    /** Delegiert an {@link GuiHopAnalysis}. */
    public static void runNetworkInfoWithHops(GuiTableRenderer tables) throws Exception {
        GuiHopAnalysis.runWithHops(tables);
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