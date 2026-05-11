package main.java.networktool_v3.gui.notification;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Zeigt eine native Benachrichtigung auf dem EIGENEN PC an.
 *
 * Windows : PowerShell NotifyIcon BalloonTip (unten rechts, kein Admin)
 * Linux/Mac: Java AWT SystemTray
 *
 * Wird aufgerufen wenn:
 *  - eine eingehende Nachricht vom {@link NotificationListener} empfangen wird
 *  - als Fallback wenn kein Übertragungsweg zum Ziel funktioniert
 */
public final class LocalToast {

    private LocalToast() {}

    public static void show(String title, String message) {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            if (showPowerShellBalloon(title, message)) return;
        }
        showAwtTray(title, message);
    }

    // ── Windows: PowerShell BalloonTip ───────────────────────────────────

    /** Startet PowerShell asynchron – blockiert nicht. */
    static boolean showPowerShellBalloon(String title, String message) {
        String t = ps(title), m = ps(message);
        String script =
                "Add-Type -AssemblyName System.Windows.Forms; " +
                "Add-Type -AssemblyName System.Drawing; " +
                "$n = New-Object System.Windows.Forms.NotifyIcon; " +
                "$n.Icon = [System.Drawing.SystemIcons]::Information; " +
                "$n.Visible = $true; " +
                "$n.BalloonTipTitle = '" + t + "'; " +
                "$n.BalloonTipText  = '" + m + "'; " +
                "$n.BalloonTipIcon  = [System.Windows.Forms.ToolTipIcon]::Info; " +
                "$n.ShowBalloonTip(8000); Start-Sleep 9; $n.Dispose()";
        try {
            Runtime.getRuntime().exec(new String[]{
                    "powershell", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-Command", script});
            return true;
        } catch (Exception e) { return false; }
    }

    // ── Linux / macOS: AWT SystemTray ────────────────────────────────────

    private static void showAwtTray(String title, String message) {
        if (!SystemTray.isSupported()) {
            System.out.println("  [Toast] " + title + " – " + message);
            return;
        }
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(0x00, 0xD4, 0xFF));
            g.fillOval(0, 0, 15, 15);
            g.dispose();

            TrayIcon icon = new TrayIcon(img, "NetTool");
            icon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(icon);
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            new Thread(() -> {
                try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
                SystemTray.getSystemTray().remove(icon);
            }, "ToastCleanup").start();
        } catch (Exception e) {
            System.out.println("  [Toast] " + title + " – " + message);
        }
    }

    /** PowerShell Single-Quote-Escape. */
    public static String ps(String s) {
        return s == null ? "" : s.replace("'", "''").replace("\n", " ").replace("\r", "");
    }
}
