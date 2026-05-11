package main.java.networktool_v3.filter;

import main.java.networktool_v3.gui.GUI;
import main.java.networktool_v3.model.HostResult;

import java.util.Comparator;
import java.util.List;

/**
 * Gibt eine Liste von {@link HostResult}s aus –
 * im GUI-Modus als Tabelle, im CLI-Modus als formatierte Konsolenausgabe.
 */
public final class HostResultPrinter {

    private HostResultPrinter() {}

    private static final String CLI_HEADER_FORMAT = "%-15s %-45s %s%n";
    private static final String CLI_SEPARATOR     = "-".repeat(80);

    public static void print(List<HostResult> list, String label) {
        if (GUI.isGuiActive()) {
            GUI.instance().showHostTable(list, label);
            return;
        }
        printCli(list, label);
    }

    private static void printCli(List<HostResult> list, String label) {
        list.sort(Comparator.comparingInt(r -> ipToInt(r.ip)));
        System.out.println("\n=== " + label + " (" + list.size() + " Gerät(e)) ===");
        System.out.printf(CLI_HEADER_FORMAT, "IP", "Hostname [MAC]", "OS / Gerät");
        System.out.println(CLI_SEPARATOR);
        for (HostResult r : list) {
            System.out.printf(CLI_HEADER_FORMAT, r.ip, r.hostname, r.os);
        }
    }

    private static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        int result = 0;
        for (String p : parts) {
            try { result = (result << 8) | Integer.parseInt(p.trim()); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
