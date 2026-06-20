package main.java.networktool.gui;

import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.LoginFormBuilder.*;
import static main.java.networktool.gui.LoginInputs.*;
import static main.java.networktool.gui.LoginButtons.*;
import static main.java.networktool.gui.LoginLayoutHelper.*;

/**
 * Baut den Anmeldebildschirm. Reine UI-Assemblierung;
 * Authentifizierungslogik bleibt in {@code LoginDialog}.
 * Registrierungsbildschirm siehe {@link RegisterScreen}.
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

        List<String> users = auth.listUsernames();
        Component userComp;
        JComboBox<String> userBox   = null;
        JTextField        userField = null;

        if (users.size() == 1) {
            userField = inputField(users.get(0), 260);
            userField.setEditable(false);
            userField.setForeground(FG_DIM);
            userComp = userField;
        } else {
            userBox  = comboBox(users, 260);
            userComp = userBox;
        }

        JPasswordField pwField  = passwordField(260);
        JLabel         errLabel = errorLabel();
        JButton        loginBtn = primaryButton("Anmelden");

        addFormRow(form, gc, 0, "Benutzername", userComp);
        addFormRow(form, gc, 1, "Passwort",     pwField);
        addSpanRow(form, gc, 2, errLabel);
        root.add(form, BorderLayout.CENTER);

        wireLoginActions(onLogin, userBox, userField, pwField, errLabel, loginBtn);
        root.add(buildFooter(onNewAccount, onQuit, loginBtn), BorderLayout.SOUTH);

        Component focusTarget = users.size() == 1 ? pwField : userComp;
        SwingUtilities.invokeLater(focusTarget::requestFocus);
        return root;
    }

    private static void wireLoginActions(LoginAttempt onLogin, JComboBox<String> userBox,
                                         JTextField userField, JPasswordField pwField,
                                         JLabel errLabel, JButton loginBtn) {
        LoginLockoutWatcher.attach(loginBtn, errLabel);

        Runnable doLogin = () -> {
            String username = resolveUsername(userBox, userField);
            String password = new String(pwField.getPassword());
            if (username == null || username.isBlank()) {
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

    private static String resolveUsername(JComboBox<String> userBox, JTextField userField) {
        if (userBox != null) return (String) userBox.getSelectedItem();
        if (userField != null) return userField.getText().trim();
        return "";
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