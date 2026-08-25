package main.java.networktool.gui.login;

import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import main.java.networktool.theme.GuiTheme;
import static main.java.networktool.theme.GuiTheme.*;
import static main.java.networktool.gui.login.LoginFormBuilder.*;
import static main.java.networktool.gui.login.LoginInputs.*;
import static main.java.networktool.gui.login.LoginButtons.*;
import static main.java.networktool.gui.login.LoginFormLayout.*;

/**
 * Baut den Anmeldebildschirm. Reine UI-Assemblierung;
 * Authentifizierungslogik bleibt in {@code LoginDialog}.
 * Registrierungsbildschirm siehe {@link RegisterScreen}.
 *
 * Benutzername wird immer manuell eingegeben (kein Auswahl-Dropdown mehr),
 * auch wenn mehrere Konten existieren.
 */
public final class LoginScreens {

    private LoginScreens() {}

    @FunctionalInterface public interface LoginAttempt { void run(String username, String password); }

    public static JPanel buildLoginScreen(UserAuth auth, LoginAttempt onLogin,
                                          Runnable onNewAccount, Runnable onQuit) {
        JPanel root = bgPanel(new BorderLayout());
        root.add(buildHeader("Anmelden", null), BorderLayout.NORTH);

        JPanel form = formPanel(GuiTheme.PANEL_BG);
        GridBagConstraints gc = defaultConstraints();

        JTextField     userField = inputField("", 260);
        JPasswordField pwField   = passwordField(260);
        JLabel         errLabel  = errorLabel();
        JButton        loginBtn  = primaryButton("Anmelden");

        addFormRow(form, gc, 0, "Benutzername", userField);
        addFormRow(form, gc, 1, "Passwort",     pwField);
        addSpanRow(form, gc, 2, errLabel);
        root.add(form, BorderLayout.CENTER);

        wireLoginActions(onLogin, userField, pwField, errLabel, loginBtn);
        root.add(buildFooter(onNewAccount, onQuit, loginBtn), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(userField::requestFocus);
        return root;
    }

    private static void wireLoginActions(LoginAttempt onLogin, JTextField userField,
                                         JPasswordField pwField, JLabel errLabel, JButton loginBtn) {
        LoginLockoutWatcher.attach(loginBtn, errLabel);

        Runnable doLogin = () -> {
            String username = userField.getText().trim();
            String password = new String(pwField.getPassword());
            if (username.isBlank()) {
                LoginShakeEffect.shake(errLabel, "Benutzername fehlt.");
                return;
            }
            onLogin.run(username, password);
            pwField.setText("");
            pwField.requestFocus();

            if (GuiLoginRateLimiter.isLocked()) {
                LoginLockoutWatcher.startCountdown(loginBtn, errLabel);
            }
        };
        loginBtn.addActionListener(e -> doLogin.run());
        pwField.addActionListener(e -> doLogin.run());
    }

    private static JPanel buildFooter(Runnable onNewAccount, Runnable onQuit, JButton loginBtn) {
        JPanel footer = bgPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(0, 40, 24, 40));

        JButton newAccBtn = linkButton("+ Neues Konto anlegen");
        newAccBtn.addActionListener(e -> onNewAccount.run());
        footer.add(newAccBtn, BorderLayout.WEST);

        JPanel buttons = bgPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton quitBtn = secondaryButton("Beenden");
        quitBtn.addActionListener(e -> onQuit.run());
        buttons.add(quitBtn);
        buttons.add(loginBtn);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }
}