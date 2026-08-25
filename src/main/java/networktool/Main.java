package main.java.networktool;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Einstiegspunkt der Anwendung.
 *
 * Startmodi:
 *   - Standard (kein Argument): Swing-GUI
 *
 * Sicherheit:
 *   1. AuditLogger und UserAuth werden mit dem Datenverzeichnis initialisiert.
 *   2. Standard-Konten (admin/user1) werden bei Bedarf angelegt, damit beim
 *      ersten Start direkt die Wahl "Anmelden / Registrieren" erscheint statt
 *      zwingend zur Registrierung zu springen.
 *   3. Login-Dialog erscheint vor dem GUI-Start.
 *   4. Alle Aktionen werden im AuditLogger protokolliert.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path dataDir = StorageUtils.resolveDataDir();
        AuditLogger.getInstance().init(dataDir);
        UserAuth.getInstance().init(dataDir);
        UserAuth.getInstance().seedDefaultUsers();
        runGui(dataDir);
    }

    private static void runGui(Path dataDir) {
        // Look & Feel zuerst setzen (vor jedem Swing-Aufruf)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Login auf dem EDT ausführen
        SwingUtilities.invokeLater(() -> {
            boolean ok = LoginDialog.show(UserAuth.getInstance());
            if (!ok) {
                // Sollte durch System.exit(0) im Dialog bereits beendet worden sein,
                // aber als Sicherheit:
                System.exit(0);
            }
            // Erfolgreich eingeloggt → GUI starten
            AuditLogger.getInstance().log("APP_START", "GUI");
            new GUI();
        });
    }
}