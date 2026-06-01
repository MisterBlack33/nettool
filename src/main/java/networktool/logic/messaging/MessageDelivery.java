package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.OsDetector;
import main.java.networktool.util.PlatformUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Enthält die einzelnen Übertragungsmethoden für Nachrichten.
 * Wird ausschließlich von {@link MessageSender} aufgerufen.
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

    // ── WinRM ─────────────────────────────────────────────────────────────

    static boolean tryWinRM(String ip, String message) {
        if (!OsDetector.isOpen(ip, 5985)) {
            System.out.println("  WinRM (5985) nicht offen → Enable-PSRemoting -Force auf Ziel");
            return false;
        }
        System.out.println("  Methode : WinRM / PowerShell-Remoting");
        String m = PlatformUtils.escapePowerShell(message);
        String script =
                "Add-Type -AssemblyName System.Windows.Forms; " +
                        "Add-Type -AssemblyName System.Drawing; " +
                        "$n = New-Object System.Windows.Forms.NotifyIcon; " +
                        "$n.Icon = [System.Drawing.SystemIcons]::Information; " +
                        "$n.Visible = $true; $n.BalloonTipTitle = 'NetTool'; " +
                        "$n.BalloonTipText = '" + m + "'; " +
                        "$n.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info; " +
                        "$n.ShowBalloonTip(8000); Start-Sleep 9; $n.Dispose()";
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"powershell",
                    "-NonInteractive", "-WindowStyle", "Hidden", "-Command",
                    "Invoke-Command -ComputerName " + ip + " -ScriptBlock { " + script + " }"});
            String err = readStream(p.getErrorStream());
            p.waitFor();
            if (p.exitValue() == 0) { System.out.println("  ✔ WinRM: BalloonTip gesendet."); return true; }
            System.out.println("  ✕ WinRM: " + (err.isBlank() ? "fehlgeschlagen"
                    : err.lines().findFirst().orElse("").trim()));
        } catch (Exception e) { System.out.println("  ✕ PowerShell: " + e.getMessage()); }
        return false;
    }

    // ── SSH ───────────────────────────────────────────────────────────────

    static boolean trySsh(String ip, String message, boolean mac) {
        if (!OsDetector.isOpen(ip, 22)) {
            System.out.println("  SSH (22) nicht offen.");
            return false;
        }
        System.out.println("  Methode : SSH → " + (mac ? "osascript" : "notify-send"));
        // PlatformUtils.escapeSshArg statt eigenem shell()-Helper
        String safe = PlatformUtils.escapeSshArg(message);
        String cmd  = mac
                ? "osascript -e 'display notification \"" + safe + "\" with title \"NetTool\"'"
                : "DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/$(id -u)/bus "
                + "notify-send 'NetTool' '" + safe + "'";
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    "ssh", "-o", "ConnectTimeout=3", "-o", "StrictHostKeyChecking=no",
                    "-o", "BatchMode=yes", ip, cmd});
            String err = readStream(p.getErrorStream());
            p.waitFor();
            if (p.exitValue() == 0) { System.out.println("  ✔ SSH: gesendet."); return true; }
            System.out.println(err.contains("publickey")
                    ? "  ✕ SSH-Key fehlt → ssh-copy-id user@" + ip
                    : "  ✕ SSH: " + err.lines().findFirst().orElse("").trim());
        } catch (Exception e) { System.out.println("  ✕ SSH: " + e.getMessage()); }
        return false;
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