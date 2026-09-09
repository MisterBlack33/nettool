package main.java.networktool.gui.notification;

/**
 * Empfängt eingehende Nachrichten auf zwei Kanälen:
 *  1. TCP-Listener (Port 9999) – NetTool ↔ NetTool direkt im LAN, siehe {@link NotificationTcpServer}
 *  2. ntfy.sh NDJSON-Subscription – Handy/externe Geräte → PC, siehe {@link NtfySubscriptions}
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
        NtfySubscriptions.startSavedTopics();
    }

    /** Stoppt laufende Listener/Subskriptionen — nützlich für Tests. */
    public static void stop() {
        NotificationTcpServer.stop();
        NtfySubscriptions.stopAll();
    }

    public static void subscribeNewTopic(String topic) {
        NtfySubscriptions.subscribe(topic);
    }

    /** Legacy-Wrapper für bestehende Tests und kompatible Aufrufer. */
    static NtfyJsonParser.NtfyEvent parseNtfyJson(String json) {
        return NtfyJsonParser.parse(json);
    }
}
