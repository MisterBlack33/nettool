package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.scan.NetworkHostScanner;
import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.logic.scan.ScanHistory;
import main.java.networktool_v3.logic.scan.SubnetDetector;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Netzwerk-Topologie-Karte.
 *
 * Beim Oeffnen wird automatisch ein Hintergrund-Scan gestartet
 * damit auch ohne vorherigen manuellen Scan alle Hosts sichtbar sind.
 *
 * Datenquellen (dedupliziert per IP):
 *  1. ScanHistory  - alle Scans der Session (inkl. automatischer Scan beim Oeffnen)
 *  2. NetworkStore - dauerhaft gespeicherte Hosts
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    public static void show() {
        SwingUtilities.invokeLater(GuiNetworkMap::buildWindow);
    }

    private static void buildWindow() {
        JDialog dlg = new JDialog((Frame) null, "Netzwerk-Karte", false);
        dlg.setSize(900, 640);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg = GuiTheme.isDark() ? new Color(0x06, 0x09, 0x07) : new Color(0xF2, 0xF0, 0xEC);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);

        MapCanvas canvas = new MapCanvas(bg);
        JLabel statusLbl = buildStatusLabel();
        JPanel toolbar = buildToolbar(canvas, statusLbl);
        root.add(toolbar, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(canvas);
        sp.setBorder(null);
        sp.getViewport().setBackground(bg);
        root.add(sp, BorderLayout.CENTER);
        root.add(statusLbl, BorderLayout.SOUTH);
        sp.addMouseWheelListener(e -> canvas.zoom(e.getWheelRotation() < 0 ? 1.1 : 0.9));

        dlg.setContentPane(root);
        dlg.setVisible(true);

        // Automatischer Hintergrund-Scan beim Oeffnen der Karte
        startBackgroundScan(canvas, statusLbl);
    }

    private static JLabel buildStatusLabel() {
        JLabel lbl = new JLabel("  Starte Scan...");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static JPanel buildToolbar(MapCanvas canvas, JLabel statusLbl) {
        Color barBg = GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel title = new JLabel("  Netzwerk-Karte  -  0 Hosts");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        title.setForeground(ACCENT);
        canvas.setTitleLabel(title);

        JButton refreshBtn = mapBtn("Aktualisieren", ACCENT2);
        JButton layoutBtn  = mapBtn("Layout",        INFO);
        bar.add(title); bar.add(refreshBtn); bar.add(layoutBtn);

        refreshBtn.addActionListener(e -> {
            canvas.reload();
            title.setText("  Netzwerk-Karte  -  " + canvas.hostCount() + " Hosts");
        });
        layoutBtn.addActionListener(e -> { canvas.autoLayout(); canvas.repaint(); });
        return bar;
    }

    /**
     * Startet im Hintergrund einen Subnetz-Scan.
     * Ergebnis landet in ScanHistory -> canvas.reload() zeigt es.
     */
    private static void startBackgroundScan(MapCanvas canvas, JLabel statusLbl) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> statusLbl.setText("  Scanne Netzwerk..."));
                List<String> subnets = SubnetDetector.getAllSubnets();
                if (subnets.isEmpty()) {
                    SwingUtilities.invokeLater(() -> statusLbl.setText("  Kein Subnetz gefunden."));
                    return;
                }
                // Scan - Ergebnis wird automatisch in ScanHistory eingetragen
                NetworkHostScanner.scan(subnets);

                SwingUtilities.invokeLater(() -> {
                    canvas.reload();
                    statusLbl.setText("  " + canvas.hostCount() + " Hosts gefunden.");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLbl.setText("  Scan fehlgeschlagen: " + e.getMessage()));
            }
        }, "MapBgScan").start();
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    static class MapCanvas extends JPanel {
        private final List<Node> nodes = new ArrayList<>();
        private double scale = 1.0;
        private Node  dragging;
        private Point dragOffset;
        private final Color bg;
        private JLabel titleLabel;

        MapCanvas(Color bg) {
            this.bg = bg;
            setBackground(bg);
            setPreferredSize(new Dimension(1200, 800));
            reload();
            installMouse();
        }

        void setTitleLabel(JLabel lbl) { this.titleLabel = lbl; }

        int hostCount() {
            return (int) nodes.stream().filter(n -> n.type == NodeType.HOST).count();
        }

        void reload() {
            nodes.clear();
            Set<String> seen = new LinkedHashSet<>();

            // Gateway
            String gw = RemoteNetScanner.detectDefaultGateway();
            if (gw != null && seen.add(gw))
                nodes.add(new Node(gw, "Gateway", "Router / Netzwerkgeraet", NodeType.GATEWAY));

            // Eigener Rechner
            try {
                InetAddress self = InetAddress.getLocalHost();
                if (seen.add(self.getHostAddress()))
                    nodes.add(new Node(self.getHostAddress(),
                            self.getHostName() + " (ich)", localOs(), NodeType.SELF));
            } catch (Exception ignored) {}

            // 1. ScanHistory - alle Scans der Session
            for (ScanHistory.Entry entry : ScanHistory.getInstance().getAll())
                for (ScanResult r : entry.results)
                    if (seen.add(r.getIp()))
                        nodes.add(new Node(r.getIp(), r.getHostname(), r.getOsGuess(), NodeType.HOST));

            // 2. NetworkStore - gespeicherte Hosts
            for (HostResult h : NetworkStore.getInstance().getAllHosts())
                if (seen.add(h.ip))
                    nodes.add(new Node(h.ip, cleanHostname(h.hostname), h.os, NodeType.HOST));

            autoLayout();
            repaint();

            if (titleLabel != null)
                SwingUtilities.invokeLater(() ->
                        titleLabel.setText("  Netzwerk-Karte  -  " + hostCount() + " Hosts"));
        }

        void autoLayout() {
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            int cx = w / 2, cy = h / 2;

            nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst()
                    .ifPresent(n -> { n.x = cx; n.y = cy; });
            nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst()
                    .ifPresent(n -> { n.x = cx - 120; n.y = cy - 120; });

            List<Node> hosts = nodes.stream().filter(n -> n.type == NodeType.HOST).toList();
            if (hosts.isEmpty()) return;
            double step = 2 * Math.PI / hosts.size();
            int radius = Math.max(80, Math.min(cx, cy) - 80);
            for (int i = 0; i < hosts.size(); i++) {
                double angle = i * step - Math.PI / 2;
                hosts.get(i).x = (int)(cx + radius * Math.cos(angle));
                hosts.get(i).y = (int)(cy + radius * Math.sin(angle));
            }
        }

        void zoom(double f) { scale = Math.max(0.3, Math.min(3.0, scale * f)); repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(scale, scale);

            int w = (int)(getWidth() / scale), h = (int)(getHeight() / scale);
            g2.setColor(bg); g2.fillRect(0, 0, w, h);

            g2.setColor(GuiTheme.isDark() ? new Color(0x12,0x18,0x12) : new Color(0xE4,0xE2,0xDC));
            for (int x = 0; x < w; x += 60) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 60) g2.drawLine(0, y, w, y);

            Node gw   = nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst().orElse(null);
            Node self = nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst().orElse(null);

            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g2.setColor(GuiTheme.isDark() ? new Color(0x30,0x45,0x30) : new Color(0xC0,0xBC,0xB4));
            if (gw != null)
                for (Node n : nodes) if (n != gw) g2.drawLine(gw.x, gw.y, n.x, n.y);

            if (gw != null && self != null) {
                g2.setStroke(new BasicStroke(2f)); g2.setColor(ACCENT2);
                g2.drawLine(self.x, self.y, gw.x, gw.y);
            }

            g2.setStroke(new BasicStroke(1.5f));
            for (Node n : nodes) drawNode(g2, n);
            drawLegend(g2, h);
            g2.dispose();
        }

        private void drawNode(Graphics2D g2, Node n) {
            Color col = nodeColor(n);
            int r = n.type == NodeType.GATEWAY ? 22 : n.type == NodeType.SELF ? 18 : 14;

            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
            g2.fillOval(n.x-r-6, n.y-r-6, (r+6)*2, (r+6)*2);
            g2.setColor(GuiTheme.isDark() ? new Color(0x08,0x0C,0x08) : new Color(0xF4,0xF2,0xEE));
            g2.fillOval(n.x-r, n.y-r, r*2, r*2);
            g2.setColor(col); g2.drawOval(n.x-r, n.y-r, r*2, r*2);

            String icon = switch (n.type) {
                case GATEWAY -> "G"; case SELF -> "*"; default -> osIcon(n.os);
            };
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 12 : 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(col);
            g2.drawString(icon, n.x - fm.stringWidth(icon)/2, n.y + fm.getAscent()/2 - 1);

            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            g2.setColor(GuiTheme.isDark() ? new Color(0xA0,0x9C,0x90) : new Color(0x40,0x42,0x3E));
            g2.drawString(n.ip, n.x - fm.stringWidth(n.ip)/2, n.y + r + 14);

            if (n.hostname != null && !n.hostname.equals(n.ip)) {
                String hn = n.hostname.length() > 16 ? n.hostname.substring(0,15) + "." : n.hostname;
                g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                g2.setColor(n.type == NodeType.SELF ? ACCENT2 : FG_DIM);
                g2.drawString(hn, n.x - fm.stringWidth(hn)/2, n.y + r + 24);
            }
        }

        private void drawLegend(Graphics2D g2, int h) {
            int lx = 10, ly = h - 110;
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            Object[][] leg = {
                    {"*","Ich",ACCENT2},{"G","Gateway",NET_COL},{"W","Windows",WIN_COL},
                    {"L","Linux",LIN_COL},{"M","macOS",APL_COL},{"A","Android",AND_COL}
            };
            for (int i = 0; i < leg.length; i++) {
                g2.setColor((Color) leg[i][2]); g2.fillOval(lx, ly + i*16, 10, 10);
                g2.setColor(GuiTheme.isDark() ? FG_DIM : new Color(0x50,0x52,0x4E));
                g2.drawString((String) leg[i][1], lx+15, ly+i*16+9);
            }
        }

        private void installMouse() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    Point sp = scale(e.getPoint());
                    dragging = nodes.stream().filter(n -> dist(n,sp) < 28).findFirst().orElse(null);
                    if (dragging != null) dragOffset = new Point(sp.x - dragging.x, sp.y - dragging.y);
                }
                public void mouseReleased(MouseEvent e) { dragging = null; }
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    Point sp = scale(e.getPoint());
                    nodes.stream().filter(n -> dist(n,sp) < 28).findFirst()
                            .ifPresent(n -> HostDetailsPanel.show(n.ip, n.hostname, n.os,
                                    NetworkStore.getInstance().findNetwork(n.ip)));
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (dragging == null) return;
                    Point sp = scale(e.getPoint());
                    dragging.x = sp.x - dragOffset.x; dragging.y = sp.y - dragOffset.y; repaint();
                }
                public void mouseMoved(MouseEvent e) {
                    Point sp = scale(e.getPoint());
                    setCursor(nodes.stream().anyMatch(n -> dist(n,sp) < 28)
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                }
            });
        }

        private Point scale(Point p) { return new Point((int)(p.x/scale), (int)(p.y/scale)); }
        private double dist(Node n, Point p) { return Math.hypot(n.x-p.x, n.y-p.y); }
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────

    private static Color nodeColor(Node n) {
        return switch (n.type) { case SELF -> ACCENT2; case GATEWAY -> NET_COL; default -> osColor(n.os); };
    }

    private static String osIcon(String os) {
        if (os == null) return "?";
        String l = os.toLowerCase();
        if (l.contains("windows")) return "W";
        if (l.contains("linux"))   return "L";
        if (l.contains("mac")||l.contains("ios")) return "M";
        if (l.contains("android")) return "A";
        if (l.contains("router")||l.contains("fritz")||l.contains("switch")) return "R";
        if (l.contains("drucker")||l.contains("printer")) return "P";
        return "?";
    }

    private static String cleanHostname(String h) {
        if (h == null) return ""; int i = h.indexOf(" ["); return i < 0 ? h : h.substring(0,i).trim();
    }

    private static String localOs() {
        String os = System.getProperty("os.name","").toLowerCase();
        if (os.contains("win")) return "Windows";
        if (os.contains("mac")) return "macOS";
        return "Linux/Unix";
    }

    private static JButton mapBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18,0x22,0x18) : new Color(0xDC,0xDA,0xD4));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(),1), new EmptyBorder(4,10,4,10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private enum NodeType { GATEWAY, SELF, HOST }

    private static class Node {
        String ip, hostname, os; NodeType type; int x, y;
        Node(String ip, String hn, String os, NodeType t) { this.ip=ip; hostname=hn; this.os=os; type=t; }
    }
}