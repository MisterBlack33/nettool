package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.LoginFormBuilder.INPUT_BG;

/** Button-Fabrik für Login-/Registrierungsformulare. */
final class LoginButtons {

    private LoginButtons() {}

    static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        btn.setForeground(Color.BLACK);
        btn.setBackground(ACCENT);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(8, 22, 8, 22));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        btn.setForeground(FG_DIM);
        btn.setBackground(INPUT_BG);
        btn.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JButton linkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        btn.setForeground(INFO);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(ACCENT); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(INFO); }
        });
        return btn;
    }
}