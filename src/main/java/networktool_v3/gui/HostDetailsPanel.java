package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.logic.analysis.OuiDatabase;
import main.java.networktool_v3.logic.ports.PortScanner;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Vollständiges Host-Details-Fenster.
 *
 * Abschnitte:
 *  ① Basis-Info      IP, Hostname, OS+Konfidenz, MAC/OUI, gespeichert am
 *  ② Ping-History    Live-Ping mit Mini-Verlaufsbalken (letzten 20 Pings)
 *  ③ Offene Ports    Scan + Banner-Grabbing on-demand
 *  ④ Notizen-Editor  Direkt bearbeiten + speichern
 *
 * Öffnen: GuiContextMenu → "🔍 Details"
 */
public final class HostDetailsPanel {

    private HostDetailsPanel() {}

    public static void show(String ip, String hostname, String os, String category) {
        SwingUtilities.invokeLater(() -> openWindow(ip, hostname, os, category));
    }

    // ── Fenster aufbauen ──────────────────────────────────────────────────

    private static void openWindow(String ip, String hostname, String os, String category) {
        JDialog dlg = new JDialog((Frame) null, "Host Details  –  " + ip, false);
        dlg.setSize(680, 700);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg    = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF8, 0xF6, 0xF2);
        Color panBg = GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xEE, 0xEC, 0xE6);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(bg);

        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC));
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel ipLbl = new JLabel(ip);
        ipLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 18));
        ipLbl.setForeground(ACCENT);
        JLabel catLbl = new JLabel(category != null ? "  [" + category + "]" : "");
        catLbl.setFont(MONO_S);
        catLbl.setForeground(FG_DIM);
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(ipLbl); headerLeft.add(catLbl);
        header.add(headerLeft, BorderLayout.WEST);

        // Refresh-Button
        JButton refreshBtn = detailBtn("↻ Refresh", ACCENT);
        header.add(refreshBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Tabs ──────────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(bg);
        tabs.setForeground(FG);
        tabs.setFont(MONO_S);

        // Tab 1: Basis-Info
        JPanel infoTab = buildInfoTab(ip, hostname, os, panBg);
        tabs.addTab("  ① Info  ", infoTab);

        // Tab 2: Ping-History
        PingHistoryPanel pingTab = new PingHistoryPanel(ip, bg, panBg);
        tabs.addTab("  ② Ping  ", pingTab);

        // Tab 3: Ports
        PortsPanel portsTab = new PortsPanel(ip, bg, panBg);
        tabs.addTab("  ③ Ports  ", portsTab);

        // Tab 4: Notiz
        JPanel notesTab = buildNotesTab(ip, category, bg, panBg);
        tabs.addTab("  ④ Notiz  ", notesTab);

        root.add(tabs, BorderLayout.CENTER);

        // Refresh startet Ping + Port-Scan neu
        refreshBtn.addActionListener(e -> {
            pingTab.restart();
            portsTab.refresh();
        });

        // Ping startet automatisch wenn Tab geöffnet wird
        pingTab.start();

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Tab 1: Basis-Info ─────────────────────────────────────────────────

    private static JPanel buildInfoTab(String ip, String hostname, String os, Color panBg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(panBg);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Basis-Felder aus Store laden
        HostResult stored = NetworkStore.getInstance().getAllHosts()
                .stream().filter(h -> h.ip.equals(ip)).findFirst().orElse(null);

        String hn       = stored != null && stored.hostname != null ? stored.hostname : hostname;
        String storedOs = stored != null && stored.os != null       ? stored.os       : os;
        String savedAt  = stored != null && stored.savedAt != null  ? stored.savedAt  : "–";

        // OS-Konfidenz
        OsDetector.OsResult osResult = OsDetector.detectWithConfidence(ip);
        String confLabel = osResult.confidence.name() + "  [" + osResult.method + "]";

        // MAC + OUI
        String mac = OsDetector.getMacFromArp(ip);
        String oui = (mac != null) ? OuiDatabase.lookup(mac) : null;
        String macStr = mac != null ? mac + (oui != null ? "  →  " + oui : "  (unbekannt)") : "nicht im ARP-Cache";

        // Hostname (ohne MAC-Teil)
        String cleanHn = hn != null && hn.contains(" [") ? hn.substring(0, hn.indexOf(" [")).trim() : hn;

        addInfoRow(p, "IP-Adresse",    ip,         ACCENT,  panBg);
        addInfoRow(p, "Hostname",      cleanHn,    FG,      panBg);
        addInfoRow(p, "OS (gespeich.)",storedOs,   osColor(storedOs), panBg);
        addInfoRow(p, "OS (aktuell)",  osResult.os,osColor(osResult.os), panBg);
        addInfoRow(p, "OS-Konfidenz",  confLabel,  FG_DIM,  panBg);
        addInfoRow(p, "MAC / OUI",     macStr,     INFO,    panBg);
        addInfoRow(p, "Gespeichert am",savedAt,    FG_DIM,  panBg);

        // Erreichbarkeit live testen
        JLabel reachLbl = infoLabel("Prüfe...", FG_DIM, panBg);
        addRowWithLabel(p, "Erreichbar", reachLbl, panBg);
        new Thread(() -> {
            try {
                long t = System.currentTimeMillis();
                boolean alive = InetAddress.getByName(ip).isReachable(2000);
                long ms = System.currentTimeMillis() - t;
                SwingUtilities.invokeLater(() -> {
                    reachLbl.setText(alive ? "✔ ja  (" + ms + " ms)" : "✕ nein");
                    reachLbl.setForeground(alive ? ACCENT2 : WARN);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> { reachLbl.setText("Fehler"); reachLbl.setForeground(WARN); });
            }
        }).start();

        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Tab 3 (Ports) als innere Klasse ───────────────────────────────────

    private static class PortsPanel extends JPanel {
        private final String ip;
        private final Color bg, panBg;
        private final JPanel listPanel;
        private final JLabel statusLbl;

        PortsPanel(String ip, Color bg, Color panBg) {
            this.ip = ip; this.bg = bg; this.panBg = panBg;
            setLayout(new BorderLayout(0, 8));
            setBackground(panBg);
            setBorder(new EmptyBorder(12, 16, 12, 16));

            statusLbl = new JLabel("  Noch nicht gescannt  →  Klicke 'Scan starten'");
            statusLbl.setFont(MONO_S);
            statusLbl.setForeground(FG_DIM);

            JButton scanBtn = detailBtn("⊕ Scan starten", ACCENT2);
            scanBtn.addActionListener(e -> refresh());

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setBackground(panBg);
            top.add(statusLbl, BorderLayout.CENTER);
            top.add(scanBtn,   BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBackground(panBg);
            JScrollPane sp = new JScrollPane(listPanel);
            sp.setBorder(new LineBorder(BORDER, 1));
            sp.getViewport().setBackground(panBg);
            add(sp, BorderLayout.CENTER);
        }

        void refresh() {
            statusLbl.setText("  Scanne Ports…");
            statusLbl.setForeground(ACCENT);
            listPanel.removeAll(); listPanel.revalidate(); listPanel.repaint();
            new Thread(() -> {
                try {
                    Map<Integer, String> ports = PortScanner.scanParallel(ip, 1500);
                    SwingUtilities.invokeLater(() -> {
                        listPanel.removeAll();
                        if (ports.isEmpty()) {
                            JLabel none = new JLabel("  Keine offenen Ports gefunden.");
                            none.setFont(MONO_S); none.setForeground(FG_DIM);
                            listPanel.add(none);
                        } else {
                            ports.entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .forEach(e -> listPanel.add(portRow(e.getKey(), e.getValue())));
                        }
                        statusLbl.setText("  " + ports.size() + " Port(s) offen  –  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        statusLbl.setForeground(ACCENT2);
                        listPanel.revalidate(); listPanel.repaint();
                    });
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }).start();
        }

        private JPanel portRow(int port, String banner) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setBackground(panBg);
            row.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(5, 8, 5, 8)));
            JLabel portLbl = new JLabel(String.format("%-6d", port));
            portLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
            portLbl.setForeground(ACCENT);
            JLabel bannerLbl = new JLabel(banner != null ? banner : "offen");
            bannerLbl.setFont(MONO_S);
            bannerLbl.setForeground(FG);
            row.add(portLbl,   BorderLayout.WEST);
            row.add(bannerLbl, BorderLayout.CENTER);
            return row;
        }
    }

    // ── Tab 2 (Ping-History) als innere Klasse ────────────────────────────

    private static class PingHistoryPanel extends JPanel {
        private final String ip;
        private final List<Long> history = new ArrayList<>(Collections.nCopies(20, -1L));
        private volatile boolean running = false;
        private Thread pingThread;
        private final JLabel statsLbl;
        private final JPanel graphPanel;

        PingHistoryPanel(String ip, Color bg, Color panBg) {
            this.ip = ip;
            setLayout(new BorderLayout(0, 8));
            setBackground(panBg);
            setBorder(new EmptyBorder(12, 16, 12, 16));

            statsLbl = new JLabel("  Starte Ping…");
            statsLbl.setFont(MONO_S);
            statsLbl.setForeground(FG_DIM);

            JButton stopBtn = detailBtn("■ Stop", WARN);
            stopBtn.addActionListener(e -> { running = false; if (pingThread != null) pingThread.interrupt(); });

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setBackground(panBg);
            top.add(statsLbl, BorderLayout.CENTER);
            top.add(stopBtn,  BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            graphPanel = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintGraph((Graphics2D) g, getWidth(), getHeight());
                }
            };
            graphPanel.setBackground(GuiTheme.isDark() ? new Color(0x05, 0x08, 0x06) : new Color(0xF8, 0xF6, 0xF2));
            graphPanel.setPreferredSize(new Dimension(0, 160));
            add(graphPanel, BorderLayout.CENTER);

            // History-Liste
            JPanel histList = buildHistoryList();
            JScrollPane sp = new JScrollPane(histList);
            sp.setBorder(new LineBorder(BORDER, 1));
            sp.getViewport().setBackground(panBg);
            sp.setPreferredSize(new Dimension(0, 200));
            add(sp, BorderLayout.SOUTH);
        }

        private final DefaultListModel<String> listModel = new DefaultListModel<>();

        private JPanel buildHistoryList() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(GuiTheme.isDark() ? new Color(0x05, 0x08, 0x06) : Color.WHITE);
            JList<String> list = new JList<>(listModel);
            list.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
            list.setForeground(FG);
            list.setBackground(p.getBackground());
            p.add(list);
            return p;
        }

        void start() {
            if (running) return;
            running = true;
            pingThread = new Thread(() -> {
                long sent = 0, lost = 0, totalMs = 0;
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        long t = System.currentTimeMillis();
                        boolean alive = InetAddress.getByName(ip).isReachable(2000);
                        long ms = System.currentTimeMillis() - t;
                        sent++;
                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        synchronized (history) {
                            history.remove(0);
                            history.add(alive ? ms : -1L);
                        }
                        if (alive) totalMs += ms; else lost++;
                        final long fs = sent, fl = lost, fms = totalMs;
                        final String entry = time + "  " + (alive ? ms + " ms" : "TIMEOUT");
                        final Color entryColor = alive ? (ms < 50 ? ACCENT2 : ms < 200 ? ACCENT : WARN) : WARN;
                        SwingUtilities.invokeLater(() -> {
                            listModel.add(0, entry);
                            if (listModel.size() > 100) listModel.remove(listModel.size() - 1);
                            long avg = fs - fl > 0 ? fms / (fs - fl) : 0;
                            statsLbl.setText(String.format("  Gesendet: %d  Verlust: %d (%.0f%%)  Ø: %d ms",
                                    fs, fl, fl * 100.0 / Math.max(fs, 1), avg));
                            statsLbl.setForeground(fl == 0 ? ACCENT2 : WARN);
                            graphPanel.repaint();
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) { break; }
                    catch (Exception ex) { /* ignorieren */ }
                }
            }, "PingHistory-" + ip);
            pingThread.setDaemon(true);
            pingThread.start();
        }

        void restart() { running = false; if (pingThread != null) pingThread.interrupt();
            history.replaceAll(_ -> -1L); listModel.clear(); start(); }

        private void paintGraph(Graphics2D g2, int w, int h) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(graphPanel.getBackground()); g2.fillRect(0, 0, w, h);
            // Grid
            g2.setColor(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x1A) : new Color(0xE0, 0xDE, 0xD8));
            for (int y = h / 4; y < h; y += h / 4) g2.drawLine(0, y, w, y);

            List<Long> snap;
            synchronized (history) { snap = new ArrayList<>(history); }
            long max = snap.stream().filter(v -> v >= 0).mapToLong(v -> v).max().orElse(100);
            if (max < 50) max = 50;
            int barW = Math.max(2, w / snap.size());
            int prevX = -1, prevY = -1;
            for (int i = 0; i < snap.size(); i++) {
                long v = snap.get(i);
                int cx = i * barW + barW / 2;
                if (v < 0) {
                    g2.setColor(WARN); g2.fillOval(cx - 3, h - 10, 6, 6);
                    prevX = prevY = -1; continue;
                }
                int cy = Math.max(4, h - 4 - (int)(v * (h - 8) / max));
                Color col = v < 20 ? ACCENT2 : v < 100 ? ACCENT : v < 300 ? new Color(0xFF, 0xD0, 0x50) : WARN;
                g2.setColor(col);
                if (prevX >= 0) { g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(prevX, prevY, cx, cy); }
                g2.fillOval(cx - 3, cy - 3, 6, 6);
                prevX = cx; prevY = cy;
            }
        }
    }

    // ── Tab 4: Notiz-Editor ───────────────────────────────────────────────

    private static JPanel buildNotesTab(String ip, String category, Color bg, Color panBg) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(panBg);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        HostResult stored = NetworkStore.getInstance().getAllHosts()
                .stream().filter(h -> h.ip.equals(ip)).findFirst().orElse(null);
        String current = stored != null && stored.notes != null ? stored.notes : "";

        JTextArea area = new JTextArea(current);
        area.setFont(MONO);
        area.setForeground(GuiTheme.isDark() ? new Color(0xFF, 0xE8, 0x90) : new Color(0x60, 0x48, 0x08));
        area.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x0E, 0x04) : new Color(0xFF, 0xFB, 0xE8));
        area.setCaretColor(ACCENT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(BORDER, 1));
        p.add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnRow.setBackground(panBg);
        JLabel savedLbl = new JLabel("");
        savedLbl.setFont(MONO_XS);
        savedLbl.setForeground(ACCENT2);
        JButton saveBtn = detailBtn("💾 Speichern", ACCENT2);
        saveBtn.addActionListener(e -> {
            String cat = category != null ? category : NetworkStore.ALL_CATEGORY;
            NetworkStore.getInstance().updateNotes(ip, cat, area.getText());
            savedLbl.setText("✔ gespeichert  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        btnRow.add(savedLbl);
        btnRow.add(saveBtn);
        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

    private static void addInfoRow(JPanel p, String label, String value, Color valColor, Color bg) {
        JLabel lbl = infoLabel(value, valColor, bg);
        addRowWithLabel(p, label, lbl, bg);
    }

    private static void addRowWithLabel(JPanel p, String label, JLabel valueLbl, Color bg) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(bg);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(6, 4, 6, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel keyLbl = new JLabel(label);
        keyLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        keyLbl.setForeground(FG_DIM);
        keyLbl.setPreferredSize(new Dimension(140, 20));
        row.add(keyLbl,   BorderLayout.WEST);
        row.add(valueLbl, BorderLayout.CENTER);
        p.add(row);
    }

    private static JLabel infoLabel(String text, Color col, Color bg) {
        JLabel l = new JLabel(text != null ? text : "–");
        l.setFont(MONO_S);
        l.setForeground(col);
        l.setBackground(bg);
        return l;
    }

    private static JButton detailBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x1E, 0x18) : new Color(0xE0, 0xDE, 0xD8));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 10, 4, 10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(GuiTheme.isDark() ? new Color(0x25, 0x2E, 0x25) : new Color(0xD0, 0xCE, 0xC8)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x1E, 0x18) : new Color(0xE0, 0xDE, 0xD8)); }
        });
        return b;
    }
}