package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.logic.analysis.ArpMonitor;
import main.java.networktool.logic.analysis.IpInspector;
import main.java.networktool.logic.analysis.PingMonitor;
import main.java.networktool.logic.scan.PortChangeMonitor;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.transfer.BandwidthTester;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static networktool.theme.GuiTheme.*;

/**
 * IP-Diagnose, Bandbreitentest, Dauerping und Sicherheits-Monitore
 * (Menüpunkte "03", "15", "16", "17").
 */
public final class GuiDiagnosticsActions {

    private static final Logger LOG = Logger.getLogger(GuiDiagnosticsActions.class.getName());

    private GuiDiagnosticsActions() {}

    public static void handleDiagnose(GuiInputPanel input, GuiMenuHandler handler) {
        String[] options = {"Schnell  (ICMP + Ports + OS)", "Voll  (+ ARP + Traceroute)"};
        int choice = JOptionPane.showOptionDialog(null, "Diagnose-Modus:", "IP-Analyse",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        input.ask("Ziel-IP / Hostname:", target -> {
            if (choice == 1) {
                AuditLogger.getInstance().log("DIAGNOSE_FULL", target);
                handler.runAsync(() -> IpInspector.inspect(target));
            } else {
                AuditLogger.getInstance().log("DIAGNOSE_QUICK", target);
                handler.runAsync(() -> IpInspector.quickScan(target, 5000));
            }
        });
    }

    public static void handleBandwidthTest(GuiInputPanel input, GuiMenuHandler handler) {
        input.ask("Ziel-IP / Hostname:", ip -> handler.runAsync(() -> {
            AuditLogger.getInstance().log("BW_TEST", ip);
            BandwidthTester.testBoth(ip);
        }));
    }

    public static void handleDauerping(GuiInputPanel input, GuiMenuHandler handler) {
        input.ask("Ziel-IP:", host ->
                input.ask("Max. Sekunden (0 = ∞):", secStr -> {
                    int sec = 0;
                    try {
                        sec = Integer.parseInt(secStr.trim());
                    } catch (NumberFormatException e) {
                        LOG.log(Level.FINE, "Ungültige Sekundenangabe \"" + secStr + "\", nutze 0", e);
                    }
                    final int maxSec = sec;
                    AuditLogger.getInstance().log("DAUERPING", host + " max=" + maxSec + "s");
                    handler.runAsync(() -> PingMonitor.start(host.trim(), maxSec));
                }));
    }

    public static void handleSecurityMonitor(GuiInputPanel input, GuiOutputPanel output) {
        SecurityMonitor secMon = SecurityMonitor.getInstance();
        ArpMonitor arpMon = ArpMonitor.getInstance();
        PortChangeMonitor portMon = PortChangeMonitor.getInstance();

        String state = "SecMon: " + (secMon.isActive() ? "✔" : "✕")
                + "  ARP: " + (arpMon.isActive() ? "✔" : "✕")
                + "  Port: " + (portMon.isActive() ? "✔ (" + portMon.getInterval() + "min)" : "✕");
        String[] options = {"SecurityMonitor", "ARP-Monitor", "Port-Monitor"};
        int choice = JOptionPane.showOptionDialog(null, state, "Sicherheits-Monitor",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        switch (choice) {
            case 0 -> {
                if (secMon.isActive()) {
                    secMon.stop();
                    output.appendText("  ✕ SecurityMonitor gestoppt\n", WARN);
                } else {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    secMon.start(topic != null ? topic : "");
                    output.appendText("  ✔ SecurityMonitor aktiv\n", ACCENT2);
                }
            }
            case 1 -> {
                if (arpMon.isActive()) {
                    AuditLogger.getInstance().log("ARP_MONITOR_STOP", "");
                    arpMon.stop();
                    output.appendText("  ✕ ARP-Monitor gestoppt\n", WARN);
                } else {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    AuditLogger.getInstance().log("ARP_MONITOR_START", "");
                    arpMon.start(topic != null ? topic : "");
                    output.appendText("  ✔ ARP-Monitor aktiv\n", ACCENT2);
                }
            }
            case 2 -> {
                if (portMon.isActive()) {
                    AuditLogger.getInstance().log("PORT_MONITOR_STOP", "");
                    portMon.stop();
                    output.appendText("  ✕ Port-Monitor gestoppt\n", WARN);
                } else {
                    input.ask("Intervall (min):", minStr -> {
                        try {
                            int min = Integer.parseInt(minStr.trim());
                            String topic = GuiContextMenu.promptNtfyTopic();
                            AuditLogger.getInstance().log("PORT_MONITOR_START", min + "min");
                            portMon.start(min, topic != null ? topic : "");
                            output.appendText("  ✔ Port-Monitor aktiv (" + min + " min)\n", ACCENT2);
                        } catch (NumberFormatException e) {
                            LOG.log(Level.FINE, "Ungültiges Port-Monitor-Intervall \"" + minStr + "\"", e);
                            output.appendText("  ✕ Ungültige Zahl\n", WARN);
                        }
                    });
                }
            }
        }
    }
}
