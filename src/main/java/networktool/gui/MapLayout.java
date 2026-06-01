package main.java.networktool.gui;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Berechnet initiale Node-Positionen für die Netzwerkkarte.
 * Gateway zentral, Switches im Ring, Hosts um ihren Parent gruppiert.
 */
final class MapLayout {

    private MapLayout() {}

    static void apply(List<GuiNetworkMap.Node> nodes,
                      List<GuiNetworkMap.Edge> edges,
                      int canvasWidth, int canvasHeight) {
        int cx = canvasWidth  / 2;
        int cy = canvasHeight / 2;

        placeGateway(nodes, cx, cy);
        placeSelf(nodes, cx, cy);
        placeSwitches(nodes, cx, cy);
        placeHosts(nodes, edges, cx, cy);
    }

    private static void placeGateway(List<GuiNetworkMap.Node> nodes, int cx, int cy) {
        nodes.stream().filter(n -> n.type == GuiNetworkMap.NodeType.GATEWAY)
                .findFirst().ifPresent(n -> { n.x = cx; n.y = cy; });
    }

    private static void placeSelf(List<GuiNetworkMap.Node> nodes, int cx, int cy) {
        nodes.stream().filter(n -> n.type == GuiNetworkMap.NodeType.SELF)
                .findFirst().ifPresent(n -> { n.x = cx - 140; n.y = cy - 110; });
    }

    private static void placeSwitches(List<GuiNetworkMap.Node> nodes, int cx, int cy) {
        List<GuiNetworkMap.Node> switches = nodes.stream()
                .filter(n -> n.type == GuiNetworkMap.NodeType.SWITCH)
                .collect(Collectors.toList());
        if (switches.isEmpty()) return;

        int radius = Math.min(cx, cy) - 100;
        double step = 2 * Math.PI / switches.size();
        for (int i = 0; i < switches.size(); i++) {
            double angle = i * step - Math.PI / 2;
            switches.get(i).x = (int)(cx + radius * Math.cos(angle));
            switches.get(i).y = (int)(cy + radius * Math.sin(angle));
        }
    }

    private static void placeHosts(List<GuiNetworkMap.Node> nodes,
                                   List<GuiNetworkMap.Edge> edges,
                                   int cx, int cy) {
        Map<GuiNetworkMap.Node, List<GuiNetworkMap.Node>> groups = new LinkedHashMap<>();
        for (GuiNetworkMap.Edge e : edges)
            if (e.from.type == GuiNetworkMap.NodeType.HOST)
                groups.computeIfAbsent(e.to, k -> new ArrayList<>()).add(e.from);

        groups.forEach((parent, children) -> {
            int n      = children.size();
            int radius = 80 + n * 7;
            double base  = Math.atan2(parent.y - cy, parent.x - cx);
            double range = n > 1 ? Math.PI : 0;
            double step  = n > 1 ? range / (n - 1) : 0;
            for (int i = 0; i < n; i++) {
                double angle = base - range / 2 + i * step;
                children.get(i).x = parent.x + (int)(radius * Math.cos(angle));
                children.get(i).y = parent.y + (int)(radius * Math.sin(angle));
            }
        });
    }
}