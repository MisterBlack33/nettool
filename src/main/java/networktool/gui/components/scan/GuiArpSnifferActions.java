package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.host.ArpSighting;
import main.java.networktool.logic.scan.host.ArpSniffer;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import static main.java.networktool.theme.GuiTheme.ACCENT2;
import static main.java.networktool.theme.GuiTheme.INFO;
import static main.java.networktool.theme.GuiTheme.WARN;

/** Test-Suite-Aktion "ARP-Sniffer" (Menü-ID "32"): passive Host-Erkennung ein/aus. */
public final class GuiArpSnifferActions {

    private GuiArpSnifferActions() {}

    public static void toggle(GuiOutputPanel output) {
        ArpSniffer sniffer = ArpSniffer.getInstance();
        if (sniffer.isActive()) {
            sniffer.stop();
            AuditLogger.getInstance().log("ARP_SNIFFER_STOP", "");
            output.appendText("  " + StatusTags.OK + " ARP-Sniffer gestoppt\n", WARN);
            return;
        }
        sniffer.start(sighting -> report(output, sighting));
        AuditLogger.getInstance().log("ARP_SNIFFER_START", "");
        output.appendText("  " + StatusTags.OK + " ARP-Sniffer aktiv (passiv)\n", ACCENT2);
    }

    static String describe(ArpSighting sighting) {
        return sighting.isNewHost()
                ? "Neuer Host " + sighting.ip() + "  " + sighting.mac()
                : "MAC-Wechsel " + sighting.ip() + "  " + sighting.previousMac() + " -> " + sighting.mac();
    }

    private static void report(GuiOutputPanel output, ArpSighting sighting) {
        AuditLogger.getInstance().log(
                sighting.isNewHost() ? "ARP_SNIFFER_NEW" : "ARP_SNIFFER_MAC_CHANGE", describe(sighting));
        output.appendText("  [ARP] " + describe(sighting) + "\n", sighting.isNewHost() ? INFO : WARN);
    }
}
