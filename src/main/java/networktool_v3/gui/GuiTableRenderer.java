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
 * Doppelklick Spalte 0 (IP)       -> Browser / Remote-Simulation
 * Doppelklick Spalte 1 (Hostname) -> Clipboard
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

    // ── Oeffentliche Tabellen-Builder ─────────────────────────────────────

    public void showHostTable(List<HostResult> rows, String title) {
        SwingUtilities.invokeLater(() -> {
            outputPanel.appendText("\n" + title + "\n\n", ACCENT);
            String[]   cols = {"IP", "Hostname", "OS / Geraet"};
            Object[][] data = rows.stream()
                    .sorted(Comparator.comparingInt(r -> ipToInt(r.ip)))
                    .map(r -> new Object[]{r.ip, formatHostname(r.hostname), r.os})
                    .toArray(Object[][]::new);
            embedTable(data, cols, WIDTHS_HOST);
            outputPanel.appendText(rows.size() + " Geraet(e) gefunden.\n", ACCENT2);
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

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(ROW_BG_EVEN);
        sp.getViewport().setBackground(ROW_BG_EVEN);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.setPreferredSize(new Dimension(0, Math.min(preferredHeight(table), 400)));

        JTextPane pane = outputPanel.getOutputPane();
        pane.setCaretPosition(pane.getDocument().getLength());
        pane.insertComponent(sp);
        outputPanel.appendText("\n\n", FG);
    }

    // ── Doppelklick-Handler ───────────────────────────────────────────────

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
                    if (ip.isEmpty() || ip.equals("-")) return;
                    String os = table.getColumnCount() > 2
                            ? String.valueOf(table.getValueAt(row, 2)) : "";
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

    // ── Browser / Remote-Simulation ───────────────────────────────────────

    private static void openInBrowser(String ip, String osFromTable) {
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
            if (isMobileOs(os))       SwingUtilities.invokeLater(() -> showSimulatedPhone(ip, os));
            else if (!isServerOs(os)) SwingUtilities.invokeLater(() -> showSimulatedDesktop(ip, os));
            else                      browseUrl("http://" + ip);
        }, "BrowserOpen-" + ip).start();
    }

    private static boolean isPortOpen(String ip, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(ip, port), 600); return true;
        } catch (Exception e) { return false; }
    }

    private static boolean isMobileOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return l.contains("android")||l.contains("ios")||l.contains("ipad")
                ||l.contains("samsung")||l.contains("xiaomi")||l.contains("huawei")
                ||l.contains("pixel")||l.contains("nothing")||l.contains("oneplus")
                ||l.contains("oppo")||l.contains("realme")||l.contains("motorola")
                ||l.contains("sony")||l.contains("mobil");
    }

    private static boolean isServerOs(String os) {
        if (os == null) return false;
        String l = os.toLowerCase();
        return (l.contains("linux")&&l.contains("server"))
                ||l.contains("datenbankserver")||l.contains("mail-server")
                ||l.contains("dns-server")||l.contains("ftp-server");
    }

    private static String detectOsSafe(String ip) {
        try { return OsDetector.detect(ip); } catch (Exception e) { return ""; }
    }

    private static void browseUrl(String url) {
        try {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) d.browse(new java.net.URI(url));
            else ClipboardUtil.copy(url);
        } catch (Exception e) { ClipboardUtil.copy(url); }
    }

    private static void showSimulatedDesktop(String ip, String os) {
        JDialog dlg = remoteDialog("Remote Desktop  -  " + ip, 480, 200);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(0x10, 0x18, 0x28));
        p.setBorder(new EmptyBorder(16, 20, 12, 20));
        p.add(mkLabel("Remote Desktop Simulation", new Color(0x60,0xA8,0xF0), Font.BOLD, 13));
        p.add(mkLabel("IP: " + ip, new Color(0xA0,0xC0,0xE8), Font.PLAIN, 11));
        p.add(mkLabel("OS: " + (os.isBlank() ? "unbekannt" : os), new Color(0xA0,0xC0,0xE8), Font.PLAIN, 11));
        p.add(Box.createVerticalStrut(10));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(rBtn("RDP kopieren",  new Color(0x60,0xA8,0xF0), () -> ClipboardUtil.copy("mstsc /v:"+ip)));
        btns.add(rBtn("SSH kopieren",  new Color(0xA0,0xC8,0x80), () -> ClipboardUtil.copy("ssh user@"+ip)));
        btns.add(rBtn("HTTP oeffnen",  new Color(0xD0,0xC0,0x60), () -> { browseUrl("http://"+ip); dlg.dispose(); }));
        btns.add(rBtn("Schliessen",    WARN, dlg::dispose));
        p.add(btns);
        dlg.add(p); dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_DESKTOP_SIM", ip);
    }

    private static void showSimulatedPhone(String ip, String os) {
        JDialog dlg = remoteDialog("Remote Geraet  -  " + ip, 380, 180);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(0x10,0x10,0x14));
        p.setBorder(new EmptyBorder(16,20,12,20));
        p.add(mkLabel("Remote Geraet Simulation", new Color(0x78,0xD8,0x78), Font.BOLD, 12));
        p.add(mkLabel("IP:  " + ip, new Color(0xB0,0xD0,0xB0), Font.PLAIN, 11));
        p.add(mkLabel("OS:  " + (os.isBlank() ? "Android/iOS" : os), new Color(0xB0,0xD0,0xB0), Font.PLAIN, 11));
        p.add(Box.createVerticalStrut(10));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        btns.add(rBtn("scrcpy kopieren", new Color(0x78,0xD8,0x78), () -> ClipboardUtil.copy("scrcpy --tcpip="+ip)));
        btns.add(rBtn("ADB kopieren",    new Color(0xA0,0xD0,0xA0), () -> ClipboardUtil.copy("adb connect "+ip+":5555")));
        btns.add(rBtn("Schliessen",      WARN, dlg::dispose));
        p.add(btns);
        dlg.add(p); dlg.setVisible(true);
        AuditLogger.getInstance().log("REMOTE_PHONE_SIM", ip);
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

    private static JDialog remoteDialog(String title, int w, int h) {
        JDialog d = new JDialog((Frame)null, title, false);
        d.setSize(w, h); d.setLocationRelativeTo(null);
        d.getContentPane().setBackground(new Color(0x10,0x10,0x14));
        return d;
    }

    private static JLabel mkLabel(String text, Color col, int style, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", style, size));
        l.setForeground(col);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2,0,2,0));
        return l;
    }

    private static JButton rBtn(String text, Color fg, Runnable action) {
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

    private static String formatHostname(String hostname) {
        if (hostname == null) return "";
        int i = hostname.indexOf(" [");
        return i < 0 ? hostname : hostname.substring(0, i) + "  " + hostname.substring(i);
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