package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import networktool.logic.sonify.TrafficSonifier;
import networktool.security.AuditLogger;

import static networktool.theme.GuiTheme.*;

/** Sidebar-Aktion fÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼r den Netzwerk-Sonifier (MenÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼-ID "24"). */
final class GuiSonifyActions {

    private GuiSonifyActions() {}

    static void toggle(GuiInputPanel input, GuiOutputPanel output) {
        TrafficSonifier sonifier = TrafficSonifier.getInstance();
        if (sonifier.isActive()) {
            sonifier.stop();
            AuditLogger.getInstance().log("SONIFY_STOP", "");
            output.appendText("  ÃƒÆ’Ã‚Â°Ãƒâ€¦Ã‚Â¸ÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ Sonify gestoppt\n", WARN);
            return;
        }
        input.ask("Interface (z.B. eth0, leer = eth0):", iface -> {
            String name = iface.isBlank() ? "eth0" : iface.trim();
            sonifier.start(name);
            AuditLogger.getInstance().log("SONIFY_START", name);
            output.appendText("  ÃƒÆ’Ã‚Â°Ãƒâ€¦Ã‚Â¸Ãƒâ€¦Ã‚Â½Ãƒâ€šÃ‚Âµ Sonify aktiv auf \"" + name + "\"\n", ACCENT2);
        });
    }
}

