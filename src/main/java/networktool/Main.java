package main.java.networktool;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageLocations;

import javax.swing.*;

/**
 * Einstiegspunkt der Anwendung. Ablauf siehe main()/runGui().
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AuditLogger.getInstance().init(StorageLocations.logs());
        DebugLogger.getInstance().init(StorageLocations.logs());
        UserAuth.getInstance().init(StorageLocations.userData());
        UserAuth.getInstance().seedDefaultUsers();
        runGui();
    }

    private static void runGui() {
        applySystemLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            UserAuth.getInstance().authenticateAsStandardUser();
            AuditLogger.getInstance().log("APP_START", "GUI");
            new GUI();
        });
    }

    /** Fällt bei nicht verfügbarem System-Look-and-Feel auf das Swing-Default zurück. */
    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            DebugLogger.getInstance().log("WARN", "[Main] System-Look-and-Feel nicht verfügbar: " + e);
        }
    }
}