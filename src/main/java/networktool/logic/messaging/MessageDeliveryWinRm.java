package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.util.PlatformSupport;

/** Überträgt Nachrichten per WinRM/PowerShell-Remoting an Windows-Ziele. Package-private. */
final class MessageDeliveryWinRm {

    private MessageDeliveryWinRm() {}

    static boolean tryWinRM(String ip, String message) {
        // Ziel-IP wird direkt in ein PowerShell-Skript eingebettet → Pflichtvalidierung
        if (!PlatformSupport.isSafeIp(ip)) {
            DebugLogger.getInstance().warn("[MessageDeliveryWinRm] Invalid destination");
            return false;
        }
        if (!OsDetector.isOpen(ip, 5985)) {
            DebugLogger.getInstance().info("[MessageDeliveryWinRm] WinRM port is closed");
            return false;
        }
        DebugLogger.getInstance().info("[MessageDeliveryWinRm] WinRM delivery started");
        String m = PlatformSupport.escapePowerShell(message);
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
            String err = MessageDelivery.readStream(p.getErrorStream());
            p.waitFor();
            if (p.exitValue() == 0) {
                DebugLogger.getInstance().info("[MessageDeliveryWinRm] WinRM delivery succeeded");
                return true;
            }
            DebugLogger.getInstance().warn("[MessageDeliveryWinRm] WinRM delivery failed");
        } catch (Exception e) {
            DebugLogger.getInstance().warn("[MessageDeliveryWinRm] WinRM delivery failed");
        }
        return false;
    }
}
