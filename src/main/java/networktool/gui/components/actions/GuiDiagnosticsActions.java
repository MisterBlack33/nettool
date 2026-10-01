package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.core.GuiStatusReporter;
import main.java.networktool.gui.core.GuiToggleAction;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.discovery.ArpMonitor;
import main.java.networktool.logic.analysis.probe.IpInspector;
import main.java.networktool.logic.analysis.probe.PingMonitor;
import main.java.networktool.logic.scan.schedule.PortChangeMonitor;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.transfer.BandwidthTester;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        String state = "SecMon: " + (secMon.isActive() ? "an" : "aus")
                + "  ARP: " + (arpMon.isActive() ? "an" : "aus")
                + "  Port: " + (portMon.isActive() ? "an (" + portMon.getInterval() + "min)" : "aus");
        String[] options = {"SecurityMonitor", "ARP-Monitor", "Port-Monitor"};
        int choice = JOptionPane.showOptionDialog(null, state, "Sicherheits-Monitor",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        switch (choice) {
            case 0 -> toggleSecurityMonitor(output, secMon);
            case 1 -> toggleArpMonitor(output, arpMon);
            case 2 -> togglePortMonitor(input, output, portMon);
        }
    }

    private static void toggleSecurityMonitor(GuiOutputPanel output, SecurityMonitor secMon) {
        GuiToggleAction.toggle(new GuiToggleAction.Parameters(
                secMon::isActive,
                () -> {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    secMon.start(topic != null ? topic : "");
                    if (secMon.isActive()) {
                        GuiStatusReporter.ok(output, "SecurityMonitor aktiv");
                    } else {
                        GuiStatusReporter.warning(output, "SecurityMonitor konnte nicht gestartet werden");
                    }
                },
                () -> {
                    secMon.stop();
                    if (secMon.isActive()) {
                        GuiStatusReporter.warning(output, "SecurityMonitor konnte nicht gestoppt werden");
                    } else {
                        GuiStatusReporter.ok(output, "SecurityMonitor gestoppt");
                    }
                }));
    }

    private static void toggleArpMonitor(GuiOutputPanel output, ArpMonitor arpMon) {
        GuiToggleAction.toggle(new GuiToggleAction.Parameters(
                arpMon::isActive,
                () -> {
                    String topic = GuiContextMenu.promptNtfyTopic();
                    AuditLogger.getInstance().log("ARP_MONITOR_START", "");
                    arpMon.start(topic != null ? topic : "");
                    if (arpMon.isActive()) {
                        GuiStatusReporter.ok(output, "ARP-Monitor aktiv");
                    } else {
                        GuiStatusReporter.warning(output, "ARP-Monitor konnte nicht gestartet werden");
                    }
                },
                () -> {
                    AuditLogger.getInstance().log("ARP_MONITOR_STOP", "");
                    arpMon.stop();
                    if (arpMon.isActive()) {
                        GuiStatusReporter.warning(output, "ARP-Monitor konnte nicht gestoppt werden");
                    } else {
                        GuiStatusReporter.ok(output, "ARP-Monitor gestoppt");
                    }
                }));
    }

    private static void togglePortMonitor(GuiInputPanel input, GuiOutputPanel output, PortChangeMonitor portMon) {
        GuiToggleAction.toggle(new GuiToggleAction.Parameters(
                portMon::isActive,
                () -> input.ask("Intervall (min):", minStr -> {
                    try {
                        int min = Integer.parseInt(minStr.trim());
                        String topic = GuiContextMenu.promptNtfyTopic();
                        AuditLogger.getInstance().log("PORT_MONITOR_START", min + "min");
                        portMon.start(min, topic != null ? topic : "");
                        if (portMon.isActive()) {
                            GuiStatusReporter.ok(output, "Port-Monitor aktiv (" + min + " min)");
                        } else {
                            GuiStatusReporter.warning(output, "Port-Monitor konnte nicht gestartet werden");
                        }
                    } catch (NumberFormatException e) {
                        LOG.log(Level.FINE, "Ungültiges Port-Monitor-Intervall \"" + minStr + "\"", e);
                        GuiStatusReporter.error(output, "Ungültige Zahl");
                    }
                }),
                () -> {
                    AuditLogger.getInstance().log("PORT_MONITOR_STOP", "");
                    portMon.stop();
                    if (portMon.isActive()) {
                        GuiStatusReporter.warning(output, "Port-Monitor konnte nicht gestoppt werden");
                    } else {
                        GuiStatusReporter.ok(output, "Port-Monitor gestoppt");
                    }
                }));
    }
}