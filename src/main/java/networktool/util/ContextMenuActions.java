package networktool.util;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import networktool.gui.components.NtfyTopicPrompt;
import networktool.gui.core.GuiMenuHandler;
import networktool.gui.notification.NotificationListener;
import networktool.gui.panels.GuiOutputPanel;
import networktool.logic.analysis.WakeOnLan;
import networktool.logic.messaging.MessageSender;
import networktool.storage.NetworkStore;

import javax.swing.*;

import static networktool.theme.GuiTheme.*;

/**
 * Aktionen fÃƒÆ’Ã‚Â¼r das KontextmenÃƒÆ’Ã‚Â¼, die ÃƒÆ’Ã‚Â¼ber einfache SpeichervorgÃƒÆ’Ã‚Â¤nge
 * hinausgehen: Wake-on-LAN und Nachricht senden.
 */
final class ContextMenuActions {

    private ContextMenuActions() {}

    static void sendWakeOnLan(String ip, String mac, GuiMenuHandler menuHandler, GuiOutputPanel output) {
        String broadcast = WakeOnLan.deriveBroadcast(ip, 24);
        String custom = JOptionPane.showInputDialog(null,
                "<html>Broadcast-Adresse fÃƒÆ’Ã‚Â¼r WoL:<br>"
                        + "<small>Standard: " + broadcast + "</small></html>",
                "Wake-on-LAN", JOptionPane.PLAIN_MESSAGE);
        if (custom == null) return;

        String target = custom.isBlank() ? broadcast : custom.trim();
        menuHandler.runAsync(() -> {
            boolean ok = WakeOnLan.send(mac, target);
            output.appendText(ok
                            ? "  ÃƒÂ¢Ã…Â¡Ã‚Â¡ WoL-Paket gesendet an " + mac + " via " + target + "\n"
                            : "  ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¢ WoL fehlgeschlagen\n",
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

