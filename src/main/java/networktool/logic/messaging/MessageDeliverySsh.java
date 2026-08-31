package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.util.PlatformUtils;

/** Überträgt Nachrichten per SSH (notify-send/osascript) an Linux/macOS-Ziele. Package-private. */
final class MessageDeliverySsh {

    private MessageDeliverySsh() {}

    static boolean trySsh(String ip, String message, boolean mac) {
        // Ziel-IP wird als ssh-Argument verwendet → Pflichtvalidierung
        if (!PlatformUtils.isSafeIp(ip)) {
            System.out.println("  ✕ SSH: ungültige Ziel-IP");
            return false;
        }
        if (!OsDetector.isOpen(ip, 22)) {
            System.out.println("  SSH (22) nicht offen.");
            return false;
        }
        System.out.println("  Methode : SSH → " + (mac ? "osascript" : "notify-send"));
        String safe = PlatformUtils.escapeSshArg(message);
        String cmd  = mac
                ? "osascript -e 'display notification \"" + safe + "\" with title \"NetTool\"'"
                : "DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/$(id -u)/bus "
                + "notify-send 'NetTool' '" + safe + "'";
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    "ssh", "-o", "ConnectTimeout=3", "-o", "StrictHostKeyChecking=no",
                    "-o", "BatchMode=yes", ip, cmd});
            String err = MessageDelivery.readStream(p.getErrorStream());
            p.waitFor();
            if (p.exitValue() == 0) { System.out.println("  ✔ SSH: gesendet."); return true; }
            System.out.println(err.contains("publickey")
                    ? "  ✕ SSH-Key fehlt → ssh-copy-id user@" + ip
                    : "  ✕ SSH: " + err.lines().findFirst().orElse("").trim());
        } catch (Exception e) { System.out.println("  ✕ SSH: " + e.getMessage()); }
        return false;
    }
}