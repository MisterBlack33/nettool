package networktool.gui.map;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.logic.scan.MapTrafficObserver;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static networktool.theme.GuiTheme.*;

/**
 * Rendert die Netzwerk-Topologie-Karte.
 *
 * Datenquellen (kein eigener Scan):
 *  - ScanHistory (Session-Ergebnisse)
 *  - NetworkStore (gespeicherte Hosts)
 *  - MapHopDiscovery (Traceroute-Zwischenknoten)
 *  - MapTrafficObserver (DNS/DHCP/mDNS-Rollen per UDP-Probe)
 */
public final class MapCanvas extends JPanel {

    private static final Logger LOG = Logger.getLogger(MapCanvas.class.getName());

    final List<GuiNetworkMap.Node> nodes = new ArrayList<>();
    final List<GuiNetworkMap.Edge> edges = new ArrayList<>();

    private final MapTrafficObserver trafficObserver = new MapTrafficObserver();
    private final Color bg;

    private JLabel titleLabel;
    private JLabel statusLabel;

    private double scale = 1.0, camX = 0, camY = 0;
    private GuiNetworkMap.Node dragNode;
    private Point  dragStart;
    private int    dragOrigX, dragOrigY;
    private boolean panning;
    private Point   panStart;

    public MapCanvas(Color bg) {
        this.bg = bg;
        setBackground(bg);
        installMouse();
    }

    public void setTitleLabel(JLabel label)  { this.titleLabel  = label; }
    public void setStatusLabel(JLabel label) { this.statusLabel = label; }

    int hostCount() {
        return (int) nodes.stream().filter(n -> n.type == GuiNetworkMap.NodeType.HOST).count();
    }

    public void setStatus(String msg) {
        if (statusLabel != null) SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    public void reload() {
        nodes.clear();
        edges.clear();
        GuiNetworkMap.Node gwNode   = MapNodeCollector.collectInto(nodes);
        GuiNetworkMap.Node selfNode = nodes.stream()
                .filter(n -> n.type == GuiNetworkMap.NodeType.SELF).findFirst().orElse(null);
        MapTopology.classifyNodes(nodes);
        MapNodeCollector.applyTrafficRoles(nodes, trafficObserver);
        edges.addAll(MapTopology.buildEdges(nodes, gwNode, selfNode, GuiNetworkMap.HOP_PARENT));
        resetLayout();
        updateLabels();
    }

    public void resetLayout() {
        camX = 0; camY = 0; scale = 1.0;
        int w = Math.max(getWidth(), 800), h = Math.max(getHeight(), 600);
        MapLayout.apply(nodes, edges, w, h);
        repaint();
    }

    /**
     * Traffic-Probing für alle bekannten Hosts.
     * Läuft asynchron – Ergebnis wird in trafficObserver gespeichert.
     */
    public void runTrafficProbing() {
        List<String> ips = nodes.stream()
                .filter(n -> n.type == GuiNetworkMap.NodeType.HOST
                        || n.type == GuiNetworkMap.NodeType.SWITCH)
                .map(n -> n.ip)
                .toList();
        if (ips.isEmpty()) return;

        setStatus("  Probe DNS/DHCP/mDNS (" + ips.size() + " Hosts)…");
        new Thread(() -> {
            trafficObserver.probeAll(ips);
            SwingUtilities.invokeLater(() -> {
                MapNodeCollector.applyTrafficRoles(nodes, trafficObserver);
                repaint();
                setStatus("  Traffic-Probe abgeschlossen.");
            });
        }, "MapTrafficProbe").start();
    }

    GuiNetworkMap.Node nodeAt(Point world) {
        return nodes.stream()
                .filter(n -> Math.hypot(n.x - world.x, n.y - world.y) < 28)
                .findFirst().orElse(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        applyCamera(g2);
        MapRenderer.drawBackground(g2, bg, getWidth(), getHeight());
        edges.forEach(e -> MapRenderer.drawEdge(g2, e));
        nodes.forEach(n -> MapRenderer.drawNode(g2, n));
        g2.dispose();
    }

    // ── Camera ────────────────────────────────────────────────────────────

    private void applyCamera(Graphics2D g2) {
        int sw = getWidth(), sh = getHeight();
        g2.translate(sw / 2.0, sh / 2.0);
        g2.scale(scale, scale);
        g2.translate(-sw / 2.0 + camX, -sh / 2.0 + camY);
    }

    private Point toWorld(Point screen) {
        int sw = getWidth(), sh = getHeight();
        return new Point(
                (int)((screen.x - sw / 2.0) / scale + sw / 2.0 - camX),
                (int)((screen.y - sh / 2.0) / scale + sh / 2.0 - camY));
    }

    // ── Mouse ─────────────────────────────────────────────────────────────

    private void installMouse() {
        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.12 : 0.9;
            scale = Math.max(0.15, Math.min(5.0, scale * factor));
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { handlePress(e); }
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
            GuiNetworkMap.Node hit = nodeAt(world);
            if (hit != null && hit.type != GuiNetworkMap.NodeType.GATEWAY
                    && hit.type != GuiNetworkMap.NodeType.SELF)
                MapContextMenu.show(hit, e.getComponent(), e.getX(), e.getY(), this);
            else {
                panning = true; panStart = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
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

    // ── Helpers ───────────────────────────────────────────────────────────

    private void updateLabels() {
        int count = hostCount();
        if (titleLabel != null)
            SwingUtilities.invokeLater(() ->
                    titleLabel.setText("  Netzwerk-Karte  –  " + count + " Hosts"));
        setStatus("  " + count + " Hosts  |  Switch-IP oben eingeben oder Rechtsklick");
    }
}
