package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.model.HostResult;
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
 * Zeigt: eigenen Rechner (SELF), Gateway, alle gespeicherten Hosts.
 * Doppelklick → Host-Details. Drag → verschieben. Scroll → Zoom.
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

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC));
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel title = new JLabel("  Netzwerk-Karte  –  "
                + NetworkStore.getInstance().getAllHosts().size() + " Hosts");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        title.setForeground(ACCENT);
        toolbar.add(title);

        JButton refreshBtn = mapBtn("↻ Aktualisieren", ACCENT2);
        JButton layoutBtn  = mapBtn("⊞ Layout",        INFO);
        toolbar.add(refreshBtn);
        toolbar.add(layoutBtn);
        root.add(toolbar, BorderLayout.NORTH);

        MapCanvas canvas = new MapCanvas(bg);
        JScrollPane sp = new JScrollPane(canvas);
        sp.setBorder(null);
        sp.getViewport().setBackground(bg);
        root.add(sp, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> canvas.reload());
        layoutBtn.addActionListener(e -> { canvas.autoLayout(); canvas.repaint(); });
        sp.addMouseWheelListener(e -> canvas.zoom(e.getWheelRotation() < 0 ? 1.1 : 0.9));

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    private static class MapCanvas extends JPanel {
        private final List<Node> nodes = new ArrayList<>();
        private double scale    = 1.0;
        private Node   dragging = null;
        private Point  dragOffset;
        private final Color bg;

        MapCanvas(Color bg) {
            this.bg = bg;
            setBackground(bg);
            setPreferredSize(new Dimension(1200, 800));
            reload();
            installMouse();
        }

        void reload() {
            nodes.clear();

            // Gateway
            String gw = RemoteNetScanner.detectDefaultGateway();
            if (gw != null) nodes.add(new Node(gw, "Gateway", "Router / Netzwerkgerät", NodeType.GATEWAY));

            // Eigener Rechner (SELF)
            try {
                InetAddress self = InetAddress.getLocalHost();
                String selfIp   = self.getHostAddress();
                String selfName = self.getHostName();
                nodes.add(new Node(selfIp, selfName + " (ich)", getCurrentOs(), NodeType.SELF));
            } catch (Exception ignored) {}

            // Gespeicherte Hosts
            for (HostResult h : NetworkStore.getInstance().getAllHosts()) {
                String hn = h.hostname != null && h.hostname.contains(" [")
                        ? h.hostname.substring(0, h.hostname.indexOf(" [")) : h.hostname;
                nodes.add(new Node(h.ip, hn, h.os, NodeType.HOST));
            }
            autoLayout();
            repaint();
        }

        private static String getCurrentOs() {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) return "Windows";
            if (os.contains("mac")) return "macOS";
            return "Linux/Unix";
        }

        void autoLayout() {
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            int cx = w / 2, cy = h / 2;

            Node gw   = nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst().orElse(null);
            Node self = nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst().orElse(null);

            if (gw != null) { gw.x = cx;       gw.y = cy; }
            if (self != null) {
                // Eigener Rechner oben-links vom Gateway
                self.x = cx - 120; self.y = cy - 120;
            }

            List<Node> hosts = nodes.stream().filter(n -> n.type == NodeType.HOST).toList();
            int count = hosts.size();
            if (count == 0) return;
            double step = 2 * Math.PI / Math.max(count, 1);
            int radius = Math.min(cx, cy) - 80;
            for (int i = 0; i < count; i++) {
                double angle = i * step - Math.PI / 2;
                hosts.get(i).x = (int)(cx + radius * Math.cos(angle));
                hosts.get(i).y = (int)(cy + radius * Math.sin(angle));
            }
        }

        void zoom(double factor) {
            scale = Math.max(0.3, Math.min(3.0, scale * factor));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(scale, scale);

            int w = (int)(getWidth() / scale), h = (int)(getHeight() / scale);
            g2.setColor(bg); g2.fillRect(0, 0, w, h);

            // Grid
            g2.setColor(GuiTheme.isDark() ? new Color(0x12, 0x18, 0x12) : new Color(0xE4, 0xE2, 0xDC));
            for (int x = 0; x < w; x += 60) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 60) g2.drawLine(0, y, w, y);

            Node gw   = nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst().orElse(null);
            Node self = nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst().orElse(null);

            // Verbindungslinien: alle Hosts → Gateway
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g2.setColor(GuiTheme.isDark() ? new Color(0x30, 0x45, 0x30) : new Color(0xC0, 0xBC, 0xB4));
            if (gw != null) {
                for (Node n : nodes) {
                    if (n == gw) continue;
                    g2.drawLine(gw.x, gw.y, n.x, n.y);
                }
            }
            // Self → Gateway (dicker)
            if (gw != null && self != null) {
                g2.setStroke(new BasicStroke(2.0f));
                g2.setColor(ACCENT2);
                g2.drawLine(self.x, self.y, gw.x, gw.y);
            }

            g2.setStroke(new BasicStroke(1.5f));
            for (Node n : nodes) drawNode(g2, n);
            drawLegend(g2, w, h);
            g2.dispose();
        }

        private void drawNode(Graphics2D g2, Node n) {
            Color col = nodeColor(n);
            int r = n.type == NodeType.GATEWAY ? 22 : n.type == NodeType.SELF ? 18 : 14;

            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
            g2.fillOval(n.x-r-6, n.y-r-6, (r+6)*2, (r+6)*2);

            g2.setColor(GuiTheme.isDark() ? new Color(0x08, 0x0C, 0x08) : new Color(0xF4, 0xF2, 0xEE));
            g2.fillOval(n.x-r, n.y-r, r*2, r*2);
            g2.setColor(col);
            g2.drawOval(n.x-r, n.y-r, r*2, r*2);

            // Icon
            String icon = switch (n.type) {
                case GATEWAY -> "G";
                case SELF    -> "★";
                default      -> osIcon(n.os);
            };
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 12 : 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(col);
            g2.drawString(icon, n.x - fm.stringWidth(icon)/2, n.y + fm.getAscent()/2 - 1);

            // IP
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            g2.setColor(GuiTheme.isDark() ? new Color(0xA0, 0x9C, 0x90) : new Color(0x40, 0x42, 0x3E));
            g2.drawString(n.ip, n.x - fm.stringWidth(n.ip)/2, n.y + r + 14);

            // Hostname
            if (n.hostname != null && !n.hostname.equals(n.ip)) {
                String hn = n.hostname.length() > 16 ? n.hostname.substring(0,15) + "…" : n.hostname;
                g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                g2.setColor(n.type == NodeType.SELF ? ACCENT2 : FG_DIM);
                g2.drawString(hn, n.x - fm.stringWidth(hn)/2, n.y + r + 24);
            }
        }

        private void drawLegend(Graphics2D g2, int w, int h) {
            int lx = 10, ly = h - 110;
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            Object[][] legend = {
                    {"★", "Ich (dieser PC)",   ACCENT2},
                    {"G", "Gateway",            NET_COL},
                    {"W", "Windows",            WIN_COL},
                    {"L", "Linux",              LIN_COL},
                    {"M", "macOS",              APL_COL},
                    {"A", "Android",            AND_COL},
            };
            for (int i = 0; i < legend.length; i++) {
                g2.setColor((Color) legend[i][2]);
                g2.fillOval(lx, ly + i*16, 10, 10);
                g2.setColor(GuiTheme.isDark() ? FG_DIM : new Color(0x50, 0x52, 0x4E));
                g2.drawString((String) legend[i][1], lx + 15, ly + i*16 + 9);
            }
        }

        private static Color nodeColor(Node n) {
            return switch (n.type) {
                case SELF    -> ACCENT2;
                case GATEWAY -> NET_COL;
                default      -> osColor(n.os);
            };
        }

        private static String osIcon(String os) {
            if (os == null) return "?";
            String l = os.toLowerCase();
            if (l.contains("windows")) return "W";
            if (l.contains("linux"))   return "L";
            if (l.contains("mac") || l.contains("ios")) return "M";
            if (l.contains("android")) return "A";
            if (l.contains("router"))  return "R";
            return "?";
        }

        private void installMouse() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    Point sp = scale(e.getPoint());
                    dragging = nodes.stream()
                            .filter(n -> Math.hypot(n.x-sp.x, n.y-sp.y) < 28)
                            .findFirst().orElse(null);
                    if (dragging != null)
                        dragOffset = new Point(sp.x - dragging.x, sp.y - dragging.y);
                }
                public void mouseReleased(MouseEvent e) { dragging = null; }
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Point sp = scale(e.getPoint());
                        nodes.stream()
                                .filter(n -> Math.hypot(n.x-sp.x, n.y-sp.y) < 28)
                                .findFirst()
                                .ifPresent(n -> HostDetailsPanel.show(n.ip, n.hostname, n.os,
                                        NetworkStore.getInstance().findNetwork(n.ip)));
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (dragging == null) return;
                    Point sp = scale(e.getPoint());
                    dragging.x = sp.x - dragOffset.x;
                    dragging.y = sp.y - dragOffset.y;
                    repaint();
                }
                public void mouseMoved(MouseEvent e) {
                    Point sp = scale(e.getPoint());
                    boolean over = nodes.stream()
                            .anyMatch(n -> Math.hypot(n.x-sp.x, n.y-sp.y) < 28);
                    setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        private Point scale(Point p) {
            return new Point((int)(p.x / scale), (int)(p.y / scale));
        }
    }

    // ── Datenklassen ──────────────────────────────────────────────────────

    private enum NodeType { GATEWAY, SELF, HOST }

    private static class Node {
        String ip, hostname, os;
        NodeType type;
        int x, y;
        Node(String ip, String hn, String os, NodeType t) {
            this.ip = ip; this.hostname = hn; this.os = os; this.type = t;
        }
    }

    private static JButton mapBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 10, 4, 10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}