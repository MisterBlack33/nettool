package main.java.networktool.gui.notification;

/**
 * Empfängt eingehende Nachrichten auf zwei Kanälen:
 *  1. TCP-Listener (Port 9999) – NetTool ↔ NetTool direkt im LAN, siehe {@link NotificationTcpServer}
 *  2. ntfy.sh NDJSON-Subscription – Handy/externe Geräte → PC, siehe {@link NtfySubscriptionManager}
 *
 * Startup-Verhalten:
 *  - Abonnierte Topics werden NICHT beim Start in die Konsole geschrieben
 *  - Erst wenn eine Nachricht eintrifft, wird ausgegeben
 *  - since=all verhindert das Wiederholen alter Nachrichten
 */
public final class NotificationListener {

    private NotificationListener() {}

    public static void start() {
        NotificationTcpServer.start();
        NtfySubscriptionManager.startSavedTopics();
    }

    /** Stoppt laufende Listener/Subskriptionen — nützlich für Tests. */
    public static void stop() {
        NotificationTcpServer.stop();
        NtfySubscriptionManager.stopAll();
    }

    public static void subscribeNewTopic(String topic) {
        NtfySubscriptionManager.subscribe(topic);
    }

    /** @deprecated nur für bestehende Tests – neue Aufrufer nutzen {@link NtfyJsonParser#parse}. */
    @Deprecated
    static NtfyJsonParser.NtfyEvent parseNtfyJson(String json) {
        return NtfyJsonParser.parse(json);
    }
}
