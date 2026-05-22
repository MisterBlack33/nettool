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
import java.util.stream.Collectors;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Netzwerk-Topologie-Karte mit Topology-Inference.
 *
 * Erkennt Switches/Router als Zwischenknoten und zeigt Hosts
 * nicht direkt am Gateway, sondern über ihren nächsten Layer-2-Knoten.
 *
 * Topology-Inferenz:
 *  1. Hosts mit OS "switch"/"router"/"netzwerk" = potentielle Zwischenknoten
 *  2. Hosts im gleichen /28-Segment wie ein Zwischenknoten → über diesen routen
 *  3. Alle anderen → direkt am Gateway
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
        MapCanvas canvas = new MapCanvas(bg);
        JLabel statusLbl = buildStatusLabel();
        JPanel toolbar   = buildToolbar(canvas, statusLbl);

        JLayeredPane layered = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                Component legend = getComponentsInLayer(JLayeredPane.PALETTE_LAYER)[0];
                Dimension ls = legend.getPreferredSize();
                legend.setBounds(10, h - ls.height - 10, ls.width, ls.height);
            }
        };
        layered.setBackground(bg);
        layered.setOpaque(true);
        layered.add(canvas, JLayeredPane.DEFAULT_LAYER);
        layered.add(buildLegendOverlay(), JLayeredPane.PALETTE_LAYER);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(bg);
        root.add(toolbar,   BorderLayout.NORTH);
        root.add(layered,   BorderLayout.CENTER);
        root.add(statusLbl, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
        startBackgroundScan(canvas, statusLbl);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

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

        refreshBtn.addActionListener(e -> canvas.reload());
        layoutBtn.addActionListener(e -> canvas.resetLayout());
        return bar;
    }

    private static JLabel buildStatusLabel() {
        JLabel lbl = new JLabel("  Starte Scan...");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    // ── Legende ───────────────────────────────────────────────────────────

    private static JPanel buildLegendOverlay() {
        Color legBg = new Color(0x08, 0x0C, 0x08, 200);
        Object[][] items = {
                {"*", "Ich",       ACCENT2},
                {"G", "Gateway",   NET_COL},
                {"S", "Switch/Hub", new Color(0xFF, 0xA0, 0x30)},
                {"W", "Windows",   WIN_COL},
                {"L", "Linux",     LIN_COL},
                {"M", "macOS",     APL_COL},
                {"A", "Android",   AND_COL},
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
        for (Object[] row : items) {
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
        panel.setPreferredSize(new Dimension(120, items.length * 18 + 14));
        return panel;
    }

    // ── Hintergrund-Scan ──────────────────────────────────────────────────

    private static void startBackgroundScan(MapCanvas canvas, JLabel statusLbl) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> statusLbl.setText("  Scanne Netzwerk..."));
                List<String> subnets = SubnetDetector.getAllSubnets();
                if (subnets.isEmpty()) {
                    SwingUtilities.invokeLater(() -> statusLbl.setText("  Kein Subnetz gefunden."));
                    return;
                }
                NetworkHostScanner.scan(subnets);
                SwingUtilities.invokeLater(() -> {
                    canvas.reload();
                    statusLbl.setText("  " + canvas.hostCount() + " Hosts gefunden.");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLbl.setText("  Fehler: " + e.getMessage()));
            }
        }, "MapBgScan").start();
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    static class MapCanvas extends JPanel {

        private final List<Node>     nodes = new ArrayList<>();
        private final List<Edge>     edges = new ArrayList<>();
        private final Color          bg;
        private JLabel               titleLabel;

        private double scale  = 1.0;
        private double camX   = 0;
        private double camY   = 0;

        private Node  dragNode;
        private Point dragNodeStart;
        private int   dragNodeOrigX, dragNodeOrigY;
        private boolean panning;
        private Point   panStart;

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

        // ── Topology build ────────────────────────────────────────────────

        void reload() {
            nodes.clear();
            edges.clear();
            Set<String> seen = new LinkedHashSet<>();

            // Gateway
            String gw = RemoteNetScanner.detectDefaultGateway();
            Node gwNode = null;
            if (gw != null && seen.add(gw)) {
                gwNode = new Node(gw, "Gateway", "Router / Netzwerkgeraet", NodeType.GATEWAY);
                nodes.add(gwNode);
            }

            // Self
            Node selfNode = null;
            try {
                InetAddress self = InetAddress.getLocalHost();
                if (seen.add(self.getHostAddress())) {
                    selfNode = new Node(self.getHostAddress(), self.getHostName() + " (ich)", localOs(), NodeType.SELF);
                    nodes.add(selfNode);
                }
            } catch (Exception ignored) {}

            // Collect all hosts
            for (ScanHistory.Entry entry : ScanHistory.getInstance().getAll())
                for (ScanResult r : entry.results)
                    if (seen.add(r.getIp()))
                        nodes.add(new Node(r.getIp(), r.getHostname(), r.getOsGuess(), NodeType.HOST));

            for (HostResult h : NetworkStore.getInstance().getAllHosts())
                if (seen.add(h.ip))
                    nodes.add(new Node(h.ip, cleanHostname(h.hostname), h.os, NodeType.HOST));

            // Promote switch/hub candidates to SWITCH type
            promoteSwitchNodes();

            // Build topology edges
            buildTopologyEdges(gwNode, selfNode);

            resetLayout();
            if (titleLabel != null)
                SwingUtilities.invokeLater(() ->
                        titleLabel.setText("  Netzwerk-Karte  -  " + hostCount() + " Hosts"));
        }

        /**
         * Promote HOST nodes to SWITCH type if their OS indicates
         * a network device (switch, hub, router secondary, etc.).
         */
        private void promoteSwitchNodes() {
            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;
                String osL = n.os != null ? n.os.toLowerCase() : "";
                String hnL = n.hostname != null ? n.hostname.toLowerCase() : "";
                if (osL.contains("switch") || osL.contains("hub")
                        || hnL.contains("switch") || hnL.contains("hub")
                        || hnL.contains("sw-") || hnL.contains("-sw")
                        || hnL.contains("sg-") || hnL.contains("gs-")) {
                    n.type = NodeType.SWITCH;
                }
            }
        }

        /**
         * Topology inference:
         *  1. For each SWITCH node → edge to gateway
         *  2. For each HOST → find nearest switch in same /28 block
         *     If found → edge HOST→SWITCH
         *     Else     → edge HOST→GATEWAY
         *  3. SELF → gateway
         */
        private void buildTopologyEdges(Node gwNode, Node selfNode) {
            if (gwNode == null) return;

            List<Node> switches = nodes.stream()
                    .filter(n -> n.type == NodeType.SWITCH)
                    .collect(Collectors.toList());

            // Switches → gateway
            for (Node sw : switches)
                edges.add(new Edge(sw, gwNode, EdgeType.UPLINK));

            // Self → gateway
            if (selfNode != null)
                edges.add(new Edge(selfNode, gwNode, EdgeType.SELF_LINK));

            // Hosts → switch or gateway
            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;
                Node via = findSwitchForHost(n, switches);
                if (via != null)
                    edges.add(new Edge(n, via, EdgeType.NORMAL));
                else
                    edges.add(new Edge(n, gwNode, EdgeType.NORMAL));
            }
        }

        /**
         * Finds the most likely switch for a host by checking if they
         * share the same /28 subnet (last octet block of 16).
         */
        private Node findSwitchForHost(Node host, List<Node> switches) {
            int[] hostOcts = parseIp(host.ip);
            if (hostOcts == null) return null;

            Node best = null;
            int  bestDistance = Integer.MAX_VALUE;

            for (Node sw : switches) {
                int[] swOcts = parseIp(sw.ip);
                if (swOcts == null) continue;

                // Must be same /24 subnet (first 3 octets match)
                if (hostOcts[0] != swOcts[0] || hostOcts[1] != swOcts[1]
                        || hostOcts[2] != swOcts[2]) continue;

                // Same /28 block (groups of 16): floor(octet/16) must match
                int hostBlock = hostOcts[3] / 16;
                int swBlock   = swOcts[3] / 16;
                if (hostBlock == swBlock) {
                    int dist = Math.abs(hostOcts[3] - swOcts[3]);
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        best = sw;
                    }
                }
            }
            return best;
        }

        // ── Layout ────────────────────────────────────────────────────────

        void resetLayout() {
            camX = 0; camY = 0; scale = 1.0;
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            int cx = w / 2, cy = h / 2;

            // Place gateway center
            nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst()
                    .ifPresent(n -> { n.x = cx; n.y = cy; });

            // Place self upper-left of gateway
            nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst()
                    .ifPresent(n -> { n.x = cx - 130; n.y = cy - 100; });

            // Place switches in a ring around gateway
            List<Node> switches = nodes.stream()
                    .filter(n -> n.type == NodeType.SWITCH).toList();
            if (!switches.isEmpty()) {
                double swStep   = 2 * Math.PI / Math.max(switches.size(), 1);
                int    swRadius = Math.min(cx, cy) - 100;
                for (int i = 0; i < switches.size(); i++) {
                    double angle = i * swStep - Math.PI / 2;
                    switches.get(i).x = (int)(cx + swRadius * Math.cos(angle));
                    switches.get(i).y = (int)(cy + swRadius * Math.sin(angle));
                }
            }

            // Place hosts near their parent (switch or gateway)
            placeHostsNearParents(cx, cy);
            repaint();
        }

        private void placeHostsNearParents(int cx, int cy) {
            // Group hosts by their parent node
            Map<Node, List<Node>> groups = new LinkedHashMap<>();
            for (Edge e : edges) {
                if (e.from.type == NodeType.HOST) {
                    groups.computeIfAbsent(e.to, k -> new ArrayList<>()).add(e.from);
                }
            }

            for (Map.Entry<Node, List<Node>> entry : groups.entrySet()) {
                Node parent = entry.getKey();
                List<Node> children = entry.getValue();
                int n = children.size();
                if (n == 0) continue;

                double step   = 2 * Math.PI / Math.max(n, 1);
                int    radius = 80 + n * 8;
                for (int i = 0; i < n; i++) {
                    double angle = i * step + offsetAngle(parent, cx, cy);
                    children.get(i).x = parent.x + (int)(radius * Math.cos(angle));
                    children.get(i).y = parent.y + (int)(radius * Math.sin(angle));
                }
            }
        }

        /** Returns angle from canvas center to node, so children spread away from center. */
        private double offsetAngle(Node n, int cx, int cy) {
            return Math.atan2(n.y - cy, n.x - cx);
        }

        // ── Rendering ─────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int sw = getWidth(), sh = getHeight();
            g2.translate(sw / 2.0, sh / 2.0);
            g2.scale(scale, scale);
            g2.translate(-sw / 2.0 + camX, -sh / 2.0 + camY);

            g2.setColor(bg);
            g2.fillRect(-10000, -10000, 30000, 30000);
            drawGrid(g2);
            drawEdges(g2);
            for (Node n : nodes) drawNode(g2, n);
            g2.dispose();
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(GuiTheme.isDark() ? new Color(0x12, 0x18, 0x12) : new Color(0xE4, 0xE2, 0xDC));
            for (int x = -3000; x < 6000; x += 60) g2.drawLine(x, -3000, x, 6000);
            for (int y = -3000; y < 6000; y += 60) g2.drawLine(-3000, y, 6000, y);
        }

        private void drawEdges(Graphics2D g2) {
            for (Edge e : edges) {
                switch (e.type) {
                    case SELF_LINK -> {
                        g2.setStroke(new BasicStroke(2f));
                        g2.setColor(ACCENT2);
                    }
                    case UPLINK -> {
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.setColor(new Color(0xFF, 0xA0, 0x30, 180));
                    }
                    default -> {
                        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
                        g2.setColor(GuiTheme.isDark()
                                ? new Color(0x30, 0x45, 0x30) : new Color(0xC0, 0xBC, 0xB4));
                    }
                }
                g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);
            }
        }

        private void drawNode(Graphics2D g2, Node n) {
            Color col = nodeColor(n);
            int r = switch (n.type) {
                case GATEWAY -> 22;
                case SELF    -> 18;
                case SWITCH  -> 18;
                default      -> 14;
            };

            // Glow
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
            g2.fillOval(n.x - r - 6, n.y - r - 6, (r + 6) * 2, (r + 6) * 2);

            // Fill
            g2.setColor(GuiTheme.isDark() ? new Color(0x08, 0x0C, 0x08) : new Color(0xF4, 0xF2, 0xEE));
            g2.fillOval(n.x - r, n.y - r, r * 2, r * 2);

            // Border (double ring for switch)
            g2.setColor(col);
            g2.setStroke(new BasicStroke(n.type == NodeType.SWITCH ? 2f : 1.5f));
            g2.drawOval(n.x - r, n.y - r, r * 2, r * 2);
            if (n.type == NodeType.SWITCH) {
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(n.x - r + 3, n.y - r + 3, (r - 3) * 2, (r - 3) * 2);
            }

            // Icon
            String icon = switch (n.type) {
                case GATEWAY -> "G";
                case SELF    -> "*";
                case SWITCH  -> "S";
                default      -> osIcon(n.os);
            };
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 12 : 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1f));
            g2.drawString(icon, n.x - fm.stringWidth(icon) / 2, n.y + fm.getAscent() / 2 - 1);

            // IP label
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            g2.setColor(GuiTheme.isDark() ? new Color(0xA0, 0x9C, 0x90) : new Color(0x40, 0x42, 0x3E));
            g2.drawString(n.ip, n.x - fm.stringWidth(n.ip) / 2, n.y + r + 14);

            if (n.hostname != null && !n.hostname.equals(n.ip)) {
                String hn = n.hostname.length() > 18 ? n.hostname.substring(0, 17) + "." : n.hostname;
                g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                g2.setColor(n.type == NodeType.SELF ? ACCENT2 : FG_DIM);
                g2.drawString(hn, n.x - fm.stringWidth(hn) / 2, n.y + r + 24);
            }
        }

        // ── Mouse ─────────────────────────────────────────────────────────

        private void installMouse() {
            addMouseWheelListener(e -> {
                double factor = e.getWheelRotation() < 0 ? 1.12 : 0.9;
                scale = Math.max(0.15, Math.min(5.0, scale * factor));
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    Point world = toWorld(e.getPoint());
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        dragNode = nodeAt(world);
                        if (dragNode != null) {
                            dragNodeStart = e.getPoint();
                            dragNodeOrigX = dragNode.x;
                            dragNodeOrigY = dragNode.y;
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        panning  = true;
                        panStart = e.getPoint();
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragNode = null; panning = false;
                    setCursor(Cursor.getDefaultCursor());
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                    Node hit = nodeAt(toWorld(e.getPoint()));
                    if (hit != null)
                        HostDetailsPanel.show(hit.ip, hit.hostname, hit.os,
                                NetworkStore.getInstance().findNetwork(hit.ip));
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragNode != null) {
                        double dx = (e.getX() - dragNodeStart.x) / scale;
                        double dy = (e.getY() - dragNodeStart.y) / scale;
                        dragNode.x = (int)(dragNodeOrigX + dx);
                        dragNode.y = (int)(dragNodeOrigY + dy);
                        repaint();
                    } else if (panning && panStart != null) {
                        camX += (e.getX() - panStart.x) / scale;
                        camY += (e.getY() - panStart.y) / scale;
                        panStart = e.getPoint();
                        repaint();
                    }
                }
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(nodeAt(toWorld(e.getPoint())) != null
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        private Point toWorld(Point screen) {
            int sw = getWidth(), sh = getHeight();
            double wx = (screen.x - sw / 2.0) / scale + sw / 2.0 - camX;
            double wy = (screen.y - sh / 2.0) / scale + sh / 2.0 - camY;
            return new Point((int) wx, (int) wy);
        }

        private Node nodeAt(Point world) {
            return nodes.stream()
                    .filter(n -> Math.hypot(n.x - world.x, n.y - world.y) < 28)
                    .findFirst().orElse(null);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int[] parseIp(String ip) {
        if (ip == null) return null;
        String[] p = ip.split("\\.");
        if (p.length != 4) return null;
        try {
            return new int[]{
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]), Integer.parseInt(p[3])
            };
        } catch (NumberFormatException e) { return null; }
    }

    private static Color nodeColor(Node n) {
        return switch (n.type) {
            case SELF    -> ACCENT2;
            case GATEWAY -> NET_COL;
            case SWITCH  -> new Color(0xFF, 0xA0, 0x30);
            default      -> osColor(n.os);
        };
    }

    private static String osIcon(String os) {
        if (os == null) return "?";
        String l = os.toLowerCase();
        if (l.contains("windows"))                          return "W";
        if (l.contains("linux") || l.contains("unix"))     return "L";
        if (l.contains("mac")   || l.contains("ios"))      return "M";
        if (l.contains("android"))                         return "A";
        if (l.contains("router") || l.contains("fritz")
                || l.contains("switch"))                   return "R";
        if (l.contains("drucker") || l.contains("printer"))return "P";
        return "?";
    }

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

    // ── Data types ────────────────────────────────────────────────────────

    private enum NodeType { GATEWAY, SELF, SWITCH, HOST }
    private enum EdgeType { NORMAL, UPLINK, SELF_LINK }

    private static class Node {
        String ip, hostname, os;
        NodeType type;
        int x, y;

        Node(String ip, String hn, String os, NodeType t) {
            this.ip = ip; hostname = hn; this.os = os; type = t;
        }
    }

    private static class Edge {
        final Node from, to;
        final EdgeType type;

        Edge(Node from, Node to, EdgeType type) {
            this.from = from; this.to = to; this.type = type;
        }
    }
}