package networktool.gui;

import networktool.gui.notification.NotificationListener;
import networktool.logic.analysis.WakeOnLan;
import networktool.logic.messaging.MessageSender;
import networktool.storage.NetworkStore;

import javax.swing.*;

import static networktool.gui.GuiTheme.*;

/**
 * Aktionen für das Kontextmenü, die über einfache Speichervorgänge
 * hinausgehen: Wake-on-LAN und Nachricht senden.
 */
final class ContextMenuActions {

    private ContextMenuActions() {}

    static void sendWakeOnLan(String ip, String mac, GuiMenuHandler menuHandler, GuiOutputPanel output) {
        String broadcast = WakeOnLan.deriveBroadcast(ip, 24);
        String custom = JOptionPane.showInputDialog(null,
                "<html>Broadcast-Adresse für WoL:<br>"
                        + "<small>Standard: " + broadcast + "</small></html>",
                "Wake-on-LAN", JOptionPane.PLAIN_MESSAGE);
        if (custom == null) return;

        String target = custom.isBlank() ? broadcast : custom.trim();
        menuHandler.runAsync(() -> {
            boolean ok = WakeOnLan.send(mac, target);
            output.appendText(ok
                            ? "  ⚡ WoL-Paket gesendet an " + mac + " via " + target + "\n"
                            : "  ✕ WoL fehlgeschlagen\n",
                    ok ? ACCENT2 : WARN);
        });
    }

    static void promptAndSendMessage(String ip, GuiMenuHandler menuHandler) {
        String msg = JOptionPane.showInputDialog(null,
                "Nachricht an " + ip + ":", "Nachricht senden", JOptionPane.PLAIN_MESSAGE);
        if (msg == null || msg.isBlank()) return;

        String topic = NtfyTopicPrompt.prompt();
        if (topic == null) return;

        String finalTopic = topic.trim();
        if (!finalTopic.isEmpty()) {
            NetworkStore.getInstance().saveNtfyTopic(finalTopic);
            NotificationListener.subscribeNewTopic(finalTopic);
        }
        menuHandler.runAsync(() -> MessageSender.send(ip, msg, finalTopic));
    }
}