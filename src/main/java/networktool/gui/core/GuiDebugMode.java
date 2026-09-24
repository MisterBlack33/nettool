package main.java.networktool.gui.core;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.model.ScanResult;
import main.java.networktool.model.HostResult;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.util.CIDRUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static main.java.networktool.theme.GuiTheme.ACCENT;
import static main.java.networktool.theme.GuiTheme.ACCENT2;

/**
 * Zentraler GUI-Debugmodus: Aktionen werden simuliert, damit keine Netzwerk-,
 * Datei- oder Systemfunktion versehentlich ausgeführt wird.
 */
public final class GuiDebugMode {

    private static final int SAMPLE_COUNT = 250;
    private static volatile boolean enabled;

    private GuiDebugMode() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static List<ScanResult> sampleScanResults() {
        return sampleScanResults("192.168.10.0/24");
    }

    public static List<ScanResult> sampleScanResults(String cidr) {
        List<String> addresses = CIDRUtils.getAllIPs(cidr);
        int count = Math.min(SAMPLE_COUNT, addresses.size());
        List<ScanResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int number = i + 1;
            Map<Integer, String> ports = new LinkedHashMap<>();
            if (number % 2 == 0) ports.put(80, "http");
            if (number % 3 == 0) ports.put(443, "https");
            if (number % 5 == 0) ports.put(22, "ssh");
            String os = switch (number % 4) {
                case 0 -> "Windows";
                case 1 -> "Linux";
                case 2 -> "macOS";
                default -> "Android";
            };
            results.add(new ScanResult(addresses.get(i),
                    "host-" + String.format("%03d", number), ports, os));
        }
        return results;
    }

    public static List<HostResult> sampleSavedHosts() {
        List<HostResult> hosts = new ArrayList<>(SAMPLE_COUNT);
        for (int i = 1; i <= SAMPLE_COUNT; i++) {
            Map<Integer, String> ports = new LinkedHashMap<>();
            if (i % 2 == 0) ports.put(80, "http");
            if (i % 3 == 0) ports.put(443, "https");
            if (i % 5 == 0) ports.put(22, "ssh");
            String os = switch (i % 4) {
                case 0 -> "Windows";
                case 1 -> "Linux";
                case 2 -> "macOS";
                default -> "Android";
            };
            hosts.add(new HostResult("192.168.10." + i,
                    "saved-host-" + String.format("%03d", i), os,
                    "2026-09-" + String.format("%02d", 1 + (i % 28)),
                    ports, "Debug-Beispieldatensatz"));
        }
        return hosts;
    }

    static void render(String menuId, GuiOutputPanel output, GuiTableRenderer tables) {
        List<ScanResult> results = sampleScanResults();
        ScanHistory.getInstance().add("DEBUG-" + menuId, results);
        output.appendText("\n=== DEBUG-MODUS: AKTION " + menuId + " SIMULIERT ===\n"
                + "Keine echte Funktion wurde ausgeführt. Beispieldaten: "
                + SAMPLE_COUNT + " Einträge.\n", ACCENT2);
        tables.showScanTable(results);
    }

    static void renderToggle(GuiOutputPanel output, boolean active) {
        output.appendText(active
                ? "\n[DEBUG-MODUS AKTIV] Alle GUI-Aktionen liefern nur Beispieldaten.\n"
                : "\n[DEBUG-MODUS DEAKTIVIERT] Echte GUI-Aktionen sind wieder freigeschaltet.\n",
                active ? ACCENT : ACCENT2);
    }

    public static void disable() {
        enabled = false;
    }
}
