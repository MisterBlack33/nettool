package main.java.networktool;

import main.java.networktool.cli.MenuHandler;
import main.java.networktool.cli.MenuPrinter;
import main.java.networktool.gui.GUI;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.LoginDialog;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Einstiegspunkt der Anwendung.
 *
 * Startmodi:
 *   - Standard (kein Argument): Swing-GUI
 *   - {@code --cli}:            Interaktives CLI-Menü
 *
 * Sicherheit:
 *   1. AuditLogger und UserAuth werden mit dem txt-Verzeichnis initialisiert.
 *   2. Login-Dialog erscheint vor dem GUI-Start.
 *   3. Alle Aktionen werden im AuditLogger protokolliert.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        // txt-Verzeichnis ermitteln
        Path txtDir = StorageUtils.resolveTxtDir();

        // Security-Subsysteme initialisieren
        AuditLogger.getInstance().init(txtDir);
        UserAuth.getInstance().init(txtDir);

        if (isCliMode(args)) {
            runCli(txtDir);
        } else {
            runGui(txtDir);
        }
    }

    // ── GUI-Modus ─────────────────────────────────────────────────────────

    private static void runGui(Path txtDir) {
        // Look & Feel zuerst setzen (vor jedem Swing-Aufruf)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

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

    // ── CLI-Modus ─────────────────────────────────────────────────────────

    private static void runCli(Path txtDir) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== NetTool v3  –  CLI-Modus ===");

        // CLI-Login
        if (!cliLogin(scanner)) {
            System.out.println("Anmeldung fehlgeschlagen. Programm wird beendet.");
            System.exit(1);
        }

        AuditLogger.getInstance().log("APP_START", "CLI");

        MenuHandler handler = new MenuHandler(scanner);
        while (true) {
            MenuPrinter.print();
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                handler.handle(choice);
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe – bitte eine Zahl eingeben.");
            }
        }
    }

    /**
     * CLI-Login-Schleife. Gibt true zurück bei Erfolg.
     * Legt automatisch den ersten Benutzer an wenn noch keiner existiert.
     */
    private static boolean cliLogin(Scanner scanner) {
        UserAuth auth = UserAuth.getInstance();

        if (!auth.hasUsers()) {
            System.out.println("\nKein Benutzer vorhanden. Ersten Benutzer anlegen:");
            System.out.print("  Benutzername: ");
            String username = scanner.nextLine().trim();
            System.out.print("  Passwort:     ");
            String pw = scanner.nextLine().trim();
            System.out.print("  Passwort wdh: ");
            String pw2 = scanner.nextLine().trim();
            if (!pw.equals(pw2)) {
                System.out.println("Passwörter stimmen nicht überein.");
                return false;
            }
            if (!auth.createUser(username, pw)) {
                System.out.println("Fehler beim Anlegen des Benutzers.");
                return false;
            }
            auth.authenticate(username, pw);
            AuditLogger.getInstance().log("USER_CREATED", username);
            AuditLogger.getInstance().log("LOGIN", username);
            System.out.println("Benutzer angelegt und eingeloggt als: " + username);
            return true;
        }

        // Vorhandene Benutzer: Login
        int attempts = 0;
        while (attempts < 3) {
            System.out.print("\n  Benutzername: ");
            String username = scanner.nextLine().trim();
            System.out.print("  Passwort:     ");
            String password = scanner.nextLine().trim();

            if (auth.authenticate(username, password)) {
                AuditLogger.getInstance().log("LOGIN", username);
                System.out.println("\n  Willkommen, " + username + "!");
                return true;
            }
            attempts++;
            System.out.println("  Ungültige Anmeldedaten. (" + attempts + "/3)");
            AuditLogger.getInstance().log("LOGIN_FAILED", username);
            // Kurze Pause gegen Brute-Force im CLI
            try { Thread.sleep(1_000L * attempts); } catch (InterruptedException ignored) {}
        }
        AuditLogger.getInstance().log("LOGIN_BLOCKED", "Zu viele Fehlversuche");
        return false;
    }

    private static boolean isCliMode(String[] args) {
        return args.length > 0 && args[0].equalsIgnoreCase("--cli");
    }
}