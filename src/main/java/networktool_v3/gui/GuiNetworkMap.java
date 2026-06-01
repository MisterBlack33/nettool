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
import java.util.concurrent.ConcurrentHashMap;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Netzwerk-Topologie-Karte.
 * Koordiniert MapSwitchStore, MapTopology, MapHopDiscovery, MapLayout, MapRenderer.
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    static final Map<String, String> HOP_PARENT =
            Collections.synchronizedMap(new HashMap<>());

    public static void show() {
        SwingUtilities.invokeLater(GuiNetworkMap::buildWindow);
    }

    // ── Window ────────────────────────────────────────────────────────────

    private static void buildWindow() {
        JDialog dlg = new JDialog((Frame) null, "Netzwerk-Karte", false);
        dlg.setSize(900, 640);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg = GuiTheme.isDark() ? new Color(0x06, 0x09, 0x07) : new Color(0xF2, 0xF0, 0xEC);
        MapCanvas canvas    = new MapCanvas(bg);
        JLabel   statusLbl  = statusLabel();
        canvas.setStatusLabel(statusLbl);

        JLayeredPane layered = buildLayered(canvas, bg);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.add(buildToolbar(canvas), BorderLayout.NORTH);
        root.add(layered,              BorderLayout.CENTER);
        root.add(statusLbl,            BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
        startBackgroundScan(canvas);
    }

    private static JLayeredPane buildLayered(MapCanvas canvas, Color bg) {
        JLayeredPane pane = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                Component leg = getComponentsInLayer(JLayeredPane.PALETTE_LAYER)[0];
                Dimension ls = leg.getPreferredSize();
                leg.setBounds(10, h - ls.height - 10, ls.width, ls.height);
            }
        };
        pane.setBackground(bg);
        pane.setOpaque(true);
        pane.add(canvas,        JLayeredPane.DEFAULT_LAYER);
        pane.add(buildLegend(), JLayeredPane.PALETTE_LAYER);
        return pane;
    }

    private static JPanel buildToolbar(MapCanvas canvas) {
        Color barBg = GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel titleLbl = new JLabel("  Netzwerk-Karte");
        titleLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        titleLbl.setForeground(ACCENT);
        canvas.setTitleLabel(titleLbl);

        JTextField switchInput = buildSwitchInput();
        JButton addSwitchBtn   = toolBtn("+ Switch", new Color(0xFF, 0xA0, 0x30));
        JButton refreshBtn     = toolBtn("↻", ACCENT2);
        JButton layoutBtn      = toolBtn("⊞", INFO);

        addSwitchBtn.addActionListener(e -> addManualSwitch(switchInput.getText(), canvas));
        switchInput.addActionListener(e -> addManualSwitch(switchInput.getText(), canvas));
        refreshBtn.addActionListener(e -> { HOP_PARENT.clear(); canvas.reload(); });
        layoutBtn.addActionListener(e -> canvas.resetLayout());

        bar.add(titleLbl); bar.add(switchInput);
        bar.add(addSwitchBtn); bar.add(refreshBtn); bar.add(layoutBtn);
        return bar;
    }

    private static JTextField buildSwitchInput() {
        JTextField f = new JTextField(16);
        f.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        f.setForeground(FG);
        f.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x14, 0x10) : Color.WHITE);
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFF, 0xA0, 0x30), 1),
                new EmptyBorder(3, 6, 3, 6)));
        f.putClientProperty("JTextField.placeholderText", "Switch-IP eingeben…");
        return f;
    }

    private static void addManualSwitch(String ip, MapCanvas canvas) {
        String trimmed = ip.trim();
        if (trimmed.isEmpty()) return;
        MapSwitchStore.add(trimmed);
        canvas.reload();
    }

    private static JPanel buildLegend() {
        Color legBg = new Color(0x08, 0x0C, 0x08, 210);
        Object[][] rows = {
                {"*", "Ich (lokal)",  ACCENT2},
                {"G", "Gateway",      NET_COL},
                {"S", "Switch/Hub",   new Color(0xFF, 0xA0, 0x30)},
                {"W", "Windows",      WIN_COL},
                {"L", "Linux",        LIN_COL},
                {"M", "macOS / iOS",  APL_COL},
                {"A", "Android",      AND_COL},
                {"P", "Drucker",      PRN_COL},
                {"?", "Unbekannt",    FG_DIM},
        };
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(legBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(6, 8, 6, 12));
        for (Object[] row : rows) {
            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
            line.setOpaque(false);
            JLabel dot = new JLabel("\u25CF");
            dot.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
            dot.setForeground((Color) row[2]);
            JLabel lbl = new JLabel((String) row[1]);
            lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
            lbl.setForeground(new Color(0xC8, 0xC4, 0xB8));
            line.add(dot); line.add(lbl);
            panel.add(line);
        }
        panel.setPreferredSize(new Dimension(170, rows.length * 18 + 14));
        return panel;
    }

    private static JLabel statusLabel() {
        JLabel lbl = new JLabel("  Scanne...");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static void startBackgroundScan(MapCanvas canvas) {
        new Thread(() -> {
            try {
                List<String> subnets = SubnetDetector.getAllSubnets();
                if (!subnets.isEmpty()) NetworkHostScanner.scan(subnets);
                SwingUtilities.invokeLater(() -> canvas.setStatus("  Traceroute läuft..."));
                HOP_PARENT.putAll(MapHopDiscovery.discover());
                SwingUtilities.invokeLater(canvas::reload);
            } catch (Exception ignored) {}
        }, "MapBgScan").start();
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    static class MapCanvas extends JPanel {

        final List<Node> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        private final Color bg;

        private JLabel titleLabel;
        private JLabel statusLabel;

        private double scale = 1.0, camX = 0, camY = 0;
        private Node  dragNode;
        private Point dragStart;
        private int   dragOrigX, dragOrigY;
        private boolean panning;
        private Point   panStart;

        MapCanvas(Color bg) {
            this.bg = bg;
            setBackground(bg);
            reload();
            installMouse();
        }

        void setTitleLabel(JLabel l)  { this.titleLabel  = l; }
        void setStatusLabel(JLabel l) { this.statusLabel = l; }

        int hostCount() {
            return (int) nodes.stream().filter(n -> n.type == NodeType.HOST).count();
        }

        void setStatus(String msg) {
            if (statusLabel != null)
                SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
        }

        // ── Build ─────────────────────────────────────────────────────────

        void reload() {
            nodes.clear();
            edges.clear();

            Node gwNode   = collectNodes();
            Node selfNode = nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst().orElse(null);

            MapTopology.classifyNodes(nodes);
            List<Edge> built = MapTopology.buildEdges(nodes, gwNode, selfNode, HOP_PARENT);
            edges.addAll(built);

            resetLayout();
            updateLabels();
        }

        private Node collectNodes() {
            Set<String> seen = new LinkedHashSet<>();
            Node gwNode = null;

            String gw = RemoteNetScanner.detectDefaultGateway();
            if (gw != null && seen.add(gw))
                gwNode = addNode(gw, "Gateway", "Router / Netzwerkgeraet", NodeType.GATEWAY);

            try {
                InetAddress self = InetAddress.getLocalHost();
                if (seen.add(self.getHostAddress()))
                    addNode(self.getHostAddress(), self.getHostName() + " (ich)", localOs(), NodeType.SELF);
            } catch (Exception ignored) {}

            for (ScanHistory.Entry e : ScanHistory.getInstance().getAll())
                for (ScanResult r : e.results)
                    if (seen.add(r.getIp()))
                        addNode(r.getIp(), r.getHostname(), r.getOsGuess(), NodeType.HOST);

            for (HostResult h : NetworkStore.getInstance().getAllHosts())
                if (seen.add(h.ip))
                    addNode(h.ip, cleanHostname(h.hostname), h.os, NodeType.HOST);

            return gwNode;
        }

        Node addNode(String ip, String hn, String os, NodeType type) {
            Node n = new Node(ip, hn, os, type);
            nodes.add(n);
            return n;
        }

        void resetLayout() {
            camX = 0; camY = 0; scale = 1.0;
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            MapLayout.apply(nodes, edges, w, h);
            repaint();
        }

        private void updateLabels() {
            int cnt = hostCount();
            if (titleLabel != null)
                SwingUtilities.invokeLater(() ->
                        titleLabel.setText("  Netzwerk-Karte  –  " + cnt + " Hosts"));
            setStatus("  " + cnt + " Hosts  |  Switch-IP oben eingeben oder Rechtsklick");
        }

        // ── Paint ─────────────────────────────────────────────────────────

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            applyCamera(g2);
            MapRenderer.drawBackground(g2, bg, getWidth(), getHeight());
            edges.forEach(e -> MapRenderer.drawEdge(g2, e));
            nodes.forEach(n -> MapRenderer.drawNode(g2, n));
            g2.dispose();
        }

        private void applyCamera(Graphics2D g2) {
            int sw = getWidth(), sh = getHeight();
            g2.translate(sw / 2.0, sh / 2.0);
            g2.scale(scale, scale);
            g2.translate(-sw / 2.0 + camX, -sh / 2.0 + camY);
        }

        // ── Mouse ─────────────────────────────────────────────────────────

        private void installMouse() {
            addMouseWheelListener(e -> {
                double f = e.getWheelRotation() < 0 ? 1.12 : 0.9;
                scale = Math.max(0.15, Math.min(5.0, scale * f));
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { handlePress(e); }
                @Override public void mouseReleased(MouseEvent e) {
                    dragNode = null; panning = false; setCursor(Cursor.getDefaultCursor());
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
                        Optional.ofNullable(nodeAt(toWorld(e.getPoint()))).ifPresent(
                                n -> HostDetailsPanel.show(n.ip, n.hostname, n.os,
                                        NetworkStore.getInstance().findNetwork(n.ip)));
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) { handleDrag(e); }
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(nodeAt(toWorld(e.getPoint())) != null
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        private void handlePress(MouseEvent e) {
            Point world = toWorld(e.getPoint());
            if (SwingUtilities.isLeftMouseButton(e)) {
                dragNode = nodeAt(world);
                if (dragNode != null) {
                    dragStart = e.getPoint();
                    dragOrigX = dragNode.x; dragOrigY = dragNode.y;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            } else if (SwingUtilities.isRightMouseButton(e)) {
                Node hit = nodeAt(world);
                if (hit != null && hit.type != NodeType.GATEWAY && hit.type != NodeType.SELF)
                    showContextMenu(hit, e.getComponent(), e.getX(), e.getY());
                else { panning = true; panStart = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
            }
        }

        private void handleDrag(MouseEvent e) {
            if (dragNode != null) {
                dragNode.x = (int)(dragOrigX + (e.getX() - dragStart.x) / scale);
                dragNode.y = (int)(dragOrigY + (e.getY() - dragStart.y) / scale);
                repaint();
            } else if (panning && panStart != null) {
                camX += (e.getX() - panStart.x) / scale;
                camY += (e.getY() - panStart.y) / scale;
                panStart = e.getPoint(); repaint();
            }
        }

        private void showContextMenu(Node n, Component comp, int x, int y) {
            boolean isSw = n.type == NodeType.SWITCH;
            Color   menuBg = new Color(0x0F, 0x13, 0x10);
            Color   menuHov = new Color(0x1A, 0x22, 0x1A);

            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(menuBg);
            menu.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

            JMenuItem hdr = menuItem(n.ip + "  " + clip(n.hostname, 20), FG_DIM, menuBg, menuHov);
            hdr.setEnabled(false);
            menu.add(hdr);
            menu.addSeparator();

            JMenuItem swItem = menuItem(
                    isSw ? "✕  Kein Switch (zurücksetzen)" : "S  Als Switch/Hub markieren",
                    isSw ? WARN : new Color(0xFF, 0xA0, 0x30), menuBg, menuHov);
            swItem.addActionListener(e -> {
                if (isSw) MapSwitchStore.remove(n.ip);
                else      MapSwitchStore.add(n.ip);
                reload();
            });
            menu.add(swItem);

            JMenuItem det = menuItem("🔍  Details", ACCENT, menuBg, menuHov);
            det.addActionListener(e ->
                    HostDetailsPanel.show(n.ip, n.hostname, n.os,
                            NetworkStore.getInstance().findNetwork(n.ip)));
            menu.add(det);
            menu.show(comp, x, y);
        }

        private Point toWorld(Point s) {
            int sw = getWidth(), sh = getHeight();
            return new Point(
                    (int)((s.x - sw / 2.0) / scale + sw / 2.0 - camX),
                    (int)((s.y - sh / 2.0) / scale + sh / 2.0 - camY));
        }

        Node nodeAt(Point w) {
            return nodes.stream()
                    .filter(n -> Math.hypot(n.x - w.x, n.y - w.y) < 28)
                    .findFirst().orElse(null);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String cleanHostname(String h) {
        if (h == null) return "";
        int i = h.indexOf(" [");
        return i < 0 ? h : h.substring(0, i).trim();
    }

    private static String localOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "Windows";
        if (os.contains("mac")) return "macOS";
        return "Linux/Unix";
    }

    private static String clip(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 8, 4, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JMenuItem menuItem(String text, Color fg, Color bg, Color hov) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        item.setForeground(fg); item.setBackground(bg);
        item.setBorder(new EmptyBorder(6, 14, 6, 20)); item.setOpaque(true);
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(hov); }
            public void mouseExited (MouseEvent e) { item.setBackground(bg);  }
        });
        return item;
    }

    // ── Data types ────────────────────────────────────────────────────────

    enum NodeType  { GATEWAY, SELF, SWITCH, HOST }
    enum EdgeType  { NORMAL, UPLINK, SELF_LINK }

    static class Node {
        String ip, hostname, os;
        NodeType type;
        int x, y;
        Node(String ip, String hn, String os, NodeType t) {
            this.ip = ip; hostname = hn; this.os = os; type = t;
        }
    }

    static class Edge {
        final Node from, to;
        final EdgeType type;
        Edge(Node f, Node t, EdgeType type) { from = f; to = t; this.type = type; }
    }
}