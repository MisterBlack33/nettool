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
 * Netzwerk-Topologie-Karte mit mehrstufiger Topology-Inferenz.
 *
 * Switch-Erkennung (Priorität):
 *  1. Manuell markiert per Rechtsklick (Session-persistent)
 *  2. OS/Hostname enthält switch/hub/router/fritz/unifi/mikrotik...
 *  3. MAC-OUI bekannter Switch-/Router-Hersteller
 *  4. Dichtester Knoten im /24-Subnetz (meiste Nachbarn im gleichen Segment)
 *
 * Routing-Logik:
 *  Hosts → nächster Switch im gleichen /24 → Gateway
 *  Kein Switch gefunden → direkt ans Gateway
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    /** Session-persistent manuell markierte Switch-IPs. */
    private static final Set<String> MANUAL_SWITCHES = Collections.synchronizedSet(new HashSet<>());

    /** MAC-OUI-Präfixe bekannter Switch/Router-Hersteller (erste 8 Zeichen: XX:XX:XX). */
    private static final Set<String> SWITCH_OUIS = Set.of(
            "00:00:0C","00:1A:A1","00:1B:54","00:1C:57","00:1D:70","00:1E:BD",
            "00:1F:CA","00:21:A0","00:22:90","00:23:AC","00:24:14","00:25:84",
            "00:26:CB","00:90:BF","C8:9C:1D","D0:72:DC","00:14:6A","00:1C:10",
            "00:60:2F","00:E0:B1","08:00:07","A0:E0:AF","CC:46:D6","E8:40:F2",
            "FC:FB:FB","00:1A:2F","00:24:B2","10:DA:43","14:91:82","18:59:36",
            "50:C7:BF","AC:84:C9","C4:6E:1F","F8:1A:67","00:09:5B","00:0F:B5",
            "00:14:6C","00:18:4D","20:E5:2A","44:94:FC","60:38:E0","A0:40:A0",
            "B0:7F:B9","C0:3F:0E","C4:04:15","E0:91:F5"
    );

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

        JPanel root = new JPanel(new BorderLayout());
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
                {"*", "Ich",        ACCENT2},
                {"G", "Gateway",    NET_COL},
                {"S", "Switch/Hub", new Color(0xFF, 0xA0, 0x30)},
                {"W", "Windows",    WIN_COL},
                {"L", "Linux",      LIN_COL},
                {"M", "macOS",      APL_COL},
                {"A", "Android",    AND_COL},
                {"?", "Unbekannt",  FG_DIM},
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
        JLabel hint = new JLabel("  Rechtsklick = als Switch markieren");
        hint.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        hint.setForeground(FG_DIM);
        panel.add(hint);
        panel.setPreferredSize(new Dimension(200, items.length * 18 + 28));
        return panel;
    }

    // ── Background scan ───────────────────────────────────────────────────

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
                    statusLbl.setText("  " + canvas.hostCount() + " Hosts  |  Rechtsklick auf Knoten = als Switch markieren");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLbl.setText("  Fehler: " + e.getMessage()));
            }
        }, "MapBgScan").start();
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    static class MapCanvas extends JPanel {

        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Color      bg;
        private JLabel           titleLabel;

        private double scale = 1.0, camX = 0, camY = 0;
        private Node  dragNode;
        private Point dragNodeStart;
        private int   dragNodeOrigX, dragNodeOrigY;
        private boolean panning;
        private Point   panStart;

        MapCanvas(Color bg) {
            this.bg = bg;
            setBackground(bg);
            reload();
            installMouse();
        }

        void setTitleLabel(JLabel lbl) { this.titleLabel = lbl; }

        int hostCount() {
            return (int) nodes.stream().filter(n -> n.type == NodeType.HOST).count();
        }

        // ── Build topology ────────────────────────────────────────────────

        void reload() {
            nodes.clear();
            edges.clear();
            Set<String> seen = new LinkedHashSet<>();

            String gw = RemoteNetScanner.detectDefaultGateway();
            Node gwNode = null;
            if (gw != null && seen.add(gw))
                gwNode = addNode(gw, "Gateway", "Router / Netzwerkgeraet", NodeType.GATEWAY);

            Node selfNode = null;
            try {
                InetAddress self = InetAddress.getLocalHost();
                if (seen.add(self.getHostAddress()))
                    selfNode = addNode(self.getHostAddress(), self.getHostName() + " (ich)", localOs(), NodeType.SELF);
            } catch (Exception ignored) {}

            for (ScanHistory.Entry entry : ScanHistory.getInstance().getAll())
                for (ScanResult r : entry.results)
                    if (seen.add(r.getIp()))
                        addNode(r.getIp(), r.getHostname(), r.getOsGuess(), NodeType.HOST);

            for (HostResult h : NetworkStore.getInstance().getAllHosts())
                if (seen.add(h.ip))
                    addNode(h.ip, cleanHostname(h.hostname), h.os, NodeType.HOST);

            promoteSwitchNodes();
            buildTopologyEdges(gwNode, selfNode);
            resetLayout();

            if (titleLabel != null)
                SwingUtilities.invokeLater(() ->
                        titleLabel.setText("  Netzwerk-Karte  -  " + hostCount() + " Hosts"));
        }

        private Node addNode(String ip, String hn, String os, NodeType type) {
            Node n = new Node(ip, hn, os, type);
            nodes.add(n);
            return n;
        }

        /**
         * Switch-Erkennung (Priorität absteigend):
         *  1. Manuell per Rechtsklick markiert
         *  2. OS/Hostname-Keywords
         *  3. MAC-OUI bekannter Switch-Hersteller
         *  4. Dichtester Knoten im /24 (Fallback)
         */
        private void promoteSwitchNodes() {
            // Pass 1: Manual + keyword
            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;
                if (MANUAL_SWITCHES.contains(n.ip)) { n.type = NodeType.SWITCH; continue; }
                if (isSwitchByKeyword(n)) n.type = NodeType.SWITCH;
            }

            // Pass 2: OUI check for remaining hosts
            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;
                if (isSwitchByOui(n)) n.type = NodeType.SWITCH;
            }

            // Pass 3: Density fallback — if no switches detected in a /24,
            // promote the host with the lowest last-octet (likely .1/.2 = router/switch)
            Set<String> subnets = nodes.stream()
                    .filter(n -> n.type == NodeType.HOST || n.type == NodeType.SWITCH)
                    .map(n -> subnet24(n.ip))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String subnet : subnets) {
                boolean hasSwitchInSubnet = nodes.stream()
                        .anyMatch(n -> n.type == NodeType.SWITCH && subnet.equals(subnet24(n.ip)));
                if (hasSwitchInSubnet) continue;

                // Count peers per host in this subnet
                List<Node> peers = nodes.stream()
                        .filter(n -> subnet.equals(subnet24(n.ip)) && n.type == NodeType.HOST)
                        .collect(Collectors.toList());
                if (peers.size() < 3) continue; // not enough hosts to infer a switch

                // Promote the host with lowest last octet as likely switch/router
                peers.stream()
                        .min(Comparator.comparingInt(n -> lastOctet(n.ip)))
                        .ifPresent(n -> n.type = NodeType.SWITCH);
            }
        }

        private static boolean isSwitchByKeyword(Node n) {
            String os = n.os != null ? n.os.toLowerCase() : "";
            String hn = n.hostname != null ? n.hostname.toLowerCase() : "";
            return os.contains("switch") || os.contains("hub") || os.contains("router")
                    || os.contains("fritz") || os.contains("netzwerk") || os.contains("unifi")
                    || os.contains("mikrotik") || os.contains("cisco") || os.contains("netgear")
                    || os.contains("tp-link") || os.contains("access point")
                    || hn.contains("switch") || hn.contains("hub") || hn.contains("router")
                    || hn.contains("sw-") || hn.contains("-sw") || hn.contains("sg-")
                    || hn.contains("gs-") || hn.contains("fritz") || hn.contains("unifi")
                    || hn.contains("mikrotik") || hn.contains("ap-");
        }

        private static boolean isSwitchByOui(Node n) {
            if (n.hostname == null) return false;
            // Extract MAC from hostname if embedded like "host [AA:BB:CC:DD:EE:FF]"
            int s = n.hostname.indexOf('['), e = n.hostname.indexOf(']');
            if (s < 0 || e <= s) return false;
            String mac = n.hostname.substring(s + 1, e).trim();
            if (mac.length() < 8) return false;
            String oui = mac.substring(0, 8).toUpperCase().replace("-", ":");
            return SWITCH_OUIS.contains(oui);
        }

        private void buildTopologyEdges(Node gwNode, Node selfNode) {
            if (gwNode == null) return;

            List<Node> switches = nodes.stream()
                    .filter(n -> n.type == NodeType.SWITCH)
                    .collect(Collectors.toList());

            for (Node sw : switches)
                edges.add(new Edge(sw, gwNode, EdgeType.UPLINK));

            if (selfNode != null)
                edges.add(new Edge(selfNode, gwNode, EdgeType.SELF_LINK));

            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;
                Node via = findNearestSwitch(n, switches);
                edges.add(new Edge(n, via != null ? via : gwNode, EdgeType.NORMAL));
            }
        }

        /**
         * Finds the nearest switch in the same /24 subnet.
         * Prefers lower last-octet distance (closer IP = more likely direct uplink).
         */
        private static Node findNearestSwitch(Node host, List<Node> switches) {
            String hostSubnet = subnet24(host.ip);
            if (hostSubnet == null) return null;

            return switches.stream()
                    .filter(sw -> hostSubnet.equals(subnet24(sw.ip)))
                    .min(Comparator.comparingInt(sw ->
                            Math.abs(lastOctet(sw.ip) - lastOctet(host.ip))))
                    .orElse(null);
        }

        // ── Layout ────────────────────────────────────────────────────────

        void resetLayout() {
            camX = 0; camY = 0; scale = 1.0;
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            int cx = w / 2, cy = h / 2;

            nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst()
                    .ifPresent(n -> { n.x = cx; n.y = cy; });
            nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst()
                    .ifPresent(n -> { n.x = cx - 130; n.y = cy - 100; });

            List<Node> switches = nodes.stream().filter(n -> n.type == NodeType.SWITCH).toList();
            if (!switches.isEmpty()) {
                int swRadius = Math.min(cx, cy) - 100;
                double step = 2 * Math.PI / Math.max(switches.size(), 1);
                for (int i = 0; i < switches.size(); i++) {
                    double angle = i * step - Math.PI / 2;
                    switches.get(i).x = (int)(cx + swRadius * Math.cos(angle));
                    switches.get(i).y = (int)(cy + swRadius * Math.sin(angle));
                }
            }

            placeHostsNearParents(cx, cy);
            repaint();
        }

        private void placeHostsNearParents(int cx, int cy) {
            Map<Node, List<Node>> groups = new LinkedHashMap<>();
            for (Edge e : edges)
                if (e.from.type == NodeType.HOST)
                    groups.computeIfAbsent(e.to, k -> new ArrayList<>()).add(e.from);

            for (Map.Entry<Node, List<Node>> entry : groups.entrySet()) {
                Node parent = entry.getKey();
                List<Node> children = entry.getValue();
                int n = children.size();
                if (n == 0) continue;
                int radius = 80 + n * 8;
                double step = 2 * Math.PI / n;
                double baseAngle = Math.atan2(parent.y - cy, parent.x - cx);
                for (int i = 0; i < n; i++) {
                    double angle = baseAngle + (i - n / 2.0) * step;
                    children.get(i).x = parent.x + (int)(radius * Math.cos(angle));
                    children.get(i).y = parent.y + (int)(radius * Math.sin(angle));
                }
            }
        }

        // ── Paint ─────────────────────────────────────────────────────────

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
            nodes.forEach(n -> drawNode(g2, n));
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
                    case SELF_LINK -> { g2.setStroke(new BasicStroke(2f)); g2.setColor(ACCENT2); }
                    case UPLINK    -> { g2.setStroke(new BasicStroke(1.5f)); g2.setColor(new Color(0xFF, 0xA0, 0x30, 180)); }
                    default        -> {
                        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
                        g2.setColor(GuiTheme.isDark() ? new Color(0x30, 0x45, 0x30) : new Color(0xC0, 0xBC, 0xB4));
                    }
                }
                g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);
            }
        }

        private void drawNode(Graphics2D g2, Node n) {
            Color col = nodeColor(n);
            int r = switch (n.type) {
                case GATEWAY, SWITCH -> 20;
                case SELF            -> 18;
                default              -> 14;
            };
            // Glow
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
            g2.fillOval(n.x - r - 6, n.y - r - 6, (r + 6) * 2, (r + 6) * 2);
            // Fill
            g2.setColor(GuiTheme.isDark() ? new Color(0x08, 0x0C, 0x08) : new Color(0xF4, 0xF2, 0xEE));
            g2.fillOval(n.x - r, n.y - r, r * 2, r * 2);
            // Border
            g2.setColor(col);
            g2.setStroke(new BasicStroke(n.type == NodeType.SWITCH ? 2.5f : 1.5f));
            g2.drawOval(n.x - r, n.y - r, r * 2, r * 2);
            if (n.type == NodeType.SWITCH) {
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(n.x - r + 4, n.y - r + 4, (r - 4) * 2, (r - 4) * 2);
            }
            // Icon
            String icon = switch (n.type) {
                case GATEWAY -> "G"; case SELF -> "*"; case SWITCH -> "S";
                default -> osIcon(n.os);
            };
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 12 : 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1f));
            g2.drawString(icon, n.x - fm.stringWidth(icon) / 2, n.y + fm.getAscent() / 2 - 1);
            // Labels
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
                        Node hit = nodeAt(world);
                        if (hit != null && hit.type != NodeType.GATEWAY && hit.type != NodeType.SELF)
                            showNodeContextMenu(hit, e.getComponent(), e.getX(), e.getY());
                        else { panning = true; panStart = e.getPoint();
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
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

        private void showNodeContextMenu(Node n, Component comp, int x, int y) {
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(new Color(0x10, 0x14, 0x10));
            menu.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

            boolean isSwitch = n.type == NodeType.SWITCH;
            String label = isSwitch ? "✕  Als normalen Host markieren" : "S  Als Switch/Hub markieren";
            Color  fg    = isSwitch ? WARN : new Color(0xFF, 0xA0, 0x30);

            JMenuItem item = new JMenuItem(label);
            item.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
            item.setForeground(fg);
            item.setBackground(new Color(0x10, 0x14, 0x10));
            item.setBorder(new EmptyBorder(6, 14, 6, 20));
            item.setOpaque(true);
            item.addActionListener(ev -> {
                if (isSwitch) {
                    MANUAL_SWITCHES.remove(n.ip);
                    n.type = NodeType.HOST;
                } else {
                    MANUAL_SWITCHES.add(n.ip);
                    n.type = NodeType.SWITCH;
                }
                reload();
            });
            menu.add(item);

            JMenuItem detailItem = new JMenuItem("🔍  Details anzeigen");
            detailItem.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
            detailItem.setForeground(ACCENT);
            detailItem.setBackground(new Color(0x10, 0x14, 0x10));
            detailItem.setBorder(new EmptyBorder(6, 14, 6, 20));
            detailItem.setOpaque(true);
            detailItem.addActionListener(ev ->
                    HostDetailsPanel.show(n.ip, n.hostname, n.os,
                            NetworkStore.getInstance().findNetwork(n.ip)));
            menu.add(detailItem);

            menu.show(comp, x, y);
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

    private static String subnet24(String ip) {
        if (ip == null) return null;
        int last = ip.lastIndexOf('.');
        return last > 0 ? ip.substring(0, last) : null;
    }

    private static int lastOctet(String ip) {
        if (ip == null) return 999;
        int last = ip.lastIndexOf('.');
        try { return Integer.parseInt(ip.substring(last + 1)); }
        catch (NumberFormatException e) { return 999; }
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
        if (l.contains("windows"))                       return "W";
        if (l.contains("linux") || l.contains("unix"))  return "L";
        if (l.contains("mac")   || l.contains("ios"))   return "M";
        if (l.contains("android"))                      return "A";
        if (l.contains("router") || l.contains("fritz")
                || l.contains("switch"))                return "R";
        if (l.contains("drucker") || l.contains("printer")) return "P";
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
        Edge(Node from, Node to, EdgeType type) { this.from = from; this.to = to; this.type = type; }
    }
}