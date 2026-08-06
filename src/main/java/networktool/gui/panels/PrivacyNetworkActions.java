package main.java.networktool.gui.panels;

import main.java.networktool.util.PlatformUtils;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System-nahe Aktionen für das Privatsphäre-Panel: MAC-Randomisierung,
 * WireGuard-Steuerung und einfacher Verschlüsselungs-Check.
 * Alle exec()-Aufrufe mit Interface/MAC über {@link PlatformUtils} abgesichert.
 */
final class PrivacyNetworkActions {

    private static final Logger LOG = Logger.getLogger(PrivacyNetworkActions.class.getName());

    private PrivacyNetworkActions() {}

    static void runTask(JTextArea log, String name, java.util.function.Consumer<JTextArea> task) {
        log.setText("");
        log("── " + name + " ──", log);
        new Thread(() -> task.accept(log), "Privacy-" + name).start();
    }

    static void randomizeMac(JTextArea log) {
        if (PlatformUtils.isWindows()) {
            log("Hinweis: MAC-Spoofing unter Windows via Geräte-Manager.", log); return;
        }
        String iface = getActiveInterface();
        if (iface == null) { log("Kein aktives Interface gefunden.", log); return; }
        if (!PlatformUtils.isSafeInterface(iface)) { log("Ungültiger Interface-Name: " + iface, log); return; }
        String newMac = randomMac();
        log("Interface: " + iface, log);
        log("Neue MAC:  " + newMac, log);
        try {
            exec(new String[]{"ip", "link", "set", iface, "down"}, log);
            exec(new String[]{"ip", "link", "set", iface, "address", newMac}, log);
            exec(new String[]{"ip", "link", "set", iface, "up"}, log);
            log("✔ MAC gesetzt: " + newMac, log);
        } catch (Exception e) { log("Fehler: " + e.getMessage(), log); }
    }

    static void resetMac(JTextArea log) {
        if (PlatformUtils.isWindows()) { log("Windows: MAC über Geräte-Manager zurücksetzen.", log); return; }
        String iface = getActiveInterface();
        if (iface == null) { log("Kein aktives Interface.", log); return; }
        if (!PlatformUtils.isSafeInterface(iface)) { log("Ungültiger Interface-Name: " + iface, log); return; }
        try {
            exec(new String[]{"ip", "link", "set", iface, "down"}, log);
            exec(new String[]{"ethtool", "-E", iface}, log);
            exec(new String[]{"ip", "link", "set", iface, "up"}, log);
            log("✔ MAC zurückgesetzt.", log);
        } catch (Exception e) { log("Fehler (ethtool nötig): " + e.getMessage(), log); }
    }

    static void startVpn(JTextArea log) {
        try {
            File wgDir = PlatformUtils.isWindows()
                    ? new File("C:\\Program Files\\WireGuard")
                    : new File("/etc/wireguard");
            if (!wgDir.exists()) {
                log("✕ WireGuard nicht gefunden. Installation: wireguard.com/install/", log); return;
            }
            String[] configs = wgDir.list((d, n) -> n.endsWith(".conf"));
            if (configs == null || configs.length == 0) {
                log("✕ Keine .conf gefunden in " + wgDir, log); return;
            }
            String conf = configs[0].replace(".conf", "");
            log("Konfiguration: " + conf, log);
            exec(PlatformUtils.isWindows()
                    ? new String[]{"wireguard", "/installtunnelservice", wgDir + "\\" + conf + ".conf"}
                    : new String[]{"wg-quick", "up", conf}, log);
            log("✔ WireGuard gestartet.", log);
        } catch (Exception e) { log("Fehler: " + e.getMessage(), log); }
    }

    static void stopVpn(JTextArea log) {
        try {
            exec(PlatformUtils.isWindows()
                    ? new String[]{"wireguard", "/uninstalltunnelservice", "wg0"}
                    : new String[]{"wg-quick", "down", "wg0"}, log);
            log("✔ WireGuard gestoppt.", log);
        } catch (Exception e) { log("Fehler: " + e.getMessage(), log); }
    }

    static void checkEncryption(JTextArea log) {
        log("── Verschlüsselungs-Check ──", log);
        log("VPN (WireGuard): " + (isVpnActive() ? "✔ aktiv" : "✕ inaktiv"), log);
        try {
            String ip = getCurrentIp();
            log("Lokale IP: " + ip, log);
            if (ip.startsWith("10.")||ip.startsWith("172.")||ip.startsWith("192.168."))
                log("DNS-Leak-Risiko: Lokale IP sichtbar → VPN empfohlen.", log);
        } catch (Exception e) { log("Fehler: " + e.getMessage(), log); }
        log("Empfehlung: dnsleak.com / ipleak.net im Browser prüfen.", log);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static void exec(String[] cmd, JTextArea log) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmd);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
             BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) log(line, log);
            while ((line = er.readLine()) != null) log("[err] " + line, log);
        }
        p.waitFor();
    }

    private static void log(String msg, JTextArea log) {
        SwingUtilities.invokeLater(() -> log.append(msg + "\n"));
    }

    private static String randomMac() {
        Random r = new Random();
        return String.format("02:%02X:%02X:%02X:%02X:%02X",
                r.nextInt(256), r.nextInt(256), r.nextInt(256), r.nextInt(256), r.nextInt(256));
    }

    static String getCurrentMac() {
        try {
            String iface = getActiveInterface();
            if (iface == null) return "unbekannt";
            java.net.NetworkInterface ni = java.net.NetworkInterface.getByName(iface);
            if (ni == null) return "unbekannt";
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return "unbekannt";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", mac[i]));
            }
            return sb.toString();
        } catch (Exception e) { return "unbekannt"; }
    }

    static String getCurrentIp() {
        try { return java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "unbekannt"; }
    }

    static boolean isVpnActive() {
        try {
            Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                String name = ifaces.nextElement().getName().toLowerCase();
                if (name.startsWith("wg")||name.startsWith("tun")||name.startsWith("tap")) return true;
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "VPN-Interfaces konnten nicht ermittelt werden", e);
        }
        return false;
    }

    private static String getActiveInterface() {
        try {
            Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface ni = ifaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) return ni.getName();
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Aktives Netzwerk-Interface konnte nicht ermittelt werden", e);
        }
        return null;
    }
}
