package main.java.networktool.security;

import main.java.networktool.gui.GUI;
import main.java.networktool.gui.GuiLoginRateLimiter;
import main.java.networktool.gui.LoginScreens;
import main.java.networktool.gui.RegisterScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Anmelde-Dialog – erscheint beim Programmstart.
 * UI-Aufbau delegiert an {@link LoginScreens}; diese Klasse
 * verantwortet nur Authentifizierungs-Logik und Rate-Limiting.
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
        else                  dlg.showLogin(auth);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        GraphicsConfiguration gc = dlg.getGraphicsConfiguration();
        if (gc != null) GUI.setLoginMonitor(gc.getDevice());
        return dlg.authenticated;
    }

    // ── Bildschirm-Wechsel ────────────────────────────────────────────────

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

    private void attemptCreate(UserAuth auth, String username, String pw1, String pw2) {
        if (username.length() < 3 || username.contains(" ")) return;
        if (pw1.length() < 6 || !pw1.equals(pw2)) return;
        if (!auth.createUser(username, pw1)) return;

        auth.authenticate(username, pw1);
        AuditLogger.getInstance().log("USER_CREATED", username);
        AuditLogger.getInstance().log("LOGIN", username);
        authenticated = true;
        dispose();
    }
}