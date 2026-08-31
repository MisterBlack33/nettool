package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.os.OsDetector;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Enthält die einzelnen Übertragungsmethoden für Nachrichten.
 * Wird ausschließlich von {@link MessageSender} aufgerufen.
 *
 * WinRM/SSH-Übertragung ausgelagert in {@link MessageDeliveryWinRm} und
 * {@link MessageDeliverySsh} — diese Klasse bleibt Fassade mit stabiler API.
 *
 * Sicherheit: jede IP, die in einen exec()-Aufruf oder PowerShell-Skript-
 * String eingebettet wird, wird zuvor über {@link main.java.networktool.util.PlatformUtils#isSafeIp}
 * validiert (Command-/Script-Injection-Schutz).
 */
final class MessageDelivery {

    private MessageDelivery() {}

    static final int TIMEOUT_MS = 3000;

    // ── NetTool-Listener ──────────────────────────────────────────────────

    static boolean tryListener(String ip, String message) {
        if (!OsDetector.isOpen(ip, MessageSender.NETTOOL_LISTENER_PORT)) return false;
        System.out.println("  Methode : NetTool-Listener (Port " + MessageSender.NETTOOL_LISTENER_PORT + ")");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, MessageSender.NETTOOL_LISTENER_PORT), TIMEOUT_MS);
            s.getOutputStream().write((message + "\n").getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            System.out.println("  ✔ Nachricht übertragen.");
            return true;
        } catch (Exception e) {
            System.out.println("  ✕ Listener: " + e.getMessage());
            return false;
        }
    }

    // ── ntfy.sh ───────────────────────────────────────────────────────────

    static void tryNtfy(String topic, String message) {
        System.out.println("  Methode : ntfy.sh → Topic \"" + topic + "\"");
        try {
            HttpURLConnection c = (HttpURLConnection)
                    new URL("https://ntfy.sh/" + topic).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("Title", "NetTool");
            c.setRequestProperty("Priority", "default");
            c.setRequestProperty("Tags", "bell");
            c.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
            System.out.println(c.getResponseCode() == 200
                    ? "  ✔ ntfy.sh: gesendet."
                    : "  ✕ ntfy.sh: HTTP " + c.getResponseCode());
            c.disconnect();
        } catch (Exception e) {
            System.out.println("  ✕ ntfy.sh: " + e.getMessage());
        }
    }

    // ── WinRM / SSH (delegiert) ───────────────────────────────────────────

    static boolean tryWinRM(String ip, String message) {
        return MessageDeliveryWinRm.tryWinRM(ip, message);
    }

    static boolean trySsh(String ip, String message, boolean mac) {
        return MessageDeliverySsh.trySsh(ip, message, mac);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    static String readStream(InputStream is) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append("\n");
            return sb.toString();
        } catch (IOException e) { return ""; }
    }
}