package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.util.PlatformSupport;

/** Überträgt Nachrichten per SSH (notify-send/osascript) an Linux/macOS-Ziele. Package-private. */
final class MessageDeliverySsh {

    private MessageDeliverySsh() {}

    static boolean trySsh(String ip, String message, boolean mac) {
        // Ziel-IP wird als ssh-Argument verwendet → Pflichtvalidierung
        if (!PlatformSupport.isSafeIp(ip)) {
            DebugLogger.getInstance().warn("[MessageDeliverySsh] Invalid destination");
            return false;
        }
        if (!OsDetector.isOpen(ip, 22)) {
            DebugLogger.getInstance().info("[MessageDeliverySsh] SSH port is closed");
            return false;
        }
        DebugLogger.getInstance().info("[MessageDeliverySsh] SSH delivery started");
        String safe = PlatformSupport.escapeSshArg(message);
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
            if (p.exitValue() == 0) {
                DebugLogger.getInstance().info("[MessageDeliverySsh] SSH delivery succeeded");
                return true;
            }
            DebugLogger.getInstance().warn("[MessageDeliverySsh] SSH delivery failed"
                    + (err.contains("publickey") ? " (key unavailable)" : ""));
        } catch (Exception e) {
            DebugLogger.getInstance().warn("[MessageDeliverySsh] SSH delivery failed");
        }
        return false;
    }
}
