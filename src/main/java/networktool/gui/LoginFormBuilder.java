package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Header- und Root-Panel-Aufbau für Login/Register.
 * Eingabefelder siehe {@link LoginInputs}, Buttons siehe {@link LoginButtons}.
 */
final class LoginFormBuilder {

    static final Color DIALOG_BG = new Color(0x0F, 0x13, 0x10);
    static final Color HEADER_BG = new Color(0x0A, 0x0E, 0x0B);
    static final Color INPUT_BG  = new Color(0x18, 0x1C, 0x1A);

    private LoginFormBuilder() {}

    static JPanel buildHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(HEADER_BG);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(20, 40, 18, 40)));

        JLabel logo = new JLabel("// NetTool  v3");
        logo.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        logo.setForeground(FG_DIM);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 20));
        titleLabel.setForeground(ACCENT);
        titleLabel.setBorder(new EmptyBorder(8, 0, subtitle != null ? 4 : 0, 0));

        panel.add(logo, BorderLayout.NORTH);
        panel.add(titleLabel, BorderLayout.CENTER);

        if (subtitle != null) {
            JLabel sub = new JLabel(subtitle);
            sub.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
            sub.setForeground(FG_DIM);
            panel.add(sub, BorderLayout.SOUTH);
        }
        return panel;
    }

    static JPanel bgPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(DIALOG_BG);
        return panel;
    }
}