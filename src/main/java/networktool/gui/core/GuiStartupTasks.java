package networktool.gui.core;

import networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.analysis.OuiUpdater;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.NetworkStore;

import networktool.theme.GuiTheme;

/**
 * Hintergrundaufgaben, die direkt nach dem Anzeigen des Hauptfensters
 * angestoßen werden (Benutzer-Banner, OUI-Datenbank, Nachrichten-Listener,
 * verzögerter SecurityMonitor-Start).
 */
final class GuiStartupTasks {

    private GuiStartupTasks() {}

    static void run(GuiOutputPanel outputPanel) {
        printUserBanner(outputPanel);

        MessageSender.startListener();
        OuiUpdater.initAsync(NetworkStore.getInstance().dataDir);

        // SecurityMonitor nach 5 s passiv starten
        new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!SecurityMonitor.getInstance().isActive())
                SecurityMonitor.getInstance().start("");
        }, "SecurityMonitor-Init").start();
    }

    private static void printUserBanner(GuiOutputPanel outputPanel) {
        String user   = UserAuth.getInstance().getCurrentUser();
        boolean admin = UserAuth.getInstance().isAdmin();
        if (user == null) return;
        String roleLabel = admin ? "  [admin]" : "  [user]";
        outputPanel.appendText("  Eingeloggt als: " + user + roleLabel + "\n\n",
                admin ? GuiTheme.ACCENT : GuiTheme.ACCENT2);
    }
}
