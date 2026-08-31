package main.java.networktool;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Einstiegspunkt der Anwendung.
 *
 * Sicherheit:
 *   1. AuditLogger/DebugLogger/UserAuth werden mit dem Datenverzeichnis initialisiert.
 *   2. Standard-Konten (admin/user1) werden bei Bedarf angelegt; UserAuth warnt
 *      dabei über DebugLogger, falls Default-Zugangsdaten noch aktiv sind.
 *   3. Login-Dialog erscheint vor dem GUI-Start.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path dataDir = StorageUtils.resolveDataDir();
        AuditLogger.getInstance().init(dataDir);
        DebugLogger.getInstance().init(dataDir);
        UserAuth.getInstance().init(dataDir);
        UserAuth.getInstance().seedDefaultUsers();
        runGui(dataDir);
    }

    private static void runGui(Path dataDir) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            boolean ok = LoginDialog.show(UserAuth.getInstance());
            if (!ok) {
                System.exit(0);
            }
            AuditLogger.getInstance().log("APP_START", "GUI");
            new GUI();
        });
    }
}