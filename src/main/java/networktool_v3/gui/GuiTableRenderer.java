package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.filter.ClipboardUtil;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.security.AuditLogger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Comparator;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;
import static main.java.networktool_v3.gui.TableConfig.*;

/**
 * Rendert Host- und Scan-Ergebnisse als JTable im Output-Panel.
 *
 * Doppelklick auf Spalte 0 (IP):
 *  → Prüft ob Webserver erreichbar (Port 80/443/8080/8443)
 *  → Wenn ja: Browser öffnen
 *  → Wenn nein + kein Server + PC/Laptop → Remote-Desktop-Simulation
 *  → Wenn nein + kein Server + Handy     → Remote-Handy-Simulation
 *
 * Doppelklick auf Spalte 1 (Hostname):
 *  → Hostname in Zwischenablage
 */
public class GuiTableRenderer {

    private final GuiOutputPanel outputPanel;
    private GuiContextMenu       contextMenu;

    public GuiTableRenderer(GuiOutputPanel outputPanel) {
        this.outputPanel = outputPanel;
    }

    public void setContextMenu(GuiContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    // ── Öffentliche Tabellen-Builder ──────────────────────────────────────

    public void showHostTable(List<HostResult> rows, String title) {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendText("\n" + title + "\n\n", ACCENT);
            String[]   cols = {"IP", "Hostname", "OS / Gerät"};
            Object[][] data = rows.stream()
                    .sorted(Comparator.comparingInt(r -> ipToInt(r.ip)))
                    .map(r -> new Object[]{r.ip, formatHostname(r.hostname), r.os})
                    .toArray(Object[][]::new);
            embedTable(data, cols, WIDTHS_HOST);
            outputPanel.appendText(rows.size() + " Gerät(e) gefunden.\n", ACCENT2);
        });
    }

    public void showScanTable(List<ScanResult> rows) {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendText("\n=== Scan-Ergebnisse ===\n\n", ACCENT);
            String[]   cols = {"IP", "Hostname", "OS", "Offene Ports"};
            Object[][] data = rows.stream()
                    .map(r -> new Object[]{
                            r.getIp(), r.getHostname(),
                            r.getOsGuess(), r.getOpenPorts().keySet().toString()})
                    .toArray(Object[][]::new);
            embedTable(data, cols, WIDTHS_SCAN);
        });
    }

    // ── Tabelle einbetten ─────────────────────────────────────────────────

    private void embedTable(Object[][] data, String[] cols, int[] widths) {
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = TableConfig.buildTable(model, widths);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(0, Comparator.comparingInt(GuiTableRenderer::ipToInt));
        sorter.setComparator(2, Comparator.nullsLast(String::compareToIgnoreCase));
        table.setRowSorter(sorter);

        if (contextMenu != null) contextMenu.attach(table);
        installDoubleClickCopy(table);

        int totalH = preferredHeight(table);

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN);
        sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, Math.min(totalH, 400)));

        JTextPane pane = outputPanel.getOutputPane();
        pane.setCaretPosition(pane.getDocument().getLength());
        pane.insertComponent(sp);
        outputPanel.appendText("\n\n", FG);
    }

    // ── Doppelklick-Handler ───────────────────────────────────────────────

    /**
     * Doppelklick auf Spalte 0 (IP) → Browser oder Remote-Simulation
     * Doppelklick auf Spalte 1 (Hostname) → In Zwischenablage
     */
    static void installDoubleClickCopy(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2) return;
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || (col != 0 && col != 1)) return;

                Object val = table.getValueAt(row, col);
                if (val == null) return;

                if (col == 0) {
                    String ip = val.toString().trim();
                    if (ip.isEmpty() || ip.equals("–")) return;
                    // OS aus Spalte 2 holen (für Gerättyp-Erkennung)
                    String os = "";
                    if (table.getColumnCount() > 2) {
                        Object osVal = table.getValueAt(row, 2);
                        if (osVal != null) os = osVal.toString();
                    }
                    openInBrowser(ip, os);
                } else {
                    String text = val.toString();
                    int bracket = text.indexOf(" [");
                    if (bracket > 0) text = text.substring(0, bracket).trim();
                    ClipboardUtil.copy(text);
                }
            }
        });
    }

    /**
     * Öffnet eine IP im Browser.
     *
     * Logik:
     *  1. Prüfe ob Port 80 / 443 / 8080 / 8443 offen → Browser öffnen
     *  2. Kein Webserver erreichbar:
     *     a) PC/Laptop (Windows/Linux/macOS) → Remote-Desktop-Simulation
     *     b) Mobiles Gerät (Android/iOS)     → Remote-Handy-Simulation
     *     c) Unbekannt                       → trotzdem Browser versuchen
     *
     * Läuft in eigenem Thread damit die UI nicht blockiert.
     */
    private static void openInBrowser(String ip, String osFromTable) {
        new Thread(() -> {
            AuditLogger.getInstance().log("BROWSER_OPEN", ip);

            // OS aus der Tabelle verwenden, falls vorhanden; sonst frisch ermitteln
            String os = (osFromTable != null && !osFromTable.isBlank())
                    ? osFromTable : detectOsSafe(ip);

            boolean port80   = isPortOpen(ip, 80);
            boolean port443  = isPortOpen(ip, 443);
            boolean port8080 = isPortOpen(ip, 8080);
            boolean port8443 = isPortOpen(ip, 8443);
            boolean hasWeb   = port80 || port443 || port8080 || port8443;

            if (hasWeb) {
                // Webserver vorhanden → Browser
                String proto = (port443 || port8443) ? "https" : "http";
                int    port  = port443 ? 443 : port8443 ? 8443 : port8080 ? 8080 : 80;
                String url   = (port == 80 || port == 443)
                        ? proto + "://" + ip
                        : proto + "://" + ip + ":" + port;
                browseUrl(url);
                System.out.println("  ↗ Browser: " + url);
                return;
            }

            // Kein Webserver – Gerättyp bestimmen
            boolean isMobile = isMobileOs(os);
            boolean isServer = isServerOs(os); // z.B. Linux-Server ohne HTTP

            if (isMobile) {
                // Handy → Remote-Handy-Simulation
                SwingUtilities.invokeLater(() -> showSimulatedPhone(ip, os));
            } else if (!isServer) {
                // PC / Laptop (Windows, macOS, Linux-Desktop) → Remote-Desktop
                SwingUtilities.invokeLater(() -> showSimulatedDesktop(ip, os));
            } else {
                // Server ohne HTTP → Browser trotzdem versuchen (timeout okay)
                browseUrl("http://" + ip);
                System.out.println("  ↗ Browser (kein Web-Port, Server): http://" + ip);
            }
        }, "BrowserOpen-" + ip).start();
    }

    // ── Port-Prüfung ─────────────────────────────────────────────────────

    private static boolean isPortOpen(String ip, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(ip, port), 600);
            return true;
        } catch (Exception e) { return false; }
    }

    // ── OS-Klassifikation ─────────────────────────────────────────────────

    private static boolean isMobileOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return l.contains("android") || l.contains("ios")  || l.contains("ipad")
                || l.contains("mobil")   || l.contains("samsung") || l.contains("xiaomi")
                || l.contains("huawei")  || l.contains("pixel")   || l.contains("nothing")
                || l.contains("oneplus") || l.contains("oppo")    || l.contains("realme")
                || l.contains("motorola")|| l.contains("sony");
    }

    private static boolean isServerOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return (l.contains("linux") && l.contains("server"))
                || l.contains("datenbankserver") || l.contains("mail-server")
                || l.contains("dns-server")      || l.contains("ftp-server")
                || l.contains("linux/unix + server");
    }

    private static String detectOsSafe(String ip) {
        try { return OsDetector.detect(ip); }
        catch (Exception e) { return ""; }
    }

    // ── Browser öffnen ────────────────────────────────────────────────────

    private static void browseUrl(String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE))
                desktop.browse(new java.net.URI(url));
            else { ClipboardUtil.copy(url); System.out.println("  URL kopiert: " + url); }
        } catch (Exception e) {
            ClipboardUtil.copy(url);
        }
    }

    // ── Simuliertes Remote-Desktop ────────────────────────────────────────

    private static void showSimulatedDesktop(String ip, String os) {
        JDialog dlg = createRemoteWindow("Remote Desktop  –  " + ip, 760, 520);

        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(new Color(0x1A, 0x1A, 0x2E));

        // Taskbar
        JPanel taskbar = new JPanel(new BorderLayout());
        taskbar.setBackground(new Color(0x0A, 0x0A, 0x18));
        taskbar.setPreferredSize(new Dimension(0, 36));
        taskbar.setBorder(new EmptyBorder(0, 10, 0, 10));
        JLabel startBtn = new JLabel("⊞  Start");
        startBtn.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        startBtn.setForeground(new Color(0xA0, 0xC8, 0xFF));
        taskbar.add(startBtn, BorderLayout.WEST);
        JLabel clock = new JLabel(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
        clock.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        clock.setForeground(new Color(0xC0, 0xD0, 0xE8));
        taskbar.add(clock, BorderLayout.EAST);

        // Desktop-Icons
        JPanel desktop = new JPanel(null);
        desktop.setBackground(new Color(0x1A, 0x1A, 0x2E));
        String[] icons = {"🖥  Computer", "📁  Dokumente", "🌐  Browser", "⚙  Einstellungen"};
        for (int i = 0; i < icons.length; i++) {
            JLabel ico = new JLabel("<html><center>" + icons[i] + "</center></html>",
                    SwingConstants.CENTER);
            ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
            ico.setForeground(new Color(0xD8, 0xE8, 0xFF));
            ico.setBounds(20 + (i % 2) * 90, 20 + (i / 2) * 80, 80, 60);
            desktop.add(ico);
        }

        // Info-Panel
        JPanel info = new JPanel();
        info.setBackground(new Color(0x10, 0x18, 0x28));
        info.setBorder(new EmptyBorder(10, 16, 10, 16));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        addInfoLine(info, "🖥  Remote Desktop Simulation",
                new Color(0x60, 0xA8, 0xF0), Font.BOLD, 13);
        addInfoLine(info, "Ziel-IP: " + ip,
                new Color(0xA0, 0xC0, 0xE8), Font.PLAIN, 11);
        addInfoLine(info, "OS:      " + (os.isBlank() ? "unbekannt" : os),
                new Color(0xA0, 0xC0, 0xE8), Font.PLAIN, 11);
        addInfoLine(info, " ", Color.GRAY, Font.PLAIN, 6);
        addInfoLine(info, "Kein Webserver – Verbindungsoptionen:",
                new Color(0xE8, 0xD0, 0x80), Font.BOLD, 11);
        addInfoLine(info, "• RDP (Port 3389) – Windows Remote Desktop",
                new Color(0xC8, 0xD8, 0xF0), Font.PLAIN, 11);
        addInfoLine(info, "• VNC (Port 5900) – Plattformübergreifend",
                new Color(0xC8, 0xD8, 0xF0), Font.PLAIN, 11);
        addInfoLine(info, "• SSH (Port 22)   – Terminal-Zugriff",
                new Color(0xC8, 0xD8, 0xF0), Font.PLAIN, 11);
        addInfoLine(info, "• AnyDesk / TeamViewer",
                new Color(0xC8, 0xD8, 0xF0), Font.PLAIN, 11);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(new Color(0x10, 0x18, 0x28));
        btnRow.add(remoteBtn("📋  RDP-Befehl kopieren",
                new Color(0x60, 0xA8, 0xF0), () -> ClipboardUtil.copy("mstsc /v:" + ip)));
        btnRow.add(remoteBtn("📋  VNC kopieren",
                new Color(0x80, 0xD0, 0x80), () -> ClipboardUtil.copy("vnc://" + ip)));
        btnRow.add(remoteBtn("📋  SSH kopieren",
                new Color(0xA0, 0xC8, 0x80), () -> ClipboardUtil.copy("ssh user@" + ip)));
        btnRow.add(remoteBtn("🌐  HTTP trotzdem öffnen",
                new Color(0xD0, 0xC0, 0x60), () -> { browseUrl("http://" + ip); dlg.dispose(); }));
        btnRow.add(remoteBtn("✕  Schließen", new Color(0xFF, 0x60, 0x50), dlg::dispose));
        info.add(btnRow);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, desktop, info);
        split.setDividerLocation(200);
        split.setDividerSize(3);
        split.setBorder(null);

        screen.add(split, BorderLayout.CENTER);
        screen.add(taskbar, BorderLayout.SOUTH);
        dlg.add(screen);
        dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_DESKTOP_SIM", ip);
    }

    // ── Simuliertes Remote-Handy ──────────────────────────────────────────

    private static void showSimulatedPhone(String ip, String os) {
        JDialog dlg = createRemoteWindow("Remote Gerät  –  " + ip, 360, 600);

        JPanel phone = new JPanel(new BorderLayout());
        phone.setBackground(new Color(0x10, 0x10, 0x14));
        phone.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(new Color(0x18, 0x1A, 0x20));
        frame.setBorder(new CompoundBorder(
                new LineBorder(new Color(0x38, 0x40, 0x50), 3),
                new EmptyBorder(2, 2, 2, 2)));

        // Statusleiste
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0x0C, 0x0E, 0x12));
        statusBar.setPreferredSize(new Dimension(0, 24));
        statusBar.setBorder(new EmptyBorder(2, 10, 2, 10));
        JLabel timeL = new JLabel(
                new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
        timeL.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        timeL.setForeground(new Color(0xE0, 0xE0, 0xE8));
        JLabel sigL = new JLabel("▲▲▲  WiFi  🔋");
        sigL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 9));
        sigL.setForeground(new Color(0xC0, 0xD0, 0xC0));
        statusBar.add(timeL, BorderLayout.WEST);
        statusBar.add(sigL,  BorderLayout.EAST);

        // App-Grid
        JPanel homeScreen = new JPanel(new GridLayout(4, 4, 8, 8));
        homeScreen.setBackground(new Color(0x18, 0x1A, 0x20));
        homeScreen.setBorder(new EmptyBorder(16, 12, 16, 12));
        String[] apps = {"📷","📞","💬","🌐","📧","🗓","🎵","📷",
                "⚙","📍","📱","🔒","🛒","📸","🎮","📰"};
        for (String a : apps) {
            JLabel app = new JLabel(a, SwingConstants.CENTER);
            app.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            app.setOpaque(true);
            app.setBackground(new Color(0x22, 0x26, 0x30));
            app.setBorder(new LineBorder(new Color(0x30, 0x38, 0x48), 1));
            homeScreen.add(app);
        }

        // Navigationsleiste
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 6));
        navBar.setBackground(new Color(0x10, 0x12, 0x18));
        navBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0x30, 0x38, 0x48)));
        for (String nav : new String[]{"◁", "○", "□"}) {
            JLabel n = new JLabel(nav);
            n.setFont(new Font("JetBrains Mono", Font.BOLD, 16));
            n.setForeground(new Color(0xC0, 0xD0, 0xE0));
            navBar.add(n);
        }

        frame.add(statusBar,  BorderLayout.NORTH);
        frame.add(homeScreen, BorderLayout.CENTER);
        frame.add(navBar,     BorderLayout.SOUTH);

        // Info-Panel
        JPanel info = new JPanel();
        info.setBackground(new Color(0x10, 0x10, 0x14));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10, 4, 4, 4));
        addInfoLine(info, "📱  Remote Gerät Simulation",
                new Color(0x78, 0xD8, 0x78), Font.BOLD, 12);
        addInfoLine(info, "IP:  " + ip,
                new Color(0xB0, 0xD0, 0xB0), Font.PLAIN, 11);
        addInfoLine(info, "OS:  " + (os.isBlank() ? "Android/iOS" : os),
                new Color(0xB0, 0xD0, 0xB0), Font.PLAIN, 11);
        addInfoLine(info, " ", Color.GRAY, Font.PLAIN, 4);
        addInfoLine(info, "Verbindungsoptionen:",
                new Color(0xE8, 0xD0, 0x80), Font.BOLD, 11);
        addInfoLine(info, "• scrcpy – Bildschirm spiegeln (USB/WLAN)",
                new Color(0xC8, 0xE0, 0xC8), Font.PLAIN, 10);
        addInfoLine(info, "• ADB – Android Debug Bridge",
                new Color(0xC8, 0xE0, 0xC8), Font.PLAIN, 10);
        addInfoLine(info, "• AnyDesk / TeamViewer (App nötig)",
                new Color(0xC8, 0xE0, 0xC8), Font.PLAIN, 10);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnRow.setBackground(new Color(0x10, 0x10, 0x14));
        btnRow.add(remoteBtn("📋  scrcpy kopieren",
                new Color(0x78, 0xD8, 0x78),
                () -> ClipboardUtil.copy("scrcpy --tcpip=" + ip)));
        btnRow.add(remoteBtn("📋  ADB kopieren",
                new Color(0xA0, 0xD0, 0xA0),
                () -> ClipboardUtil.copy("adb connect " + ip + ":5555")));
        btnRow.add(remoteBtn("✕  Schließen",
                new Color(0xFF, 0x60, 0x50), dlg::dispose));
        info.add(btnRow);

        phone.add(frame, BorderLayout.CENTER);
        phone.add(info,  BorderLayout.SOUTH);
        dlg.add(phone);
        dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_PHONE_SIM", ip);
    }

    // ── Hilfsmethoden für Remote-Fenster ──────────────────────────────────

    private static JDialog createRemoteWindow(String title, int w, int h) {
        JDialog dlg = new JDialog((Frame) null, title, false);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);
        dlg.getContentPane().setBackground(new Color(0x10, 0x10, 0x14));
        return dlg;
    }

    private static void addInfoLine(JPanel p, String text, Color col, int style, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", style, size));
        l.setForeground(col);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(1, 0, 1, 0));
        p.add(l);
    }

    private static JButton remoteBtn(String text, Color fg, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(new Color(0x1C, 0x22, 0x1C));
        b.setBorder(new CompoundBorder(
                new LineBorder(fg.darker(), 1),
                new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(0x28, 0x32, 0x28)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(new Color(0x1C, 0x22, 0x1C)); }
        });
        return b;
    }

    // ── Allgemeine Hilfsmethoden ───────────────────────────────────────────

    private static String formatHostname(String hostname) {
        if (hostname == null) return "";
        int macIdx = hostname.indexOf(" [");
        return macIdx < 0 ? hostname
                : hostname.substring(0, macIdx) + "  " + hostname.substring(macIdx);
    }

    static int ipToInt(Object ipObj) {
        if (ipObj == null) return 0;
        String[] parts = ipObj.toString().split("\\.");
        int result = 0;
        for (String p : parts) {
            try { result = (result << 8) | Integer.parseInt(p.trim()); }
            catch (NumberFormatException e) { return 0; }
        }
        return result;
    }
}