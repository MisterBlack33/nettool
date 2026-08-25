package main.java.networktool.gui.core;

import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.panels.audit.GuiAuditPanel;
import main.java.networktool.gui.panels.privacy.GuiPrivacyPanel;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;

import main.java.networktool.theme.GuiTheme;

/**
 * Zentraler Menü-Dispatch mit Admin-Check und SearchBar-Steuerung.
 *
 * Regeln:
 *  "09" → savedHostsPanel.show() ruft searchBar.show() intern
 *  alle anderen → searchBar.hide()
 *  "11" Fremdnetz → nur Admins
 *  "23" Audit-Log → nur Admins
 *  "30" Privatsphäre → GuiPrivacyPanel
 */
final class GuiMenuDispatch {

    private GuiMenuDispatch() {}

    static void handle(String id, GuiOutputPanel outputPanel, GuiSearchBar searchBar, GuiMenuHandler menuHandler) {
        if ("11".equals(id) && !UserAuth.getInstance().isAdmin()) {
            outputPanel.appendText("  ✕ Fremdnetz-Scanner: nur für Admins.\n", GuiTheme.WARN);
            return;
        }
        if ("23".equals(id)) {
            if (!UserAuth.getInstance().isAdmin()) {
                outputPanel.appendText("  ✕ Audit-Log: nur für Admins.\n", GuiTheme.WARN);
                return;
            }
            AuditLogger.getInstance().log("MENU", "23-AuditLog");
            GuiAuditPanel.show(outputPanel);
            return;
        }
        if ("30".equals(id)) {
            AuditLogger.getInstance().log("MENU", "30-Privacy");
            GuiPrivacyPanel.show(outputPanel);
            searchBar.hide(); // sicherheitshalber
            return;
        }

        // SearchBar: bei allen Menüpunkten außer 09 ausblenden.
        // Bei 09 übernimmt savedHostsPanel.show() die Aktivierung.
        if (!"09".equals(id)) {
            searchBar.hide();
        }

        menuHandler.handle(id);
    }
}
