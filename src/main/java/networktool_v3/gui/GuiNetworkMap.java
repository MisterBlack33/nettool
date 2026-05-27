package main.java.networktool_v3.gui;

import main.java.networktool_v3.logic.scan.NetworkHostScanner;
import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.logic.scan.ScanHistory;
import main.java.networktool_v3.logic.scan.SubnetDetector;
import main.java.networktool_v3.logic.analysis.TracerouteRunner;
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
 * Netzwerk-Topologie-Karte.
 *
 * Topologie-Erkennung (Reihenfolge):
 *  1. Traceroute (HOP_PARENT): direkter Upstream-Knoten per Traceroute ermittelt
 *     → zuverlässigste Methode, läuft asynchron im Hintergrund
 *  2. Manuell per Rechtsklick gesetzt (MANUAL_SWITCHES, session-persistent)
 *  3. OS/Hostname Switch-Keywords
 *  4. MAC-OUI bekannter Switch-Hersteller
 *  5. Ports: SNMP(161)/Telnet(23) ohne PC/Drucker-Ports
 *
 *  isEndDevice() verhindert Fehlklassifikation von PCs/Handys/Druckern in Pass 3-5.
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    /** Persistente manuell markierte Switch-IPs — werden in txt/mapSwitches.json gespeichert. */
    static final Set<String> MANUAL_SWITCHES =
            Collections.synchronizedSet(new HashSet<>());

    private static final String SWITCHES_FILE = "mapSwitches.json";

    static {
        loadManualSwitches();
    }

    static void loadManualSwitches() {
        try {
            java.nio.file.Path file = main.java.networktool_v3.storage.StorageUtils
                    .resolveTxtDir().resolve(SWITCHES_FILE);
            if (!java.nio.file.Files.exists(file)) return;
            String json = java.nio.file.Files.readString(file,
                    java.nio.charset.StandardCharsets.UTF_8);
            // parse ["ip1","ip2",...]
            json = json.trim();
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]"))   json = json.substring(0, json.length() - 1);
            for (String part : json.split(",")) {
                String ip = part.trim().replaceAll("^\"|\"$", "");
                if (!ip.isBlank()) MANUAL_SWITCHES.add(ip);
            }
        } catch (Exception ignored) {}
    }

    static void saveManualSwitches() {
        try {
            java.nio.file.Path dir  = main.java.networktool_v3.storage.StorageUtils.resolveTxtDir();
            java.nio.file.Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder("[");
            List<String> sorted = new ArrayList<>(MANUAL_SWITCHES);
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(sorted.get(i)).append("\"");
            }
            sb.append("]");
            java.nio.file.Files.writeString(dir.resolve(SWITCHES_FILE), sb.toString(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    /**
     * Hop-Topologie-Cache: IP → IP des vorherigen Hop (Layer-3-Next-Hop zum Gateway).
     * Wird im Hintergrund per Traceroute befüllt.
     * Schlüssel = Host-IP, Wert = IP des direkten Upstream-Knotens (Switch oder Gateway).
     */
    static final Map<String, String> HOP_PARENT =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * OUI-Präfixe bekannter Switch/Router-Hersteller.
     * Format: "XX:XX:XX" (erste 3 Bytes, Großbuchstaben, Doppelpunkt-getrennt).
     * Enthält HP/Aruba ProCurve, Cisco, Netgear, TP-Link, Ubiquiti.
     */
    private static final Set<String> SWITCH_OUIS = Set.of(
            "00:00:0C","00:1A:A1","00:1B:54","00:1C:57","00:1D:70","00:1E:BD",
            "00:1F:CA","00:21:A0","00:22:90","00:23:AC","00:24:14","00:25:84",
            "00:26:CB","00:90:BF","C8:9C:1D","D0:72:DC",
            "00:17:A4","00:18:71","00:1A:4B","00:1C:2E","00:1F:FE","00:21:5A",
            "00:22:64","00:23:47","00:24:81","00:25:B3","00:26:55","00:30:C1",
            "3C:D9:2B","40:B0:34","50:65:F3","5C:8A:38","64:51:06","6C:C2:17",
            "78:AC:C0","80:C1:6E","84:34:97","88:51:FB","9C:8E:99","A0:1D:48",
            "A8:97:DC","B4:39:D6","C4:34:6B","D8:C7:C8","F0:92:1C","F4:CE:46",
            "00:14:6A","00:1C:10","00:60:2F","00:E0:B1","A0:E0:AF","CC:46:D6",
            "E8:40:F2","FC:FB:FB","00:1A:2F","00:24:B2","10:DA:43","14:91:82",
            "50:C7:BF","AC:84:C9","C4:6E:1F","F8:1A:67","00:09:5B","00:0F:B5",
            "00:14:6C","00:18:4D","20:E5:2A","44:94:FC","60:38:E0","A0:40:A0",
            "B0:7F:B9","C0:3F:0E","C4:04:15","E0:91:F5"
    );

    /** OS-Strings die eindeutig kein Switch sind. */
    private static final List<String> ENDDEVICE_OS = List.of(
            "windows","android","ios","ipad","ipados","macos","apple","linux","unix",
            "raspberry","samsung","xiaomi","huawei","oppo","realme","oneplus",
            "drucker","printer","jetdirect","ipp","lpd","cups","bandwidth"
    );

    /** Hostname-Fragmente die eindeutig kein Switch sind. */
    private static final List<String> ENDDEVICE_HN = List.of(
            "desktop","laptop","phone","mobile","tablet","pad",
            "iphone","ipad","galaxy","pixel","a21","a31","a51","a52","a71","a72",
            "s20","s21","s22","s23","s24","note","redmi","poco","huawei","honor",
            "printer","drucker","epson","canon","brother","kyocera","hp-color",
            "macbook","imac","mac-mini"
    );

    /** Ports die auf PC oder Drucker hinweisen (kein Switch). */
    private static final Set<Integer> ENDDEVICE_PORTS =
            Set.of(3389, 445, 5985, 5986, 9100, 515, 631);

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
        MapCanvas canvas   = new MapCanvas(bg);
        JLabel    statusLbl = buildStatusLabel();

        JLayeredPane layered = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                Component leg = getComponentsInLayer(JLayeredPane.PALETTE_LAYER)[0];
                Dimension ls = leg.getPreferredSize();
                leg.setBounds(10, h - ls.height - 10, ls.width, ls.height);
            }
        };
        layered.setBackground(bg);
        layered.setOpaque(true);
        layered.add(canvas,           JLayeredPane.DEFAULT_LAYER);
        layered.add(buildLegend(),    JLayeredPane.PALETTE_LAYER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.add(buildToolbar(canvas), BorderLayout.NORTH);
        root.add(layered,              BorderLayout.CENTER);
        root.add(statusLbl,            BorderLayout.SOUTH);
        canvas.setStatusLabel(statusLbl);

        dlg.setContentPane(root);
        dlg.setVisible(true);
        startBackgroundScan(canvas);
    }

    private static JPanel buildToolbar(MapCanvas canvas) {
        Color barBg = GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel title = new JLabel("  Netzwerk-Karte");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        title.setForeground(ACCENT);
        canvas.setTitleLabel(title);

        // Switch-IP Eingabefeld
        JLabel swLabel = new JLabel("Switch-IP:");
        swLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        swLabel.setForeground(new Color(0xFF, 0xA0, 0x30));

        JTextField swField = new JTextField(12);
        swField.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        swField.setForeground(FG);
        swField.setBackground(GuiTheme.isDark() ? new Color(0x10, 0x16, 0x10) : Color.WHITE);
        swField.setCaretColor(ACCENT);
        swField.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFF, 0xA0, 0x30), 1),
                new EmptyBorder(2, 6, 2, 6)));
        swField.setToolTipText("Switch-IP eingeben (z.B. 192.168.178.36)");

        // Aktuell markierte Switches als Tooltip anzeigen
        Runnable updateTooltip = () -> {
            if (MANUAL_SWITCHES.isEmpty()) {
                swField.setToolTipText("Switch-IP eingeben (z.B. 192.168.1.36)");
            } else {
                swField.setToolTipText("Markierte Switches: " + MANUAL_SWITCHES);
            }
        };
        updateTooltip.run();

        JButton addSwBtn = mapBtn("+ Switch", new Color(0xFF, 0xA0, 0x30));
        JButton remSwBtn = mapBtn("✕",        WARN);
        remSwBtn.setToolTipText("Switch-Markierung entfernen");

        Runnable addSwitch = () -> {
            String ip = swField.getText().trim();
            if (ip.isEmpty()) return;
            MANUAL_SWITCHES.add(ip);
            saveManualSwitches();
            swField.setText("");
            updateTooltip.run();
            canvas.reload();
        };

        swField.addActionListener(e -> addSwitch.run());
        addSwBtn.addActionListener(e -> addSwitch.run());

        remSwBtn.addActionListener(e -> {
            String ip = swField.getText().trim();
            if (!ip.isEmpty()) {
                MANUAL_SWITCHES.remove(ip);
            } else if (!MANUAL_SWITCHES.isEmpty()) {
                // Kein Text → alle entfernen nach Bestätigung
                int ok = JOptionPane.showConfirmDialog(null,
                        "Alle Switch-Markierungen entfernen?", "Bestätigen",
                        JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) MANUAL_SWITCHES.clear();
            }
            saveManualSwitches();
            swField.setText("");
            updateTooltip.run();
            canvas.reload();
        });

        JButton refreshBtn = mapBtn("↻", ACCENT2);
        JButton layoutBtn  = mapBtn("⊞", INFO);
        refreshBtn.setToolTipText("Aktualisieren (neuer Scan)");
        layoutBtn.setToolTipText("Layout zurücksetzen");
        refreshBtn.addActionListener(e -> { HOP_PARENT.clear(); canvas.reload(); });
        layoutBtn.addActionListener(e -> canvas.resetLayout());

        bar.add(title);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(swLabel); bar.add(swField); bar.add(addSwBtn); bar.add(remSwBtn);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(refreshBtn); bar.add(layoutBtn);

        // Bekannte Switches initial in Tooltip zeigen
        if (!MANUAL_SWITCHES.isEmpty())
            swField.setToolTipText("Markierte Switches: " + MANUAL_SWITCHES);

        return bar;
    }

    private static JLabel buildStatusLabel() {
        JLabel lbl = new JLabel("  Scanne...");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static JPanel buildLegend() {
        Color legBg = new Color(0x08, 0x0C, 0x08, 210);
        Object[][] rows = {
                {"*", "Ich (lokal)",   ACCENT2},
                {"G", "Gateway",       NET_COL},
                {"S", "Switch/Hub",    new Color(0xFF, 0xA0, 0x30)},
                {"W", "Windows",       WIN_COL},
                {"L", "Linux",         LIN_COL},
                {"M", "macOS / iOS",   APL_COL},
                {"A", "Android",       AND_COL},
                {"P", "Drucker",       PRN_COL},
                {"?", "Unbekannt",     FG_DIM},
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
        JLabel hint = new JLabel("  Rechtsklick → Switch markieren");
        hint.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        hint.setForeground(FG_DIM);
        hint.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(hint);
        panel.setPreferredSize(new Dimension(210, rows.length * 18 + 30));
        return panel;
    }

    private static void startBackgroundScan(MapCanvas canvas) {
        new Thread(() -> {
            try {
                // Phase 1: Host-Scan
                List<String> subnets = SubnetDetector.getAllSubnets();
                if (!subnets.isEmpty()) NetworkHostScanner.scan(subnets);

                // Phase 2: Kurz-Traceroute für alle bekannten Hosts (max 4 Hops)
                SwingUtilities.invokeLater(() -> {
                    if (canvas.statusLabel != null)
                        canvas.statusLabel.setText("  Traceroute läuft...");
                });
                runHopDiscovery();

                SwingUtilities.invokeLater(canvas::reload);
            } catch (Exception ignored) {}
        }, "MapBgScan").start();
    }

    /**
     * Führt für jeden bekannten Host einen schnellen Traceroute durch (max 4 Hops).
     * Ergebnis wird in HOP_PARENT gespeichert: Host-IP → direkter Upstream-Knoten.
     *
     * Läuft parallelisiert mit max 20 Threads damit es nicht zu lange dauert.
     */
    static void runHopDiscovery() {
        String gw = RemoteNetScanner.detectDefaultGateway();
        List<HostResult> hosts = NetworkStore.getInstance().getAllHosts();

        java.util.concurrent.ExecutorService exec =
                java.util.concurrent.Executors.newFixedThreadPool(
                        Math.min(hosts.size() + 1, 20));

        for (HostResult h : hosts) {
            exec.submit(() -> {
                try {
                    List<TracerouteRunner.HopInfo> hops =
                            TracerouteRunner.run(h.ip, 4);
                    if (hops.size() < 2) return;

                    // Letzter nicht-Timeout-Hop vor dem Ziel = direkter Parent
                    String parent = null;
                    for (int i = hops.size() - 1; i >= 0; i--) {
                        TracerouteRunner.HopInfo hop = hops.get(i);
                        if (!hop.timeout && !hop.ip.equals(h.ip)
                                && hop.ip != null && !hop.ip.isEmpty()) {
                            parent = hop.ip;
                            break;
                        }
                    }
                    if (parent != null && !parent.equals(gw))
                        HOP_PARENT.put(h.ip, parent);
                } catch (Exception ignored) {}
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── Switch detection (static helpers) ────────────────────────────────

    /**
     * Gibt true zurück wenn dieser Host definitiv ein Endgerät ist.
     * Wird als negativer Gate vor allen Switch-Erkennungspfaden verwendet.
     */
    static boolean isEndDevice(String ip, String os, String hostname) {
        String o = os       != null ? os.toLowerCase()       : "";
        String h = hostname != null ? hostname.toLowerCase() : "";

        for (String kw : ENDDEVICE_OS) if (o.contains(kw)) return true;
        for (String kw : ENDDEVICE_HN) if (h.contains(kw)) return true;

        // Port-basierte Prüfung aus gespeicherten Hosts
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(hr -> hr.ip.equals(ip))
                .findFirst()
                .map(hr -> hr.ports.keySet().stream().anyMatch(ENDDEVICE_PORTS::contains))
                .orElse(false);
    }

    static boolean isSwitchByKeyword(String os, String hostname) {
        String o = os       != null ? os.toLowerCase()       : "";
        String h = hostname != null ? hostname.toLowerCase() : "";
        return o.contains("switch") || o.contains("hub") || o.contains("router")
                || o.contains("fritz")  || o.contains("netzwerk") || o.contains("unifi")
                || o.contains("mikrotik") || o.contains("cisco") || o.contains("netgear")
                || o.contains("tp-link") || o.contains("access point") || o.contains("procurve")
                || o.contains("aruba")
                || h.contains("switch") || h.contains("hub") || h.contains("router")
                || h.contains("sw-")    || h.contains("-sw") || h.contains("sg-")
                || h.contains("gs-")    || h.contains("fritz") || h.contains("unifi")
                || h.contains("mikrotik") || h.contains("ap-") || h.contains("procurve");
    }

    /** OUI aus MAC extrahieren (erwartet Format aus Hostname "[AA:BB:CC:DD:EE:FF]"). */
    static boolean isSwitchByOui(String hostname) {
        if (hostname == null) return false;
        int s = hostname.indexOf('['), e = hostname.indexOf(']');
        if (s < 0 || e <= s + 7) return false;
        String mac = hostname.substring(s + 1, e).trim().toUpperCase().replace("-", ":");
        if (mac.length() < 8) return false;
        return SWITCH_OUIS.contains(mac.substring(0, 8));
    }

    static boolean isSwitchByPorts(String ip) {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> h.ip.equals(ip))
                .findFirst()
                .map(h -> (h.ports.containsKey(161) || h.ports.containsKey(23))
                        && h.ports.keySet().stream().noneMatch(ENDDEVICE_PORTS::contains))
                .orElse(false);
    }

    // ── Canvas ────────────────────────────────────────────────────────────

    static class MapCanvas extends JPanel {

        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Color      bg;
        private JLabel titleLabel;
        private JLabel statusLabel;

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

        void setTitleLabel(JLabel l)  { this.titleLabel  = l; }
        void setStatusLabel(JLabel l) { this.statusLabel = l; }
        int  hostCount() { return (int) nodes.stream().filter(n -> n.type == NodeType.HOST).count(); }

        // ── Build ─────────────────────────────────────────────────────────

        void reload() {
            nodes.clear();
            edges.clear();
            Set<String> seen = new LinkedHashSet<>();

            String gw    = RemoteNetScanner.detectDefaultGateway();
            Node gwNode  = null;
            if (gw != null && seen.add(gw))
                gwNode = node(gw, "Gateway", "Router / Netzwerkgeraet", NodeType.GATEWAY);

            Node selfNode = null;
            try {
                InetAddress self = InetAddress.getLocalHost();
                if (seen.add(self.getHostAddress()))
                    selfNode = node(self.getHostAddress(),
                            self.getHostName() + " (ich)", localOs(), NodeType.SELF);
            } catch (Exception ignored) {}

            for (ScanHistory.Entry e : ScanHistory.getInstance().getAll())
                for (ScanResult r : e.results)
                    if (seen.add(r.getIp()))
                        node(r.getIp(), r.getHostname(), r.getOsGuess(), NodeType.HOST);

            for (HostResult h : NetworkStore.getInstance().getAllHosts())
                if (seen.add(h.ip))
                    node(h.ip, cleanHostname(h.hostname), h.os, NodeType.HOST);

            classifySwitches();

            // Hop-Daten neu holen falls noch leer (z.B. nach manuellem Reload)
            if (HOP_PARENT.isEmpty()) {
                if (statusLabel != null)
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText("  Traceroute läuft..."));
                new Thread(() -> {
                    runHopDiscovery();
                    SwingUtilities.invokeLater(this::rebuildEdgesAndLayout);
                }, "MapHopRefresh").start();
            }

            buildEdges(gwNode, selfNode);
            resetLayout();

            int cnt = hostCount();
            if (titleLabel  != null) SwingUtilities.invokeLater(() ->
                    titleLabel.setText("  Netzwerk-Karte  –  " + cnt + " Hosts"));
            if (statusLabel != null) SwingUtilities.invokeLater(() ->
                    statusLabel.setText("  " + cnt + " Hosts  |  "
                            + "Unbekannter Switch? → Rechtsklick → \"Als Switch markieren\""));
        }

        /** Wird nach asynchronem Hop-Discovery aufgerufen. */
        private void rebuildEdgesAndLayout() {
            edges.clear();
            Node gwNode   = nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst().orElse(null);
            Node selfNode = nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst().orElse(null);
            buildEdges(gwNode, selfNode);
            resetLayout();
            int cnt = hostCount();
            if (statusLabel != null)
                statusLabel.setText("  " + cnt + " Hosts  |  "
                        + "Unbekannter Switch? → Rechtsklick → \"Als Switch markieren\"");
        }

        private Node node(String ip, String hn, String os, NodeType t) {
            Node n = new Node(ip, hn, os, t);
            nodes.add(n);
            return n;
        }

        /**
         * Klassifiziert HOST-Nodes als SWITCH wenn eindeutige Hinweise vorliegen.
         * isEndDevice() wird als Gate VOR JEDEM Pass geprüft.
         */
        private void classifySwitches() {
            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;

                // Pass 1: manuell
                if (MANUAL_SWITCHES.contains(n.ip)) { n.type = NodeType.SWITCH; continue; }

                // isEndDevice-Gate: wenn Endgerät → überspringen
                if (isEndDevice(n.ip, n.os, n.hostname)) continue;

                // Pass 2: Keyword
                if (isSwitchByKeyword(n.os, n.hostname)) { n.type = NodeType.SWITCH; continue; }

                // Pass 3: OUI
                if (isSwitchByOui(n.hostname)) { n.type = NodeType.SWITCH; continue; }

                // Pass 4: Ports
                if (isSwitchByPorts(n.ip)) n.type = NodeType.SWITCH;
            }
        }

        private void buildEdges(Node gwNode, Node selfNode) {
            if (gwNode == null) return;

            // Switches → Gateway (Uplink) — initiale Liste
            List<Node> switches = nodes.stream()
                    .filter(n -> n.type == NodeType.SWITCH).collect(Collectors.toList());
            switches.forEach(sw -> edges.add(new Edge(sw, gwNode, EdgeType.UPLINK)));

            if (selfNode != null)
                edges.add(new Edge(selfNode, gwNode, EdgeType.SELF_LINK));

            for (Node n : nodes) {
                if (n.type != NodeType.HOST) continue;

                // Priorität 1: Hop-basierter Parent aus Traceroute
                String hopParentIp = HOP_PARENT.get(n.ip);
                if (hopParentIp != null) {
                    Node hopNode = nodes.stream()
                            .filter(x -> x.ip.equals(hopParentIp))
                            .findFirst().orElse(null);
                    if (hopNode == null) {
                        hopNode = node(hopParentIp, hopParentIp, "Router / Switch", NodeType.SWITCH);
                        edges.add(new Edge(hopNode, gwNode, EdgeType.UPLINK));
                        switches.add(hopNode);
                    } else if (hopNode.type == NodeType.HOST) {
                        hopNode.type = NodeType.SWITCH;
                        edges.add(new Edge(hopNode, gwNode, EdgeType.UPLINK));
                        switches.add(hopNode);
                    }
                    edges.add(new Edge(n, hopNode, EdgeType.NORMAL));
                    continue;
                }

                // Priorität 2: Manuell markierter Switch im gleichen /24
                // (routed ALLE Hosts im /24 über den Switch, unabhängig von Hop-Daten)
                String sub = subnet24(n.ip);
                Node manual = sub == null ? null : switches.stream()
                        .filter(sw -> MANUAL_SWITCHES.contains(sw.ip)
                                && sub.equals(subnet24(sw.ip)))
                        .findFirst().orElse(null);
                if (manual != null) {
                    edges.add(new Edge(n, manual, EdgeType.NORMAL));
                    continue;
                }

                // Priorität 3: Beliebiger Switch im gleichen /24 (nächster IP-Abstand)
                Node nearest = sub == null ? null : switches.stream()
                        .filter(sw -> sub.equals(subnet24(sw.ip)))
                        .min(Comparator.comparingInt(sw ->
                                Math.abs(lastOctet(sw.ip) - lastOctet(n.ip))))
                        .orElse(null);

                edges.add(new Edge(n, nearest != null ? nearest : gwNode, EdgeType.NORMAL));
            }
        }

        // ── Layout ────────────────────────────────────────────────────────

        void resetLayout() {
            camX = 0; camY = 0; scale = 1.0;
            int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
            int cx = w / 2, cy = h / 2;

            nodes.stream().filter(n -> n.type == NodeType.GATEWAY).findFirst()
                    .ifPresent(n -> { n.x = cx; n.y = cy; });
            nodes.stream().filter(n -> n.type == NodeType.SELF).findFirst()
                    .ifPresent(n -> { n.x = cx - 140; n.y = cy - 110; });

            List<Node> switches = nodes.stream().filter(n -> n.type == NodeType.SWITCH).toList();
            if (!switches.isEmpty()) {
                int r = Math.min(cx, cy) - 100;
                double step = 2 * Math.PI / switches.size();
                for (int i = 0; i < switches.size(); i++) {
                    double a = i * step - Math.PI / 2;
                    switches.get(i).x = (int)(cx + r * Math.cos(a));
                    switches.get(i).y = (int)(cy + r * Math.sin(a));
                }
            }

            // Hosts um ihren Parent gruppieren
            Map<Node, List<Node>> groups = new LinkedHashMap<>();
            for (Edge e : edges)
                if (e.from.type == NodeType.HOST)
                    groups.computeIfAbsent(e.to, k -> new ArrayList<>()).add(e.from);

            groups.forEach((parent, children) -> {
                int n   = children.size();
                int rad = 80 + n * 7;
                double base = Math.atan2(parent.y - cy, parent.x - cx);
                double step = n > 1 ? Math.PI / (n - 1) : 0;
                double start = base - (n > 1 ? Math.PI / 2 : 0);
                for (int i = 0; i < n; i++) {
                    double a = start + i * step;
                    children.get(i).x = parent.x + (int)(rad * Math.cos(a));
                    children.get(i).y = parent.y + (int)(rad * Math.sin(a));
                }
            });
            repaint();
        }

        // ── Paint ─────────────────────────────────────────────────────────

        @Override protected void paintComponent(Graphics g) {
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
            edges.forEach(e -> drawEdge(g2, e));
            nodes.forEach(n -> drawNode(g2, n));
            g2.dispose();
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(GuiTheme.isDark()
                    ? new Color(0x12, 0x18, 0x12) : new Color(0xE4, 0xE2, 0xDC));
            for (int x = -3000; x < 6000; x += 60) g2.drawLine(x, -3000, x, 6000);
            for (int y = -3000; y < 6000; y += 60) g2.drawLine(-3000, y, 6000, y);
        }

        private void drawEdge(Graphics2D g2, Edge e) {
            switch (e.type) {
                case SELF_LINK -> { g2.setStroke(new BasicStroke(2f));
                    g2.setColor(ACCENT2); }
                case UPLINK    -> { g2.setStroke(new BasicStroke(1.8f));
                    g2.setColor(new Color(0xFF, 0xA0, 0x30, 200)); }
                default        -> { g2.setStroke(new BasicStroke(1f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{4f, 4f}, 0f));
                    g2.setColor(GuiTheme.isDark()
                            ? new Color(0x30, 0x45, 0x30)
                            : new Color(0xC0, 0xBC, 0xB4)); }
            }
            g2.drawLine(e.from.x, e.from.y, e.to.x, e.to.y);
        }

        private void drawNode(Graphics2D g2, Node n) {
            Color col = nodeColor(n);
            int r = switch (n.type) {
                case GATEWAY, SWITCH -> 20;
                case SELF            -> 17;
                default              -> 13;
            };
            // Glow
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
            g2.fillOval(n.x - r - 6, n.y - r - 6, (r + 6) * 2, (r + 6) * 2);
            // Body
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
                default      -> osIcon(n.os);
            };
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, r > 14 ? 11 : 9));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1f));
            g2.drawString(icon, n.x - fm.stringWidth(icon) / 2, n.y + fm.getAscent() / 2 - 1);
            // IP
            g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            g2.setColor(GuiTheme.isDark() ? new Color(0xA0, 0x9C, 0x90) : new Color(0x40, 0x42, 0x3E));
            g2.drawString(n.ip, n.x - fm.stringWidth(n.ip) / 2, n.y + r + 13);
            // Hostname
            if (n.hostname != null && !n.hostname.equals(n.ip)) {
                String hn = n.hostname.length() > 18 ? n.hostname.substring(0, 17) + "." : n.hostname;
                g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                g2.setColor(n.type == NodeType.SELF ? ACCENT2 : FG_DIM);
                g2.drawString(hn, n.x - fm.stringWidth(hn) / 2, n.y + r + 23);
            }
        }

        // ── Mouse ─────────────────────────────────────────────────────────

        private void installMouse() {
            addMouseWheelListener(e -> {
                double f = e.getWheelRotation() < 0 ? 1.12 : 0.9;
                scale = Math.max(0.15, Math.min(5.0, scale * f));
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    Point w = toWorld(e.getPoint());
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        dragNode = nodeAt(w);
                        if (dragNode != null) {
                            dragNodeStart = e.getPoint();
                            dragNodeOrigX = dragNode.x; dragNodeOrigY = dragNode.y;
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        Node hit = nodeAt(w);
                        if (hit != null && hit.type != NodeType.GATEWAY && hit.type != NodeType.SELF)
                            showContextMenu(hit, e.getComponent(), e.getX(), e.getY());
                        else { panning = true; panStart = e.getPoint();
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
                    }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragNode = null; panning = false; setCursor(Cursor.getDefaultCursor());
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
                        dragNode.x = (int)(dragNodeOrigX + (e.getX() - dragNodeStart.x) / scale);
                        dragNode.y = (int)(dragNodeOrigY + (e.getY() - dragNodeStart.y) / scale);
                        repaint();
                    } else if (panning && panStart != null) {
                        camX += (e.getX() - panStart.x) / scale;
                        camY += (e.getY() - panStart.y) / scale;
                        panStart = e.getPoint(); repaint();
                    }
                }
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(nodeAt(toWorld(e.getPoint())) != null
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        private void showContextMenu(Node n, Component comp, int x, int y) {
            boolean isSw = n.type == NodeType.SWITCH;
            JPopupMenu menu = new JPopupMenu();
            Color menuBg  = new Color(0x0F, 0x13, 0x10);
            Color menuHov = new Color(0x1A, 0x22, 0x1A);
            menu.setBackground(menuBg);
            menu.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

            JMenuItem hdr = menuItem(n.ip + "  " + clip(n.hostname, 20), FG_DIM, menuBg, menuHov);
            hdr.setEnabled(false);
            menu.add(hdr);
            menu.addSeparator();

            JMenuItem swItem = menuItem(
                    isSw ? "✕  Kein Switch  (zurücksetzen)" : "S  Als Switch/Hub markieren",
                    isSw ? WARN : new Color(0xFF, 0xA0, 0x30), menuBg, menuHov);
            swItem.addActionListener(e -> {
                if (isSw) MANUAL_SWITCHES.remove(n.ip);
                else      MANUAL_SWITCHES.add(n.ip);
                saveManualSwitches();
                reload();
            });
            menu.add(swItem);

            JMenuItem det = menuItem("🔍  Details anzeigen", ACCENT, menuBg, menuHov);
            det.addActionListener(e ->
                    HostDetailsPanel.show(n.ip, n.hostname, n.os,
                            NetworkStore.getInstance().findNetwork(n.ip)));
            menu.add(det);
            menu.show(comp, x, y);
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

        private Point toWorld(Point s) {
            int sw = getWidth(), sh = getHeight();
            return new Point(
                    (int)((s.x - sw / 2.0) / scale + sw / 2.0 - camX),
                    (int)((s.y - sh / 2.0) / scale + sh / 2.0 - camY));
        }

        private Node nodeAt(Point w) {
            return nodes.stream()
                    .filter(n -> Math.hypot(n.x - w.x, n.y - w.y) < 28)
                    .findFirst().orElse(null);
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────

    static String subnet24(String ip) {
        if (ip == null) return null;
        int i = ip.lastIndexOf('.');
        return i > 0 ? ip.substring(0, i) : null;
    }

    static int lastOctet(String ip) {
        if (ip == null) return 999;
        int i = ip.lastIndexOf('.');
        try { return Integer.parseInt(ip.substring(i + 1)); }
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
        if (l.contains("windows"))                          return "W";
        if (l.contains("linux") || l.contains("unix"))     return "L";
        if (l.contains("mac")   || l.contains("ios"))      return "M";
        if (l.contains("android"))                         return "A";
        if (l.contains("drucker") || l.contains("printer")
                || l.contains("jetdirect"))                return "P";
        if (l.contains("router") || l.contains("fritz")
                || l.contains("switch"))                   return "R";
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

    private static String clip(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
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

    enum NodeType { GATEWAY, SELF, SWITCH, HOST }
    enum EdgeType { NORMAL, UPLINK, SELF_LINK }

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
        Edge(Node from, Node to, EdgeType type) { this.from = from; this.to = to; this.type = type; }
    }
}