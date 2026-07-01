package networktool;

import networktool.cli.MenuHandler;
import networktool.cli.MenuPrinter;
import networktool.gui.GUI;
import networktool.security.AuditLogger;
import networktool.security.LoginDialog;
import networktool.security.UserAuth;
import networktool.storage.StorageUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

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

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Datenverzeichnis ermitteln (früher: "txt")
        Path dataDir = StorageUtils.resolveDataDir();

        // Security-Subsysteme initialisieren
        AuditLogger.getInstance().init(dataDir);
        UserAuth.getInstance().init(dataDir);

        if (isCliMode(args)) {
            runCli(dataDir);
        } else {
            runGui(dataDir);
        }
    }

    // ── GUI-Modus ─────────────────────────────────────────────────────────

    private static void runGui(Path dataDir) {
        // Look & Feel zuerst setzen (vor jedem Swing-Aufruf)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException unsupportedLaf) {
            LOGGER.log(Level.WARNING, "System Look & Feel not supported, using default", unsupportedLaf);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to set Look & Feel", exception);
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

    // ── CLI-Modus ─────────────────────────────────────────────────────────

    private static void runCli(Path dataDir) {
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
            try {
                Thread.sleep(1_000L * attempts);
            } catch (InterruptedException interruptedException) {
                LOGGER.log(Level.FINE, "Thread sleep interrupted during login", interruptedException);
                Thread.currentThread().interrupt();
            }
        }
        AuditLogger.getInstance().log("LOGIN_BLOCKED", "Zu viele Fehlversuche");
        return false;
    }

    private static boolean isCliMode(String[] args) {
        return args.length > 0 && args[0].equalsIgnoreCase("--cli");
    }
}