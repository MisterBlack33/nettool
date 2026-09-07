package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.components.TrafficVisualizerPanel;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.sonify.ActiveInterfaceDetector;
import main.java.networktool.logic.visualize.TrafficVisualizer;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import javax.swing.*;

import static main.java.networktool.theme.GuiTheme.*;

/** Sidebar-Aktion für den Traffic-Visualizer (Menü-ID "25"). */
public final class GuiTrafficVisualizerActions {

    private GuiTrafficVisualizerActions() {}

    public static void toggle(GuiInputPanel input, GuiOutputPanel output) {
        TrafficVisualizer visualizer = TrafficVisualizer.getInstance();
        if (visualizer.isActive()) {
            visualizer.stop();
            AuditLogger.getInstance().log("TRAFFIC_VIS_STOP", visualizer.getActiveInterface());
            output.appendText("  " + StatusTags.OK + " Visualizer gestoppt\n", WARN);
            return;
        }
        input.ask("Interface (leer = automatisch erkannt):", raw ->
                startWithInterface(output, raw.isBlank() ? ActiveInterfaceDetector.detect() : raw.trim()));
    }

    static void startWithInterface(GuiOutputPanel output, String iface) {
        TrafficVisualizer.getInstance().start(iface);
        AuditLogger.getInstance().log("TRAFFIC_VIS_START", iface);
        embedPanel(output);
        output.appendText("  " + StatusTags.OK + " Visualizer aktiv auf \"" + iface + "\"\n", ACCENT2);
    }

    private static void embedPanel(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> {
            TrafficVisualizerPanel panel = new TrafficVisualizerPanel(BG);
            JTextPane pane = output.getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(panel);
            output.appendText("\n", FG);
        });
    }
}