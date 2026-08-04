package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import networktool.theme.GuiTheme;

/**
 * Orchestriert Netzwerk-Topologie-Karte.
 *
 * Beim Aufrufen führt die Karte automatisch einen Quick Scan des lokalen
 * Netzwerks durch und aktualisiert die Host-Daten in LastScanCache.
 *
 * Hintergrund-Tasks siehe {@link GuiNetworkMapScanTasks},
 * Toolbar/Layout siehe {@link GuiNetworkMapChrome}.
 */
public final class GuiNetworkMap {

    private GuiNetworkMap() {}

    public static final Map<String, String> HOP_PARENT = Collections.synchronizedMap(new HashMap<>());

    public static boolean isScanRunning() { return GuiNetworkMapScanTasks.isScanRunning(); }

    public static void show() {
        SwingUtilities.invokeLater(GuiNetworkMap::buildWindow);
        // Starte Quick Scan des lokalen Netzwerks im Hintergrund
        GuiNetworkMapScanTasks.startQuickLocalScan();
    }

    private static void buildWindow() {
        JDialog dlg = new JDialog((Frame) null, "Netzwerk-Karte", false);
        dlg.setSize(900, 640);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(true);

        Color bg = GuiTheme.isDark() ? new Color(0x06, 0x09, 0x07) : new Color(0xF2, 0xF0, 0xEC);
        MapCanvas canvas    = new MapCanvas(bg);
        JLabel    statusLbl = GuiNetworkMapChrome.buildStatusLabel();
        canvas.setStatusLabel(statusLbl);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.add(GuiNetworkMapChrome.buildToolbar(canvas, HOP_PARENT,
                () -> GuiNetworkMapScanTasks.startHopDiscovery(canvas, HOP_PARENT)), BorderLayout.NORTH);
        root.add(GuiNetworkMapChrome.buildLayered(canvas, bg), BorderLayout.CENTER);
        root.add(statusLbl, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);

        SwingUtilities.invokeLater(canvas::reload);
        GuiNetworkMapScanTasks.startHopDiscovery(canvas, HOP_PARENT);
    }

    // ── Datentypen ────────────────────────────────────────────────────────

    public enum NodeType  { GATEWAY, SELF, SWITCH, HOST }
    public enum EdgeType  { NORMAL, UPLINK, SELF_LINK }

    public static class Node {
        public String ip, hostname, os;
        public NodeType type;
        public int x, y;

        public Node(String ip, String hn, String os, NodeType type) {
            this.ip = ip; hostname = hn; this.os = os; this.type = type;
        }
    }

    public static class Edge {
        public final Node from, to;
        public final EdgeType type;

        public Edge(Node from, Node to, EdgeType type) {
            this.from = from; this.to = to; this.type = type;
        }
    }
}
