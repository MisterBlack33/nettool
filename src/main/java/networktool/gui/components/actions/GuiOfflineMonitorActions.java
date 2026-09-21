package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.notification.LocalToast;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.messaging.WebhookDelivery;
import main.java.networktool.logic.scan.schedule.OfflineThresholdMonitor;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import static main.java.networktool.theme.GuiTheme.*;

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
            output.appendText("  " + StatusTags.FEHLER + " Ungültige Stundenzahl\n", WARN);
            return;
        }
        String webhook = rawWebhook == null ? "" : rawWebhook.trim();
        OfflineThresholdMonitor.getInstance().start(hours, CHECK_INTERVAL_MIN,
                msg -> notifyOffline(msg, webhook, output));
        AuditLogger.getInstance().log("OFFLINE_MONITOR_START", hours + "h");
        output.appendText("  " + StatusTags.OK + " Offline-Monitor aktiv (> " + hours + " h)\n", ACCENT2);
    }

    static void stop(GuiOutputPanel output) {
        OfflineThresholdMonitor.getInstance().stop();
        AuditLogger.getInstance().log("OFFLINE_MONITOR_STOP", "");
        output.appendText("  " + StatusTags.FEHLER + " Offline-Monitor gestoppt\n", WARN);
    }

    static void notifyOffline(String msg, String webhook, GuiOutputPanel output) {
        AuditLogger.getInstance().log("OFFLINE_ALERT", msg);
        output.appendText("\n  " + msg + "\n", WARN);
        LocalToast.show("NetTool – Offline", msg);
        if (!webhook.isBlank()) WebhookDelivery.send(webhook, msg);
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
