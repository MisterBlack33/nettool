package main.java.networktool.gui.core;

import main.java.networktool.filter.OutputRendererRegistry;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.SecurityMonitor;
import main.java.networktool.security.UserAuth;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startet und beendet GUI-Instanzen: Neustart nach Theme-/Login-Wechsel
 * sowie der initiale Programmstart. Ausgelagert aus {@link GUI}.
 */
final class GuiRestartFlow {

    private static final Logger LOG = Logger.getLogger(GuiRestartFlow.class.getName());

    private GuiRestartFlow() {}

    static void restart(GUI current) {
        AuditLogger.getInstance().log("APP_RESTART", UserAuth.getInstance().getCurrentUser());
        SecurityMonitor.getInstance().stop();
        GraphicsDevice monitor = current.getGraphicsConfiguration().getDevice();
        OutputRendererRegistry.unregister(current);
        current.dispose();
        GUI.clearInstance();

        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            GUI.setLoginMonitor(monitor);
            boolean ok = LoginDialog.show(UserAuth.getInstance());
            if (!ok) System.exit(0);
            AuditLogger.getInstance().log("LOGIN_AFTER_RESTART", UserAuth.getInstance().getCurrentUser());
            new GUI();
        });
    }

    static void launch() {
        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            new GUI();
        });
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            LOG.log(Level.FINE, "System-Look-and-Feel konnte nicht gesetzt werden", e);
        }
    }
}
