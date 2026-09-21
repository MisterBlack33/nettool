package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.map.MapHeatmapSettings;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import static main.java.networktool.theme.GuiTheme.*;

/** Test-Suite-Aktion "Karte: Heatmap" (Menü-ID "38"). */
public final class GuiMapHeatmapActions {

    private GuiMapHeatmapActions() {}

    public static void toggle(GuiOutputPanel output) {
        boolean on = MapHeatmapSettings.getInstance().toggle();
        AuditLogger.getInstance().log("MAP_HEATMAP", on ? "on" : "off");
        output.appendText("  " + StatusTags.OK + " Karten-Heatmap " + (on ? "aktiv" : "aus")
                + " – in der Karte ↻ drücken\n", on ? ACCENT2 : WARN);
    }
}
