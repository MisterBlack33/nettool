package main.java.networktool.gui.components;

import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.sonify.TrafficSonifier;
import main.java.networktool.security.AuditLogger;

import static main.java.networktool.theme.GuiTheme.*;

/** Sidebar-Aktion für den Netzwerk-Sonifier (Menü-ID "24"). */
final class GuiSonifyActions {

    private GuiSonifyActions() {}

    static void toggle(GuiInputPanel input, GuiOutputPanel output) {
        TrafficSonifier sonifier = TrafficSonifier.getInstance();
        if (sonifier.isActive()) {
            sonifier.stop();
            AuditLogger.getInstance().log("SONIFY_STOP", "");
            output.appendText("  ⏹ Sonify gestoppt\n", WARN);
            return;
        }
        input.ask("Interface (z.B. eth0, leer = eth0):", iface -> {
            String name = iface.isBlank() ? "eth0" : iface.trim();
            sonifier.start(name);
            AuditLogger.getInstance().log("SONIFY_START", name);
            output.appendText("  🎵 Sonify aktiv auf \"" + name + "\"\n", ACCENT2);
        });
    }
}

