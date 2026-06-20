package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.LoginFormBuilder.*;
import static main.java.networktool.gui.LoginInputs.*;
import static main.java.networktool.gui.LoginButtons.*;
import static main.java.networktool.gui.LoginLayoutHelper.*;

/**
 * Baut den Registrierungsbildschirm inkl. Passwort-Stärkeanzeige.
 * Gegenstück zu {@link LoginScreens}.
 */
public final class RegisterScreen {

    private RegisterScreen() {}

    @FunctionalInterface public interface RegisterAttempt { void run(String username, String pw1, String pw2); }

    public static JPanel build(boolean isFirst, RegisterAttempt onCreate,
                               Runnable onBack, Runnable onQuit) {
        JPanel root = bgPanel(new BorderLayout());
        root.add(buildHeader("Konto erstellen",
                        isFirst ? "Lege deinen ersten Account an." : "Neuen Benutzer anlegen."),
                BorderLayout.NORTH);

        JPanel form = formPanel(GuiTheme.PANEL_BG);
        GridBagConstraints gc = defaultConstraints();

        JTextField     userField = inputField("", 260);
        JPasswordField pw1       = passwordField(260);
        JPasswordField pw2       = passwordField(260);
        JLabel         errLabel  = errorLabel();
        JProgressBar   strength  = buildStrengthBar();

        wireStrengthMeter(pw1, strength);

        addFormRow(form, gc, 0, "Benutzername",    userField);
        addFormRow(form, gc, 1, "Passwort",        pw1);
        addFormRow(form, gc, 2, "Passwort (wdh.)", pw2);
        addStrengthRow(form, strength);
        addSpanRow(form, gc, 4, errLabel);
        root.add(form, BorderLayout.CENTER);

        JButton createBtn = primaryButton("Konto erstellen");
        Runnable doCreate = () -> onCreate.run(
                userField.getText().trim(),
                new String(pw1.getPassword()),
                new String(pw2.getPassword()));
        createBtn.addActionListener(e -> doCreate.run());
        pw2.addActionListener(e -> doCreate.run());

        root.add(buildFooter(isFirst, onBack, onQuit, createBtn), BorderLayout.SOUTH);
        SwingUtilities.invokeLater(userField::requestFocus);
        return root;
    }

    private static JProgressBar buildStrengthBar() {
        JProgressBar bar = new JProgressBar(0, 4);
        bar.setStringPainted(false);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(260, 3));
        bar.setForeground(WARN);
        return bar;
    }

    private static void wireStrengthMeter(JPasswordField pw1, JProgressBar strength) {
        pw1.getDocument().addDocumentListener(docListener(() -> {
            int score = passwordScore(new String(pw1.getPassword()));
            strength.setValue(score);
            strength.setForeground(strengthColor(score));
        }));
    }

    private static int passwordScore(String pw) {
        int score = 0;
        if (pw.length() >= 8)               score++;
        if (pw.matches(".*[A-Z].*"))        score++;
        if (pw.matches(".*[0-9].*"))        score++;
        if (pw.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    private static Color strengthColor(int score) {
        return switch (score) {
            case 0, 1 -> WARN;
            case 2    -> new Color(0xFF, 0xA0, 0x30);
            case 3    -> ACCENT;
            default   -> ACCENT2;
        };
    }

    private static void addStrengthRow(JPanel form, JProgressBar strength) {
        GridBagConstraints sc = defaultConstraints();
        sc.gridx = 1; sc.gridy = 3; sc.insets = new Insets(4, 0, 0, 0);
        form.add(strength, sc);
    }

    private static JPanel buildFooter(boolean isFirst, Runnable onBack,
                                      Runnable onQuit, JButton createBtn) {
        JPanel footer = bgPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(0, 40, 24, 40));

        if (!isFirst) {
            JButton backBtn = linkButton("← Zurück zur Anmeldung");
            backBtn.addActionListener(e -> onBack.run());
            footer.add(backBtn, BorderLayout.WEST);
        }

        JPanel buttons = bgPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton quitBtn = secondaryButton("Beenden");
        quitBtn.addActionListener(e -> onQuit.run());
        buttons.add(quitBtn);
        buttons.add(createBtn);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private static javax.swing.event.DocumentListener docListener(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        };
    }
}