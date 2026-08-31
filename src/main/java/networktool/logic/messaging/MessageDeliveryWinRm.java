package main.java.networktool.logic.messaging;

import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.util.PlatformUtils;

/** Überträgt Nachrichten per WinRM/PowerShell-Remoting an Windows-Ziele. Package-private. */
final class MessageDeliveryWinRm {

    private MessageDeliveryWinRm() {}

    static boolean tryWinRM(String ip, String message) {
        // Ziel-IP wird direkt in ein PowerShell-Skript eingebettet → Pflichtvalidierung
        if (!PlatformUtils.isSafeIp(ip)) {
            System.out.println("  ✕ WinRM: ungültige Ziel-IP");
            return false;
        }
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
            String err = MessageDelivery.readStream(p.getErrorStream());
            p.waitFor();
            if (p.exitValue() == 0) { System.out.println("  ✔ WinRM: BalloonTip gesendet."); return true; }
            System.out.println("  ✕ WinRM: " + (err.isBlank() ? "fehlgeschlagen"
                    : err.lines().findFirst().orElse("").trim()));
        } catch (Exception e) { System.out.println("  ✕ PowerShell: " + e.getMessage()); }
        return false;
    }
}