package main.java.networktool_v3.gui;

import main.java.networktool_v3.filter.ClipboardUtil;
import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

import static main.java.networktool_v3.gui.GuiTheme.*;

/** Remote-Simulation-Dialoge und Browser-Öffnung. Paket-privat. */
final class GuiRemoteActions {

    private GuiRemoteActions() {}

    static void openInBrowser(String ip, String osFromTable) {
        new Thread(() -> {
            AuditLogger.getInstance().log("BROWSER_OPEN", ip);
            String os = (osFromTable != null && !osFromTable.isBlank())
                    ? osFromTable : detectOsSafe(ip);

            boolean p80   = isPortOpen(ip, 80);
            boolean p443  = isPortOpen(ip, 443);
            boolean p8080 = isPortOpen(ip, 8080);
            boolean p8443 = isPortOpen(ip, 8443);

            if (p80 || p443 || p8080 || p8443) {
                String proto = (p443 || p8443) ? "https" : "http";
                int    port  = p443 ? 443 : p8443 ? 8443 : p8080 ? 8080 : 80;
                String url   = (port == 80 || port == 443)
                        ? proto + "://" + ip : proto + "://" + ip + ":" + port;
                browseUrl(url);
                return;
            }
            if (isMobileOs(os))       SwingUtilities.invokeLater(() -> showPhone(ip, os));
            else if (!isServerOs(os)) SwingUtilities.invokeLater(() -> showDesktop(ip, os));
            else                      browseUrl("http://" + ip);
        }, "BrowserOpen-" + ip).start();
    }

    private static void showDesktop(String ip, String os) {
        JDialog dlg = dialog("Remote Desktop  -  " + ip, 480, 200);
        JPanel p = panel(new Color(0x10, 0x18, 0x28));
        p.add(lbl("Remote Desktop Simulation", new Color(0x60,0xA8,0xF0), Font.BOLD, 13));
        p.add(lbl("IP: " + ip,  new Color(0xA0,0xC0,0xE8), Font.PLAIN, 11));
        p.add(lbl("OS: " + (os.isBlank() ? "unbekannt" : os), new Color(0xA0,0xC0,0xE8), Font.PLAIN, 11));
        p.add(Box.createVerticalStrut(10));
        JPanel btns = row();
        btns.add(btn("RDP kopieren",  new Color(0x60,0xA8,0xF0), () -> ClipboardUtil.copy("mstsc /v:"+ip)));
        btns.add(btn("SSH kopieren",  new Color(0xA0,0xC8,0x80), () -> ClipboardUtil.copy("ssh user@"+ip)));
        btns.add(btn("HTTP öffnen",   new Color(0xD0,0xC0,0x60), () -> { browseUrl("http://"+ip); dlg.dispose(); }));
        btns.add(btn("Schließen",     WARN, dlg::dispose));
        p.add(btns);
        dlg.add(p); dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_DESKTOP_SIM", ip);
    }

    private static void showPhone(String ip, String os) {
        JDialog dlg = dialog("Remote Gerät  -  " + ip, 380, 180);
        JPanel p = panel(new Color(0x10,0x10,0x14));
        p.add(lbl("Remote Gerät Simulation", new Color(0x78,0xD8,0x78), Font.BOLD, 12));
        p.add(lbl("IP: " + ip, new Color(0xB0,0xD0,0xB0), Font.PLAIN, 11));
        p.add(lbl("OS: " + (os.isBlank() ? "Android/iOS" : os), new Color(0xB0,0xD0,0xB0), Font.PLAIN, 11));
        p.add(Box.createVerticalStrut(10));
        JPanel btns = row();
        btns.add(btn("scrcpy kopieren", new Color(0x78,0xD8,0x78), () -> ClipboardUtil.copy("scrcpy --tcpip="+ip)));
        btns.add(btn("ADB kopieren",    new Color(0xA0,0xD0,0xA0), () -> ClipboardUtil.copy("adb connect "+ip+":5555")));
        btns.add(btn("Schließen",       WARN, dlg::dispose));
        p.add(btns);
        dlg.add(p); dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_PHONE_SIM", ip);
    }

    static boolean isPortOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), 600);
            return true;
        } catch (Exception e) { return false; }
    }

    static boolean isMobileOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return l.contains("android")||l.contains("ios")||l.contains("ipad")
                ||l.contains("samsung")||l.contains("xiaomi")||l.contains("huawei")
                ||l.contains("pixel")||l.contains("nothing")||l.contains("oneplus")
                ||l.contains("oppo")||l.contains("realme")||l.contains("motorola")
                ||l.contains("sony")||l.contains("mobil");
    }

    static boolean isServerOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return (l.contains("linux")&&l.contains("server"))
                ||l.contains("datenbankserver")||l.contains("mail-server")
                ||l.contains("dns-server")||l.contains("ftp-server");
    }

    private static String detectOsSafe(String ip) {
        try { return OsDetector.detect(ip); } catch (Exception e) { return ""; }
    }

    static void browseUrl(String url) {
        try {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) d.browse(new java.net.URI(url));
            else ClipboardUtil.copy(url);
        } catch (Exception e) { ClipboardUtil.copy(url); }
    }

    private static JDialog dialog(String title, int w, int h) {
        JDialog d = new JDialog((Frame)null, title, false);
        d.setSize(w, h); d.setLocationRelativeTo(null);
        d.getContentPane().setBackground(new Color(0x10,0x10,0x14));
        return d;
    }

    private static JPanel panel(Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(16,20,12,20));
        return p;
    }

    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        return p;
    }

    private static JLabel lbl(String text, Color col, int style, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", style, size));
        l.setForeground(col);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2,0,2,0));
        return l;
    }

    private static JButton btn(String text, Color fg, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(new Color(0x1C,0x22,0x1C));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(),1), new EmptyBorder(3,8,3,8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }
}