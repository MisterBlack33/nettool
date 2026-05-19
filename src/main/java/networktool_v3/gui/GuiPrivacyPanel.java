package main.java.networktool_v3.gui;

import main.java.networktool_v3.util.PlatformUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Privatsphäre & Tarnung Panel (Menü-ID "30").
 * Alle exec()-Aufrufe mit Interface/MAC über PlatformUtils abgesichert.
 */
public final class GuiPrivacyPanel {

    private GuiPrivacyPanel() {}

    public static void show(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embedPanel(output));
    }

    private static void embedPanel(GuiOutputPanel output) {
        output.appendText("\n🔒 Privatsphäre & Tarnung\n\n", ACCENT);

        Color bg    = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF4, 0xF2, 0xEE);
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xE8, 0xE6, 0xE0);

        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(bg);
        outer.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel statusSection = buildSection("Status", panBg);
        JLabel macLabel = statusLabel("MAC: ...", FG_DIM);
        JLabel ipLabel  = statusLabel("IP:  ...", FG_DIM);
        JLabel vpnLabel = statusLabel("VPN: prüfe...", FG_DIM);
        statusSection.add(macLabel);
        statusSection.add(ipLabel);
        statusSection.add(vpnLabel);
        outer.add(statusSection, BorderLayout.NORTH);

        JPanel actSection = buildSection("Aktionen", panBg);
        JButton macRandBtn  = actionBtn("🎲 MAC randomisieren",    ACCENT);
        JButton macResetBtn = actionBtn("↩ MAC zurücksetzen",      FG_DIM);
        JButton vpnStartBtn = actionBtn("▶ WireGuard starten",     ACCENT2);
        JButton vpnStopBtn  = actionBtn("■ WireGuard stoppen",     WARN);
        JButton checkEncBtn = actionBtn("🔍 Verschlüsselung prüfen", INFO);
        JTextArea logArea   = buildLogArea(bg);

        actSection.add(wrapRow(macRandBtn, macResetBtn));
        actSection.add(wrapRow(vpnStartBtn, vpnStopBtn));
        actSection.add(checkEncBtn);
        actSection.add(new JScrollPane(logArea) {{
            setBorder(new LineBorder(BORDER, 1));
            setPreferredSize(new Dimension(0, 120));
            getViewport().setBackground(bg);
        }});

        JLabel info = new JLabel(
                "<html><small>⚠ MAC-Randomisierung und VPN-Kontrolle können Root-Rechte benötigen.</small></html>");
        info.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        info.setForeground(FG_DIM);
        info.setBorder(new EmptyBorder(4, 8, 4, 8));
        actSection.add(info);
        outer.add(actSection, BorderLayout.CENTER);

        macRandBtn.addActionListener(e ->
                runTask(logArea, "MAC randomisieren", GuiPrivacyPanel::randomizeMac));
        macResetBtn.addActionListener(e ->
                runTask(logArea, "MAC zurücksetzen",  GuiPrivacyPanel::resetMac));
        vpnStartBtn.addActionListener(e ->
                runTask(logArea, "WireGuard starten", GuiPrivacyPanel::startVpn));
        vpnStopBtn.addActionListener(e ->
                runTask(logArea, "WireGuard stoppen", GuiPrivacyPanel::stopVpn));
        checkEncBtn.addActionListener(e ->
                runTask(logArea, "Verschlüsselung prüfen", GuiPrivacyPanel::checkEncryption));

        new Thread(() -> {
            String mac = getCurrentMac();
            String ip  = getCurrentIp();
            String vpn = isVpnActive() ? "✔ aktiv" : "✕ inaktiv";
            SwingUtilities.invokeLater(() -> {
                macLabel.setText("MAC: " + mac); macLabel.setForeground(FG);
                ipLabel.setText("IP:  " + ip);   ipLabel.setForeground(FG);
                vpnLabel.setText("VPN: " + vpn);
                vpnLabel.setForeground(isVpnActive() ? ACCENT2 : FG_DIM);
            });
        }).start();

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(outer);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    // ── Aktionen ──────────────────────────────────────────────────────────

    private static void randomizeMac(JTextArea log) {
        if (PlatformUtils.isWindows()) {
            log("Hinweis: MAC-Spoofing unter Windows via Geräte-Manager.", log); return;
        }
        String iface = getActiveInterface();
        if (iface == null) { log("Kein aktives Interface gefunden.", log); return; }
        // Validierung vor exec()
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

    private static void resetMac(JTextArea log) {
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

    private static void startVpn(JTextArea log) {
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

    private static void stopVpn(JTextArea log) {
        try {
            exec(PlatformUtils.isWindows()
                    ? new String[]{"wireguard", "/uninstalltunnelservice", "wg0"}
                    : new String[]{"wg-quick", "down", "wg0"}, log);
            log("✔ WireGuard gestoppt.", log);
        } catch (Exception e) { log("Fehler: " + e.getMessage(), log); }
    }

    private static void checkEncryption(JTextArea log) {
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

    private static void runTask(JTextArea log, String name, java.util.function.Consumer<JTextArea> task) {
        log.setText("");
        log("── " + name + " ──", log);
        new Thread(() -> task.accept(log), "Privacy-" + name).start();
    }

    private static String randomMac() {
        Random r = new Random();
        return String.format("02:%02X:%02X:%02X:%02X:%02X",
                r.nextInt(256), r.nextInt(256), r.nextInt(256), r.nextInt(256), r.nextInt(256));
    }

    private static String getCurrentMac() {
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

    private static String getCurrentIp() {
        try { return java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "unbekannt"; }
    }

    private static boolean isVpnActive() {
        try {
            Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                String name = ifaces.nextElement().getName().toLowerCase();
                if (name.startsWith("wg")||name.startsWith("tun")||name.startsWith("tap")) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String getActiveInterface() {
        try {
            Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface ni = ifaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) return ni.getName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── UI-Hilfsmethoden ─────────────────────────────────────────────────

    private static JPanel buildSection(String title, Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        p.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(BORDER, 1), "  " + title + "  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("JetBrains Mono", Font.BOLD, 11), ACCENT),
                new EmptyBorder(8, 12, 12, 12)));
        return p;
    }

    private static JLabel statusLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    private static JButton actionBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(5, 12, 5, 12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited (MouseEvent e) { b.setBackground(BTN_BG); }
        });
        return b;
    }

    private static JTextArea buildLogArea(Color bg) {
        JTextArea a = new JTextArea();
        a.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        a.setForeground(new Color(0xA0, 0xE0, 0xA0));
        a.setBackground(GuiTheme.isDark() ? new Color(0x06,0x09,0x06) : new Color(0xF0,0xF8,0xF0));
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(6, 8, 6, 8));
        return a;
    }

    private static JPanel wrapRow(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton b : btns) p.add(b);
        return p;
    }
}