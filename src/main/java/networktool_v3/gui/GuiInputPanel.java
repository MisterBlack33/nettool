package main.java.networktool_v3.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.function.Consumer;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Eingabezeile am unteren Rand der GUI.
 *
 * Über {@link #requestInput} wird ein Prompt angezeigt und der übergebene
 * Callback nach Bestätigung (Enter oder Button) aufgerufen.
 */
public class GuiInputPanel {

    @FunctionalInterface
    public interface InputCallback {
        void onInput(String value);
    }

    private final JTextField     field;
    private final JLabel         statusLabel;
    private final GuiOutputPanel outputPanel;
    private volatile InputCallback pending = null;

    public GuiInputPanel(JLabel statusLabel, GuiOutputPanel outputPanel) {
        this.statusLabel = statusLabel;
        this.outputPanel = outputPanel;
        this.field       = new JTextField();
    }

    // ── Panel-Builder ─────────────────────────────────────────────────────

    public JPanel buildPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(PANEL_BG);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(10, 14, 10, 14)
        ));
        p.add(buildArrowLabel(), BorderLayout.WEST);
        p.add(buildTextField(),  BorderLayout.CENTER);
        p.add(buildSendButton(), BorderLayout.EAST);
        return p;
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    /** Zeigt einen Prompt an und ruft {@code cb} nach Eingabe auf. */
    public void requestInput(String prompt, InputCallback cb) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Eingabe: " + prompt);
            statusLabel.setForeground(ACCENT);
            field.setEnabled(true);
            field.setText("");
            field.requestFocus();
            outputPanel.appendText("  ▶ " + prompt + "\n", ACCENT);
            pending = cb;
        });
    }

    /** Kurzform für {@link #requestInput}, bequem in verketteten Dialogen. */
    public void ask(String prompt, Consumer<String> next) {
        requestInput(prompt, next::accept);
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private JLabel buildArrowLabel() {
        JLabel arrow = new JLabel("▶");
        arrow.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        arrow.setForeground(ACCENT);
        return arrow;
    }

    private JTextField buildTextField() {
        field.setFont(MONO);
        field.setForeground(FG);
        field.setBackground(BG);
        field.setCaretColor(ACCENT);
        field.setBorder(new EmptyBorder(2, 8, 2, 8));
        field.setEnabled(false);
        field.addActionListener(e -> submit());
        return field;
    }

    private JButton buildSendButton() {
        JButton btn = new JButton("ENTER");
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        btn.setForeground(ACCENT);
        btn.setBackground(BTN_BG);
        btn.setBorder(new CompoundBorder(
            new LineBorder(ACCENT, 1),
            new EmptyBorder(4, 14, 4, 14)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> submit());
        return btn;
    }

    private void submit() {
        String val = field.getText().trim();
        field.setText("");
        field.setEnabled(false);
        statusLabel.setText("Bereit");
        statusLabel.setForeground(FG_DIM);
        outputPanel.appendText("  " + val + "\n", FG);
        if (pending != null) {
            InputCallback cb = pending;
            pending = null;
            cb.onInput(val);
        }
    }
}
