package main.java.networktool_v3.filter;

import main.java.networktool_v3.gui.GUI;
import main.java.networktool_v3.model.ScanResult;

import java.util.List;

/**
 * Gibt {@link ScanResult}-Listen aus –
 * im GUI-Modus als Tabelle, im CLI-Modus als formatierte Konsolenausgabe.
 */
public final class TablePrinter {

    private TablePrinter() {}

    private static final String CLI_ROW_FORMAT = "%-15s %-25s %-15s %-20s%n";
    private static final String CLI_SEPARATOR  = "-".repeat(75);

    public static void print(List<ScanResult> results) {
        if (GUI.isGuiActive()) {
            GUI.instance().showScanTable(results);
            return;
        }
        printCli(results);
    }

    private static void printCli(List<ScanResult> results) {
        System.out.println("\n=== Ergebnisse ===");
        System.out.printf(CLI_ROW_FORMAT, "IP", "Hostname", "OS", "Ports");
        System.out.println(CLI_SEPARATOR);
        for (ScanResult r : results) {
            System.out.printf(CLI_ROW_FORMAT,
                    r.getIp(),
                    r.getHostname(),
                    r.getOsGuess(),
                    r.getOpenPorts().keySet());
        }
    }
}
