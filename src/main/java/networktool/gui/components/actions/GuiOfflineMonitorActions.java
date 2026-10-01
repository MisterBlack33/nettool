package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.core.GuiStatusReporter;
import main.java.networktool.gui.notification.LocalToast;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.messaging.WebhookDelivery;
import main.java.networktool.logic.scan.schedule.OfflineThresholdMonitor;
import main.java.networktool.security.AuditLogger;

/** Test-Suite-Aktion "Offline-Monitor" (Menü-ID "44"): Schwellenwert-Alarme, optional per Webhook. */
public final class GuiOfflineMonitorActions {

    private static final int CHECK_INTERVAL_MIN = 5;

    private GuiOfflineMonitorActions() {}

    public static void handle(GuiInputPanel input, GuiOutputPanel output) {
        if (OfflineThresholdMonitor.getInstance().isActive()) {
            stop(output);
            return;
        }
        input.ask("Offline-Schwelle in Stunden:", hours ->
                input.ask("Webhook-URL für Alarme (leer = keine):", url -> start(hours, url, output)));
    }

    static void start(String rawHours, String rawWebhook, GuiOutputPanel output) {
        int hours = parsePositive(rawHours);
        if (hours <= 0) {
            GuiStatusReporter.error(output, "Ungültige Stundenzahl");
            return;
        }
        String webhook = rawWebhook == null ? "" : rawWebhook.trim();
        OfflineThresholdMonitor monitor = OfflineThresholdMonitor.getInstance();
        boolean wasActive = monitor.isActive();
        monitor.start(hours, CHECK_INTERVAL_MIN,
                msg -> notifyOffline(msg, webhook, output));
        AuditLogger.getInstance().log("OFFLINE_MONITOR_START", hours + "h");
        if (!wasActive && monitor.isActive()) {
            GuiStatusReporter.ok(output, "Offline-Monitor aktiv (> " + hours + " h)");
        } else {
            GuiStatusReporter.warning(output, "Offline-Monitor konnte nicht gestartet werden");
        }
    }

    static void stop(GuiOutputPanel output) {
        OfflineThresholdMonitor monitor = OfflineThresholdMonitor.getInstance();
        boolean wasActive = monitor.isActive();
        monitor.stop();
        AuditLogger.getInstance().log("OFFLINE_MONITOR_STOP", "");
        if (wasActive && !monitor.isActive()) {
            GuiStatusReporter.ok(output, "Offline-Monitor gestoppt");
        } else {
            GuiStatusReporter.warning(output, "Offline-Monitor konnte nicht gestoppt werden");
        }
    }

    static void notifyOffline(String msg, String webhook, GuiOutputPanel output) {
        AuditLogger.getInstance().log("OFFLINE_ALERT", msg);
        GuiStatusReporter.warning(output, msg);
        LocalToast.show("NetTool – Offline", msg);
        if (!webhook.isBlank() && !WebhookDelivery.send(webhook, msg)) {
            GuiStatusReporter.warning(output, "Offline-Webhook fehlgeschlagen");
        }
    }

    static int parsePositive(String raw) {
        if (raw == null) return -1;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
