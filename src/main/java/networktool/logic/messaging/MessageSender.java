package main.java.networktool.logic.messaging;

import main.java.networktool.gui.notification.LocalToast;
import main.java.networktool.gui.notification.NotificationListener;
import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logic.error.ScanContext;
import main.java.networktool.logic.error.ScanFailure;
import main.java.networktool.logic.scan.host.ScanErrorClassifier;
import main.java.networktool.logging.DebugLogger;

/**
 * Sendet Benachrichtigungen an Netzwerk-Hosts.
 * Delegiert an {@link MessageDelivery} für die eigentliche Übertragung.
 *
 * Reihenfolge:
 *  1. ntfy.sh (falls Topic angegeben)  → Handy / PC mit App
 *  2. NetTool-Listener Port 9999       → NetTool auf Ziel läuft
 *  3. WinRM                            → Windows-Ziel mit PSRemoting
 *  4. SSH                              → Linux / macOS mit Key-Auth
 *  Fallback: {@link LocalToast} + Hinweise
 */
public final class MessageSender {

    private MessageSender() {}

    /** Port des eingebauten TCP-Listeners. */
    public static final int NETTOOL_LISTENER_PORT = 9999;

    public static void send(String targetIp, String message, String ntfyTopic) {
        printHeader(targetIp, message, ntfyTopic);
        try {
            boolean ntfySent = false;

            // ntfy immer zuerst (Topic angegeben) – zählt als erfolgreich gesendet
            if (!ntfyTopic.isBlank()) {
                ntfySent = MessageDelivery.tryNtfy(ntfyTopic, message);
            }

            if (!java.net.InetAddress.getByName(targetIp)
                    .isReachable(MessageDelivery.TIMEOUT_MS)) {
                if (!ntfySent)
                    System.out.println("  Ziel nicht erreichbar – nur ntfy-Kanal möglich.");
                System.out.println("\n═══════════════════════════════════════════════");
                return;
            }

            String os = OsDetector.detect(targetIp).toLowerCase();
            System.out.println("  OS-Heuristik : " + OsDetector.detect(targetIp));
            System.out.println();

            boolean localSent = MessageDelivery.tryListener(targetIp, message);
            if (!localSent && os.contains("windows")) localSent = MessageDelivery.tryWinRM(targetIp, message);
            if (!localSent && isUnix(os)) localSent = MessageDelivery.trySsh(targetIp, message, isMac(os));
            if (!localSent && !ntfySent) printFallbackHints(targetIp);

        } catch (java.io.IOException e) {
            logFailure(targetIp, e);
            System.err.println("  Fehler bei der Nachrichtenübertragung.");
        } catch (RuntimeException e) {
            logFailure(targetIp, e);
            System.err.println("  Fehler bei der Nachrichtenübertragung.");
        }
        System.out.println("\n═══════════════════════════════════════════════");
    }

    public static void send(String targetIp, String message) {
        send(targetIp, message, "");
    }

    /** Zeigt Toast auf eigenem PC. Delegiert an {@link LocalToast}. */
    public static void showLocalToast(String title, String message) {
        LocalToast.show(title, message);
    }

    /** Startet eingehenden Listener. Delegiert an {@link NotificationListener}. */
    public static void startListener() {
        NotificationListener.start();
    }

    /** Stoppt den Listener (nur für Tests). */
    public static void stopListener() {
        NotificationListener.stop();
    }

    private static boolean isUnix(String os) {
        return os.contains("linux") || os.contains("unix")
                || os.contains("macos") || os.contains("apple");
    }
    private static boolean isMac(String os) {
        return os.contains("macos") || os.contains("apple");
    }

    private static void logFailure(String targetIp, Throwable error) {
        ScanFailure failure = ScanFailure.from(
                new ScanContext("unknown", "message-send", targetIp, -1), error);
        DebugLogger.getInstance().log(failure.kind() == ScanErrorClassifier.Kind.TIMEOUT
                ? "FINE" : "WARN", "[MessageSender] " + failure.describe());
    }

    private static void printHeader(String ip, String msg, String topic) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           Nachricht senden                   ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("  Ziel     : " + ip);
        if (!topic.isBlank()) System.out.println("  ntfy-Topic: " + topic);
        System.out.println("  Nachricht: " + msg);
        System.out.println();
    }

    private static void printFallbackHints(String ip) {
        System.out.println("  Kein Übertragungsweg verfügbar. Optionen:");
        System.out.println("  • NetTool auf Ziel öffnen (Port 9999 öffnet automatisch)");
        System.out.println("  • WinRM: Enable-PSRemoting -Force  (Admin auf Windows-Ziel)");
        System.out.println("  • SSH-Key: ssh-copy-id user@" + ip + "  (Linux/Mac-Ziel)");
        System.out.println("  • ntfy-App installieren + Topic angeben (Handy)");
        LocalToast.show("NetTool – nicht gesendet an " + ip, "Kein Übertragungsweg");
    }
}
