package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.messaging.WebhookDelivery;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "Webhook" (Menü-ID "43"): sendet eine Nachricht an eine Webhook-URL. */
public final class GuiWebhookActions {

    private GuiWebhookActions() {}

    public static void handle(GuiInputPanel input, GuiOutputPanel output, GuiMenuHandler handler) {
        input.ask("Webhook-URL (http/https):", url ->
                input.ask("Nachricht:", msg -> handler.runAsync(() -> send(url, msg, output))));
    }

    static boolean send(String url, String message, GuiOutputPanel output) {
        boolean ok = WebhookDelivery.send(url, message);
        AuditLogger.getInstance().log("WEBHOOK_SEND", ok ? "ok" : "failed");
        output.appendText(ok ? "  " + StatusTags.OK + " Webhook gesendet\n"
                : "  " + StatusTags.FEHLER + " Webhook fehlgeschlagen\n", ok ? ACCENT2 : WARN);
        return ok;
    }
}
