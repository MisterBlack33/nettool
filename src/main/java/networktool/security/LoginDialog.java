package main.java.networktool.security;

import main.java.networktool.gui.core.GUI;
import main.java.networktool.gui.login.GuiLoginRateLimiter;
import main.java.networktool.gui.login.LoginScreens;
import main.java.networktool.gui.login.RegisterScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Anmelde-Dialog – erscheint beim Programmstart.
 *
 * Ablauf:
 *  - Keine Nutzer vorhanden  -> direkt Registrierung (logisch zwingend).
 *  - Nutzer vorhanden        -> explizite Auswahl "Anmelden" / "Registrieren".
 *
 * UI-Aufbau delegiert an {@link LoginScreens}/{@link RegisterScreen}; diese
 * Klasse verantwortet nur Bildschirm-Navigation, Authentifizierungs-Logik
 * und Rate-Limiting.
 */
public final class LoginDialog extends JDialog {

    private boolean authenticated = false;

    private LoginDialog() {
        super((Frame) null, "NetTool", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
    }

    public static boolean show(UserAuth auth) {
        GuiLoginRateLimiter.reset();
        LoginDialog dlg = new LoginDialog();
        if (!auth.hasUsers()) dlg.showRegister(auth, true);
        else                  dlg.showChoice(auth);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        GraphicsConfiguration gc = dlg.getGraphicsConfiguration();
        if (gc != null) GUI.setLoginMonitor(gc.getDevice());
        return dlg.authenticated;
    }

    // ── Bildschirm-Wechsel ────────────────────────────────────────────────

    /** Startbildschirm wenn bereits Nutzer existieren: explizite Wahl statt Auto-Entscheidung. */
    private void showChoice(UserAuth auth) {
        setTitle("NetTool – Willkommen");
        getContentPane().removeAll();
        setContentPane(main.java.networktool.gui.login.LoginChoiceScreen.build(
                () -> { showLogin(auth); repack(); },
                () -> { showRegister(auth, false); repack(); },
                () -> System.exit(0)));
        repack();
    }

    private void showLogin(UserAuth auth) {
        setTitle("NetTool – Anmelden");
        getContentPane().removeAll();
        JPanel screen = LoginScreens.buildLoginScreen(auth,
                (username, password) -> attemptLogin(auth, username, password),
                () -> { showRegister(auth, false); repack(); },
                () -> System.exit(0));
        setContentPane(screen);
        repack();
    }

    private void showRegister(UserAuth auth, boolean isFirst) {
        setTitle("NetTool – Konto erstellen");
        getContentPane().removeAll();
        JPanel screen = RegisterScreen.build(isFirst,
                (username, pw1, pw2) -> attemptCreate(auth, username, pw1, pw2),
                () -> { showLogin(auth); repack(); },
                () -> System.exit(0));
        setContentPane(screen);
        repack();
    }

    private void repack() {
        pack();
        setMinimumSize(new Dimension(440, getHeight()));
        setLocationRelativeTo(null);
    }

    // ── Authentifizierung ─────────────────────────────────────────────────

    private void attemptLogin(UserAuth auth, String username, String password) {
        if (GuiLoginRateLimiter.isLocked()) return;

        if (auth.authenticate(username, password)) {
            GuiLoginRateLimiter.recordSuccess();
            AuditLogger.getInstance().log("LOGIN", username);
            authenticated = true;
            dispose();
            return;
        }

        GuiLoginRateLimiter.recordFailure();
        AuditLogger.getInstance().log("LOGIN_FAILED", username);
    }

    /**
     * Legt ein neues Konto an.
     *
     * @return Fehlermeldung falls die Erstellung fehlschlägt, sonst {@code null}
     *         (vorher wurde bei ungültigem Passwort/Namen still ohne Feedback abgebrochen).
     */
    private String attemptCreate(UserAuth auth, String username, String pw1, String pw2) {
        if (username.length() < 3 || username.contains(" "))
            return "Benutzername: mind. 3 Zeichen, keine Leerzeichen.";
        if (!pw1.equals(pw2))
            return "Passwörter stimmen nicht überein.";
        if (!UserAuth.isStrongPassword(pw1))
            return "Passwort: mind. 8 Zeichen, mit Buchstabe und Ziffer.";
        if (!auth.createUser(username, pw1))
            return "Benutzername bereits vergeben.";

        auth.authenticate(username, pw1);
        AuditLogger.getInstance().log("USER_CREATED", username);
        AuditLogger.getInstance().log("LOGIN", username);
        authenticated = true;
        dispose();
        return null;
    }
}