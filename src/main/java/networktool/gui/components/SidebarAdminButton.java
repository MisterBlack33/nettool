package main.java.networktool.gui.components;

import main.java.networktool.gui.login.LoginShakeEffect;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * "GET ADMIN"-Button der Sidebar: fragt per Passwort-Dialog eine
 * Admin-Freischaltung der laufenden Session an (siehe UserAuth.grantSessionAdmin).
 */
final class SidebarAdminButton {

    private static final String LABEL_LOCKED  = "GET ADMIN";
    private static final String LABEL_GRANTED = "ADMIN AKTIV";

    private SidebarAdminButton() {}

    static JButton build(Runnable onAdminGranted) {
        JButton btn = new JButton(LABEL_LOCKED);
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        btn.setForeground(ACCENT);
        btn.setBackground(BTN_BG);
        btn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 9, 4, 9)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showPasswordDialog(btn, onAdminGranted));
        return btn;
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    private static void showPasswordDialog(JButton triggerBtn, Runnable onAdminGranted) {
        JDialog dlg = new JDialog((Frame) null, "Admin-Freischaltung", true);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(PANEL_BG);

        JPasswordField pwField = buildPasswordField();
        JLabel errLabel = buildErrorLabel();
        JButton confirmBtn = buildConfirmButton();

        Runnable attempt = () -> attemptGrant(dlg, pwField, errLabel, triggerBtn, onAdminGranted);
        confirmBtn.addActionListener(e -> attempt.run());
        pwField.addActionListener(e -> attempt.run());

        dlg.setContentPane(buildContent(pwField, errLabel, confirmBtn));
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(pwField::requestFocus);
        dlg.setVisible(true);
    }

    private static JPanel buildContent(JPasswordField pwField, JLabel errLabel, JButton confirmBtn) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PANEL_BG);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));

        JLabel title = new JLabel("Admin-Passwort eingeben");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        pwField.setAlignmentX(Component.LEFT_ALIGNMENT);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(confirmBtn);

        root.add(title);
        root.add(Box.createVerticalStrut(10));
        root.add(pwField);
        root.add(Box.createVerticalStrut(6));
        root.add(errLabel);
        root.add(Box.createVerticalStrut(10));
        root.add(btnRow);
        return root;
    }

    private static JPasswordField buildPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        field.setForeground(FG);
        field.setBackground(BTN_BG);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        field.setPreferredSize(new Dimension(220, 34));
        field.setMaximumSize(new Dimension(220, 34));
        return field;
    }

    private static JLabel buildErrorLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        l.setForeground(WARN);
        return l;
    }

    private static JButton buildConfirmButton() {
        JButton b = new JButton("Freischalten");
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(Color.BLACK);
        b.setBackground(ACCENT);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Freischaltungs-Logik ──────────────────────────────────────────────

    private static void attemptGrant(JDialog dlg, JPasswordField pwField, JLabel errLabel,
                                     JButton triggerBtn, Runnable onAdminGranted) {
        String pw = new String(pwField.getPassword());
        boolean ok = UserAuth.getInstance().grantSessionAdmin(pw);
        if (ok) {
            AuditLogger.getInstance().log("GET_ADMIN", "User");
            triggerBtn.setText(LABEL_GRANTED);
            triggerBtn.setEnabled(false);
            dlg.dispose();
            onAdminGranted.run();
        } else {
            AuditLogger.getInstance().log("GET_ADMIN_FAILED", "User");
            LoginShakeEffect.shake(errLabel, "Falsches Passwort.");
            pwField.setText("");
        }
    }
}