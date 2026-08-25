package main.java.networktool.gui.login;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.login.LoginFormBuilder.*;
import static main.java.networktool.gui.login.LoginButtons.*;

/**
 * Startbildschirm wenn bereits Nutzer existieren: fragt explizit
 * "Anmelden" oder "Registrieren" ab, statt automatisch zum Login zu springen.
 */
public final class LoginChoiceScreen {

    private LoginChoiceScreen() {}

    public static JPanel build(Runnable onLogin, Runnable onRegister, Runnable onQuit) {
        JPanel root = bgPanel(new BorderLayout());
        root.add(buildHeader("Willkommen zurück", "Wie möchtest du fortfahren?"), BorderLayout.NORTH);

        JPanel body = bgPanel(new GridLayout(2, 1, 0, 12));
        body.setBorder(new EmptyBorder(28, 40, 12, 40));

        JButton loginBtn = primaryButton("Anmelden");
        loginBtn.addActionListener(e -> onLogin.run());

        JButton registerBtn = secondaryButton("Neues Konto registrieren");
        registerBtn.addActionListener(e -> onRegister.run());

        body.add(loginBtn);
        body.add(registerBtn);
        root.add(body, BorderLayout.CENTER);

        JPanel footer = bgPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setBorder(new EmptyBorder(0, 40, 24, 40));
        JButton quitBtn = secondaryButton("Beenden");
        quitBtn.addActionListener(e -> onQuit.run());
        footer.add(quitBtn);
        root.add(footer, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(loginBtn::requestFocus);
        return root;
    }
}