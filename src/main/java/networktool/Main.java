package main.java.networktool;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageLocations;

import javax.swing.*;

/**
 * Einstiegspunkt der Anwendung.
 *
 * Speicherorte (siehe {@link StorageLocations}): Nutzerkonten, Logs und
 * Netzwerkdaten liegen in getrennten Unterordnern von saves/, nicht mehr
 * in einem gemeinsamen "data"-Verzeichnis.
 *
 * Sicherheit:
 *   1. AuditLogger/DebugLogger/UserAuth werden mit ihren jeweiligen
 *      Datenverzeichnissen initialisiert.
 *   2. Standard-Konten (admin/user1) werden bei Bedarf angelegt; UserAuth warnt
 *      dabei über DebugLogger, falls Default-Zugangsdaten noch aktiv sind.
 *   3. Login-Dialog erscheint vor dem GUI-Start.
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