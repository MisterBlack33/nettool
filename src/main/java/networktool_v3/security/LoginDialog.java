package main.java.networktool_v3.security;

import main.java.networktool_v3.gui.GUI;
import main.java.networktool_v3.gui.GuiLoginRateLimiter;
import main.java.networktool_v3.gui.GuiTheme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Anmelde-Dialog – erscheint beim Programmstart.
 * Integriert GuiLoginRateLimiter (max. 5 Versuche, 30s Sperre).
 */
public final class LoginDialog extends JDialog {

    private boolean authenticated = false;
    private static final Color DIALOG_BG = new Color(0x0F, 0x13, 0x10);
    private static final Color HEADER_BG = new Color(0x0A, 0x0E, 0x0B);
    private static final Color INPUT_BG  = new Color(0x18, 0x1C, 0x1A);

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
        if (!auth.hasUsers()) dlg.buildRegisterScreen(auth, true);
        else                  dlg.buildLoginScreen(auth);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        GraphicsConfiguration gc = dlg.getGraphicsConfiguration();
        if (gc != null) GUI.setLoginMonitor(gc.getDevice());
        return dlg.authenticated;
    }

    // ── Anmelden ──────────────────────────────────────────────────────────

    private void buildLoginScreen(UserAuth auth) {
        setTitle("NetTool – Anmelden");
        getContentPane().removeAll();

        JPanel root = bg(new BorderLayout());
        root.add(buildHeader("Anmelden", null), BorderLayout.NORTH);

        JPanel form = bg(new GridBagLayout());
        form.setBorder(new EmptyBorder(28, 40, 12, 40));
        GridBagConstraints gc = defaultGc();

        List<String> users = auth.listUsernames();
        Component userComp;
        JComboBox<String> userBox   = null;
        JTextField        userField = null;

        if (users.size() == 1) {
            userField = inputField(users.get(0), 260);
            userField.setEditable(false);
            userField.setForeground(GuiTheme.FG_DIM);
            userComp = userField;
        } else {
            userBox  = inputCombo(users, 260);
            userComp = userBox;
        }

        JPasswordField pwField  = pwField(260);
        JLabel         errLabel = errLbl();
        JButton        loginBtn = mainBtn("Anmelden");

        addFormRow(form, gc, 0, "Benutzername", userComp);
        addFormRow(form, gc, 1, "Passwort",     pwField);
        addSpan   (form, gc, 2, errLabel);
        root.add(form, BorderLayout.CENTER);

        JPanel footer = bg(new BorderLayout());
        footer.setBorder(new EmptyBorder(0, 40, 24, 40));
        JButton newAccBtn = linkBtn("+ Neues Konto anlegen");
        newAccBtn.addActionListener(e -> { buildRegisterScreen(auth, false); repack(); });
        footer.add(newAccBtn, BorderLayout.WEST);
        JPanel btns = bg(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton quitBtn = secondBtn("Beenden");
        btns.add(quitBtn); btns.add(loginBtn);
        footer.add(btns, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        final JComboBox<String> finalBox   = userBox;
        final JTextField        finalField = userField;

        // Rate-Limit: Sperre prüfen und Timer starten falls aktiv
        updateLoginButtonState(loginBtn, errLabel);

        Runnable doLogin = () -> {
            if (GuiLoginRateLimiter.isLocked()) {
                errLabel.setText("Gesperrt – noch " + GuiLoginRateLimiter.remainingSeconds() + "s");
                return;
            }
            String u = finalBox != null ? (String) finalBox.getSelectedItem()
                    : finalField != null ? finalField.getText().trim() : "";
            String p = new String(pwField.getPassword());
            if (u == null || u.isBlank()) { shake(errLabel, "Benutzername fehlt."); return; }
            if (auth.authenticate(u, p)) {
                GuiLoginRateLimiter.recordSuccess();
                AuditLogger.getInstance().log("LOGIN", u);
                authenticated = true;
                dispose();
            } else {
                boolean locked = GuiLoginRateLimiter.recordFailure();
                AuditLogger.getInstance().log("LOGIN_FAILED", u);
                pwField.setText("");
                pwField.requestFocus();
                if (locked) {
                    errLabel.setText("Zu viele Versuche – " + GuiLoginRateLimiter.remainingSeconds() + "s gesperrt.");
                    loginBtn.setEnabled(false);
                    startLockoutTimer(loginBtn, errLabel);
                } else {
                    int rem = GuiLoginRateLimiter.MAX_ATTEMPTS - GuiLoginRateLimiter.getAttempts();
                    shake(errLabel, "Falsches Passwort. (" + rem + " Versuch(e) verbleibend)");
                }
            }
        };

        loginBtn.addActionListener(e -> doLogin.run());
        pwField.addActionListener (e -> doLogin.run());
        quitBtn.addActionListener (e -> System.exit(0));
        SwingUtilities.invokeLater(() ->
                (users.size() == 1 ? pwField : userComp).requestFocus());
    }

    /** Aktualisiert Button-Zustand beim Öffnen (falls Sperre noch aktiv). */
    private void updateLoginButtonState(JButton loginBtn, JLabel errLabel) {
        if (GuiLoginRateLimiter.isLocked()) {
            loginBtn.setEnabled(false);
            errLabel.setText("Gesperrt – noch " + GuiLoginRateLimiter.remainingSeconds() + "s");
            startLockoutTimer(loginBtn, errLabel);
        }
    }

    /** Tickt jede Sekunde, entsperrt wenn Zeit abgelaufen. */
    private void startLockoutTimer(JButton loginBtn, JLabel errLabel) {
        Timer t = new Timer(1000, null);
        t.addActionListener(e -> {
            if (!GuiLoginRateLimiter.isLocked()) {
                t.stop();
                loginBtn.setEnabled(true);
                errLabel.setText(" ");
            } else {
                errLabel.setText("Gesperrt – noch " + GuiLoginRateLimiter.remainingSeconds() + "s");
            }
        });
        t.start();
    }

    // ── Registrieren ──────────────────────────────────────────────────────

    private void buildRegisterScreen(UserAuth auth, boolean isFirst) {
        setTitle("NetTool – Konto erstellen");
        getContentPane().removeAll();

        JPanel root = bg(new BorderLayout());
        root.add(buildHeader("Konto erstellen",
                        isFirst ? "Lege deinen ersten Account an." : "Neuen Benutzer anlegen."),
                BorderLayout.NORTH);

        JPanel form = bg(new GridBagLayout());
        form.setBorder(new EmptyBorder(28, 40, 12, 40));
        GridBagConstraints gc = defaultGc();

        JTextField     userField = inputField("", 260);
        JPasswordField pw1       = pwField(260);
        JPasswordField pw2       = pwField(260);
        JLabel         errLabel  = errLbl();

        JProgressBar strength = new JProgressBar(0, 4);
        strength.setStringPainted(false);
        strength.setBorderPainted(false);
        strength.setPreferredSize(new Dimension(260, 3));
        strength.setForeground(GuiTheme.WARN);

        pw1.getDocument().addDocumentListener(docListener(() -> {
            String pw = new String(pw1.getPassword());
            int s = 0;
            if (pw.length() >= 8)               s++;
            if (pw.matches(".*[A-Z].*"))        s++;
            if (pw.matches(".*[0-9].*"))        s++;
            if (pw.matches(".*[^A-Za-z0-9].*")) s++;
            strength.setValue(s);
            strength.setForeground(s <= 1 ? GuiTheme.WARN
                    : s == 2 ? new Color(0xFF,0xA0,0x30)
                    : s == 3 ? GuiTheme.ACCENT : GuiTheme.ACCENT2);
        }));

        addFormRow(form, gc, 0, "Benutzername",    userField);
        addFormRow(form, gc, 1, "Passwort",        pw1);
        addFormRow(form, gc, 2, "Passwort (wdh.)", pw2);
        GridBagConstraints sc = defaultGc();
        sc.gridx = 1; sc.gridy = 3; sc.insets = new Insets(4, 0, 0, 0);
        form.add(strength, sc);
        addSpan(form, gc, 4, errLabel);
        root.add(form, BorderLayout.CENTER);

        JPanel footer = bg(new BorderLayout());
        footer.setBorder(new EmptyBorder(0, 40, 24, 40));
        if (!isFirst) {
            JButton backBtn = linkBtn("← Zurück zur Anmeldung");
            backBtn.addActionListener(e -> { buildLoginScreen(auth); repack(); });
            footer.add(backBtn, BorderLayout.WEST);
        }
        JPanel btns = bg(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton quitBtn   = secondBtn("Beenden");
        JButton createBtn = mainBtn("Konto erstellen");
        btns.add(quitBtn); btns.add(createBtn);
        footer.add(btns, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        Runnable doCreate = () -> {
            String u  = userField.getText().trim();
            String p1 = new String(pw1.getPassword());
            String p2 = new String(pw2.getPassword());
            if (u.length() < 3)  { shake(errLabel, "Benutzername mind. 3 Zeichen."); return; }
            if (u.contains(" ")) { shake(errLabel, "Benutzername ohne Leerzeichen."); return; }
            if (p1.length() < 6) { shake(errLabel, "Passwort mind. 6 Zeichen."); return; }
            if (!p1.equals(p2))  { shake(errLabel, "Passwörter stimmen nicht überein."); pw2.setText(""); return; }
            if (!auth.createUser(u, p1)) { shake(errLabel, "Benutzername bereits vergeben."); return; }
            auth.authenticate(u, p1);
            AuditLogger.getInstance().log("USER_CREATED", u);
            AuditLogger.getInstance().log("LOGIN", u);
            authenticated = true;
            dispose();
        };

        createBtn.addActionListener(e -> doCreate.run());
        pw2.addActionListener      (e -> doCreate.run());
        quitBtn.addActionListener  (e -> System.exit(0));
        SwingUtilities.invokeLater(userField::requestFocus);
    }

    // ── UI-Hilfsmethoden ─────────────────────────────────────────────────

    private JPanel buildHeader(String title, String sub) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HEADER_BG);
        p.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, GuiTheme.BORDER),
                new EmptyBorder(20, 40, 18, 40)));
        JLabel logo = new JLabel("// NetTool  v3");
        logo.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        logo.setForeground(GuiTheme.FG_DIM);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 20));
        titleLbl.setForeground(GuiTheme.ACCENT);
        titleLbl.setBorder(new EmptyBorder(8, 0, sub != null ? 4 : 0, 0));
        p.add(logo, BorderLayout.NORTH);
        p.add(titleLbl, BorderLayout.CENTER);
        if (sub != null) {
            JLabel subLbl = new JLabel(sub);
            subLbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
            subLbl.setForeground(GuiTheme.FG_DIM);
            p.add(subLbl, BorderLayout.SOUTH);
        }
        return p;
    }

    private static void addFormRow(JPanel f, GridBagConstraints gc, int row, String label, Component field) {
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        gc.fill  = GridBagConstraints.NONE;
        gc.insets = new Insets(row == 0 ? 0 : 14, 0, 0, 14);
        f.add(fldLbl(label), gc);
        gc.gridx = 1; gc.weightx = 1;
        gc.fill  = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(row == 0 ? 0 : 14, 0, 0, 0);
        f.add(field, gc);
    }

    private static void addSpan(JPanel f, GridBagConstraints gc, int row, Component c) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        gc.insets = new Insets(8, 0, 0, 0);
        f.add(c, gc);
        gc.gridwidth = 1;
    }

    private static GridBagConstraints defaultGc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        return gc;
    }

    private static JLabel fldLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        l.setForeground(GuiTheme.FG_DIM);
        return l;
    }

    private static JTextField inputField(String val, int w) {
        JTextField f = new JTextField(val);
        f.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        f.setForeground(GuiTheme.FG); f.setBackground(INPUT_BG);
        f.setCaretColor(GuiTheme.ACCENT);
        f.setBorder(new CompoundBorder(new LineBorder(GuiTheme.BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        f.setPreferredSize(new Dimension(w, 34));
        return f;
    }

    private static JPasswordField pwField(int w) {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        f.setForeground(GuiTheme.FG); f.setBackground(INPUT_BG);
        f.setCaretColor(GuiTheme.ACCENT);
        f.setBorder(new CompoundBorder(new LineBorder(GuiTheme.BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        f.setPreferredSize(new Dimension(w, 34));
        return f;
    }

    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> inputCombo(List<T> items, int w) {
        JComboBox<T> b = new JComboBox<>(items.toArray((T[]) new Object[0]));
        b.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        b.setBackground(INPUT_BG);
        b.setPreferredSize(new Dimension(w, 34));
        return b;
    }

    private static JButton mainBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        b.setForeground(Color.BLACK); b.setBackground(GuiTheme.ACCENT);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(8, 22, 8, 22));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton secondBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        b.setForeground(GuiTheme.FG_DIM); b.setBackground(INPUT_BG);
        b.setBorder(new CompoundBorder(new LineBorder(GuiTheme.BORDER, 1), new EmptyBorder(7, 16, 7, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton linkBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        b.setForeground(GuiTheme.INFO);
        b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(GuiTheme.ACCENT); }
            public void mouseExited (MouseEvent e) { b.setForeground(GuiTheme.INFO); }
        });
        return b;
    }

    private static JLabel errLbl() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        l.setForeground(GuiTheme.WARN);
        return l;
    }

    private JPanel bg(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(DIALOG_BG);
        return p;
    }

    private void repack() {
        pack();
        setMinimumSize(new Dimension(440, getHeight()));
        setLocationRelativeTo(null);
    }

    private static void shake(JLabel lbl, String msg) {
        lbl.setText(msg);
        Window win = SwingUtilities.getWindowAncestor(lbl);
        if (win == null) return;
        Point orig = win.getLocation();
        int[] offs = {-7,7,-5,5,-3,3,-1,1,0};
        int[] i    = {0};
        Timer t = new Timer(28, null);
        t.addActionListener(e -> {
            if (i[0] >= offs.length) { t.stop(); win.setLocation(orig); return; }
            win.setLocation(orig.x + offs[i[0]++], orig.y);
        });
        t.start();
    }

    private static javax.swing.event.DocumentListener docListener(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate (javax.swing.event.DocumentEvent e) { r.run(); }
            public void removeUpdate (javax.swing.event.DocumentEvent e) { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        };
    }
}