package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;

import java.awt.Color;
import java.util.List;

/** Momentaufnahme der Topologie (Knoten + Kanten) für Exporte, ohne die Karte anzuzeigen. */
public record MapSnapshot(List<GuiNetworkMap.Node> nodes, List<GuiNetworkMap.Edge> edges) {

    public static MapSnapshot capture() {
        MapCanvas canvas = new MapCanvas(Color.BLACK);
        canvas.reload();
        return new MapSnapshot(List.copyOf(canvas.nodes), List.copyOf(canvas.edges));
    }
}
