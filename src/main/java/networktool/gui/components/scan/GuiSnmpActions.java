package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.snmp.SnmpPortTable;
import main.java.networktool.logic.analysis.snmp.SnmpWalker;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.IpValidator;
import main.java.networktool.util.StatusTags;

import java.io.PrintStream;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.WARN;

/** Test-Suite-Aktion "SNMP-Walk" (Menü-ID "31"): Portliste eines Managed Switches. */
public final class GuiSnmpActions {

    private static final String DEFAULT_COMMUNITY = "public";

    private GuiSnmpActions() {}

    public static void handleWalk(GuiInputPanel input, GuiOutputPanel output, GuiMenuHandler handler) {
        input.ask("Switch-IP / Hostname:", target ->
                input.ask("SNMP-Community (leer = public):", community -> {
                    String host = target.trim();
                    if (!IpValidator.isValidHostname(host)) {
                        output.appendText("  " + StatusTags.FEHLER + " Ungültiges Ziel\n", WARN);
                        return;
                    }
                    AuditLogger.getInstance().log("SNMP_WALK", host);
                    handler.runAsync(() -> printPortTable(host, communityOrDefault(community)));
                }));
    }

    static String communityOrDefault(String raw) {
        return raw == null || raw.isBlank() ? DEFAULT_COMMUNITY : raw.trim();
    }

    static void printPortTable(String host, String community) {
        printRows(SnmpPortTable.read(SnmpWalker.forHost(host, community)), System.out);
    }

    static void printRows(List<SnmpPortTable.Row> rows, PrintStream out) {
        if (rows.isEmpty()) {
            out.println("  Keine SNMP-Antwort (Erreichbarkeit und Community prüfen).");
            return;
        }
        out.println("  Port Name                         Status Speed");
        rows.forEach(row -> out.println(row.toLine()));
    }
}
