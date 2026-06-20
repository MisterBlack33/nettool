package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

import static main.java.networktool.gui.GuiTheme.*;
import static main.java.networktool.gui.LoginFormBuilder.INPUT_BG;

/** Eingabefeld-Fabrik für Login-/Registrierungsformulare. */
final class LoginInputs {

    private LoginInputs() {}

    static JTextField inputField(String value, int width) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        field.setForeground(FG);
        field.setBackground(INPUT_BG);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        field.setPreferredSize(new Dimension(width, 34));
        return field;
    }

    static JPasswordField passwordField(int width) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        field.setForeground(FG);
        field.setBackground(INPUT_BG);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        field.setPreferredSize(new Dimension(width, 34));
        return field;
    }

    @SuppressWarnings("unchecked")
    static <T> JComboBox<T> comboBox(List<T> items, int width) {
        JComboBox<T> box = new JComboBox<>(items.toArray((T[]) new Object[0]));
        box.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        box.setBackground(INPUT_BG);
        box.setPreferredSize(new Dimension(width, 34));
        return box;
    }

    static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        label.setForeground(WARN);
        return label;
    }

    static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        label.setForeground(FG_DIM);
        return label;
    }
}