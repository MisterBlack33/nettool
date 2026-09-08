package main.java.networktool.gui.components.scan;

import main.java.networktool.gui.components.TrafficSpectrogramPanel;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.sonify.ActiveInterfaceDetector;
import main.java.networktool.logic.visualize.TrafficVisualizer;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.util.StatusTags;

import javax.swing.*;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Sidebar-Aktion für die Spektrogramm-Ansicht des Traffic-Visualizers (Menü-ID "26").
 * Teilt sich die Datenerfassung mit {@link GuiTrafficVisualizerActions} (Menü-ID "25")
 * über die gemeinsame {@link TrafficVisualizer}-Instanz — nur die Darstellung
 * (Funktion 2: Spektrogramm statt Balken) unterscheidet sich.
 */
public final class GuiTrafficSpectrogramActions {

    private GuiTrafficSpectrogramActions() {}

    public static void toggle(GuiInputPanel input, GuiOutputPanel output) {
        TrafficVisualizer visualizer = TrafficVisualizer.getInstance();
        if (visualizer.isActive()) {
            visualizer.stop();
            AuditLogger.getInstance().log("TRAFFIC_SPECTRO_STOP", visualizer.getActiveInterface());
            output.appendText("  " + StatusTags.OK + " Spektrogramm gestoppt\n", WARN);
            return;
        }
        input.ask("Interface (leer = automatisch erkannt):", raw ->
                startWithInterface(output, raw.isBlank() ? ActiveInterfaceDetector.detect() : raw.trim()));
    }

    static void startWithInterface(GuiOutputPanel output, String iface) {
        TrafficVisualizer.getInstance().start(iface);
        AuditLogger.getInstance().log("TRAFFIC_SPECTRO_START", iface);
        embedPanel(output);
        output.appendText("  " + StatusTags.OK + " Spektrogramm aktiv auf \"" + iface + "\"\n", ACCENT2);
    }

    private static void embedPanel(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> {
            TrafficSpectrogramPanel panel = new TrafficSpectrogramPanel();
            JTextPane pane = output.getOutputPane();
            pane.setCaretPosition(pane.getDocument().getLength());
            pane.insertComponent(panel);
            output.appendText("\n", FG);
        });
    }
}