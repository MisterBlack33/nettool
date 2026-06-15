package main.java.networktool.gui;

import main.java.networktool.logic.scan.LastScanCache;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.SubnetDetector;
import main.java.networktool.model.ScanResult;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Orchestriert Netzwerk-Topologie-Karte.
 *
 * Beim Aufrufen führt die Karte automatisch einen Quick Scan des lokalen
 * Netzwerks durch und aktualisiert die Host-Daten in LastScanCache.
 *
 * Daten kommen aus:
 *  • Quick Scan des lokalen Netzwerks (automatisch beim Show)
 *  • ScanHistory + NetworkStore für historische Daten
 *
 * Hintergrund-Tasks:
 *  1. Hop-Discovery (Traceroute) → Switch-Erkennung per Pfad
 *  2. Traffic-Probe (DNS/DHCP/mDNS) → Infra-Rollen-Erkennung
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    static final Map<String, String> HOP_PARENT = Collections.synchronizedMap(new HashMap<>());
    private static final AtomicBoolean scanRunning = new AtomicBoolean(false);

    public static boolean isScanRunning() { return scanRunning.get(); }

    public static void show() {
        SwingUtilities.invokeLater(GuiNetworkMap::buildWindow);
        // Starte Quick Scan des lokalen Netzwerks im Hintergrund
        startQuickLocalScan();
    }

    private static void buildWindow() {
        JDialog dlg = new JDialog((Frame) null, "Netzwerk-Karte", false);
        dlg.setSize(900, 640);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg = GuiTheme.isDark() ? new Color(0x06, 0x09, 0x07) : new Color(0xF2, 0xF0, 0xEC);
        MapCanvas canvas    = new MapCanvas(bg);
        JLabel    statusLbl = buildStatusLabel();
        canvas.setStatusLabel(statusLbl);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.add(buildToolbar(canvas), BorderLayout.NORTH);
        root.add(buildLayered(canvas, bg), BorderLayout.CENTER);
        root.add(statusLbl, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);

        SwingUtilities.invokeLater(canvas::reload);
        startHopDiscovery(canvas);
    }

    // ── Hintergrund-Tasks ─────────────────────────────────────────────────

    /**
     * Führt einen Quick Scan des lokalen Netzwerks durch
     * und aktualisiert LastScanCache mit den Ergebnissen.
     */
    private static void startQuickLocalScan() {
        new Thread(() -> {
            try {
                List<String> cidrs = SubnetDetector.getAllCidrs();
                if (cidrs.isEmpty()) {
                    System.out.println("[GuiNetworkMap] Keine lokalen Netze erkannt");
                    return;
                }

                // Scanne das erste lokale Netzwerk (üblicherweise das Hauptnetzwerk)
                String mainCidr = cidrs.getFirst();
                System.out.println("[GuiNetworkMap] Starte Quick Scan für: " + mainCidr);

                List<ScanResult> results = NetworkScanner.scanCIDR(mainCidr);

                if (!results.isEmpty()) {
                    LastScanCache.updateFromScanResults(results);
                    System.out.println("[GuiNetworkMap] Quick Scan abgeschlossen: " + results.size() + " Hosts");

                    // Benachrichtige User im GUI wenn aktiv
                    if (GUI.isGuiActive()) {
                        GUI.instance().setStatus(
                            "Netzwerk-Karte aktualisiert (" + results.size() + " Hosts)",
                            ACCENT2);
                    }
                }
            } catch (Exception e) {
                System.err.println("[GuiNetworkMap] Quick Scan Fehler: " + e.getMessage());
            }
        }, "GuiNetworkMapQuickScan").start();
    }

    private static void startHopDiscovery(MapCanvas canvas) {
        new Thread(() -> {
            scanRunning.set(true);
            if (GUI.isGuiActive()) GUI.instance().setStatus("Hop-Analyse läuft…", ACCENT);
            try {
                canvas.setStatus("  Traceroute läuft…");
                HOP_PARENT.putAll(MapHopDiscovery.discover());
                SwingUtilities.invokeLater(() -> {
                    canvas.reload();
                    // Nach Hop-Discovery Traffic-Probe starten
                    canvas.runTrafficProbing();
                });
            } catch (Exception ignored) {
            } finally {
                scanRunning.set(false);
                if (GUI.isGuiActive()) GUI.instance().setStatus("Fertig", ACCENT2);
            }
        }, "MapHopDiscovery").start();
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private static JLayeredPane buildLayered(MapCanvas canvas, Color bg) {
        JLayeredPane pane = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                Component legend = getComponentsInLayer(JLayeredPane.PALETTE_LAYER)[0];
                Dimension ls = legend.getPreferredSize();
                legend.setBounds(10, h - ls.height - 10, ls.width, ls.height);
            }
        };
        pane.setBackground(bg);
        pane.setOpaque(true);
        pane.add(canvas, JLayeredPane.DEFAULT_LAYER);
        pane.add(MapLegend.build(), JLayeredPane.PALETTE_LAYER);
        return pane;
    }

    private static JPanel buildToolbar(MapCanvas canvas) {
        Color barBg = GuiTheme.isDark() ? new Color(0x0A, 0x0E, 0x0B) : new Color(0xE4, 0xE2, 0xDC);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel titleLabel = new JLabel("  Netzwerk-Karte");
        titleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        titleLabel.setForeground(ACCENT);
        canvas.setTitleLabel(titleLabel);

        JTextField switchInput = buildSwitchInput();

        JButton addBtn     = toolBtn("+S",         new Color(0xFF, 0xA0, 0x30));
        JButton refreshBtn = toolBtn("↻",           ACCENT2);
        JButton layoutBtn  = toolBtn("⊞",           INFO);
        JButton probeBtn   = toolBtn("DNS/DHCP",    new Color(0xA0, 0xD8, 0xFF));

        addBtn.setToolTipText("Switch-IP manuell hinzufügen");
        probeBtn.setToolTipText("DNS/DHCP/mDNS-Rollen erneut erkennen");
        refreshBtn.setToolTipText("Karte + Hop-Discovery neu laden");
        layoutBtn.setToolTipText("Layout zurücksetzen");

        addBtn.addActionListener(e -> {
            String ip = switchInput.getText().trim();
            if (!ip.isBlank()) {
                MapSwitchStore.add(ip);
                switchInput.setText("");
                canvas.reload();
            }
        });
        switchInput.addActionListener(e -> addBtn.doClick());

        refreshBtn.addActionListener(e -> {
            HOP_PARENT.clear();
            canvas.reload();
            startHopDiscovery(canvas);
        });
        layoutBtn.addActionListener(e -> canvas.resetLayout());
        probeBtn.addActionListener(e -> canvas.runTrafficProbing());

        bar.add(titleLabel);
        bar.add(switchInput);
        bar.add(addBtn);
        bar.add(refreshBtn);
        bar.add(layoutBtn);
        bar.add(probeBtn);
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

    private static JLabel buildStatusLabel() {
        JLabel lbl = new JLabel("  Lade Karte…");
        lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        lbl.setForeground(FG_DIM);
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static JButton toolBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 8, 4, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(GuiTheme.isDark() ? new Color(0x24, 0x30, 0x24) : new Color(0xCC, 0xCA, 0xC4));
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xDC, 0xDA, 0xD4));
            }
        });
        return b;
    }

    // ── Datentypen ────────────────────────────────────────────────────────

    enum NodeType  { GATEWAY, SELF, SWITCH, HOST }
    enum EdgeType  { NORMAL, UPLINK, SELF_LINK }

    static class Node {
        String ip, hostname, os;
        NodeType type;
        int x, y;

        Node(String ip, String hn, String os, NodeType type) {
            this.ip = ip; hostname = hn; this.os = os; this.type = type;
        }
    }

    static class Edge {
        final Node from, to;
        final EdgeType type;

        Edge(Node from, Node to, EdgeType type) {
            this.from = from; this.to = to; this.type = type;
        }
    }
}