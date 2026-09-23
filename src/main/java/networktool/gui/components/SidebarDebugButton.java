package main.java.networktool.gui.components;

import main.java.networktool.gui.core.GuiDebugMode;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import static main.java.networktool.theme.GuiTheme.*;

/** Admin-only toggle for the GUI debug simulation mode. */
final class SidebarDebugButton {

    private static final Color ACTIVE_BG = new Color(0x808080);

    private SidebarDebugButton() {}

    static JButton build(Runnable onToggle) {
        JButton button = new JButton("DEBUG MODUS");
        button.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            updateStyle(button);
            onToggle.run();
            updateStyle(button);
        });
        updateStyle(button);
        return button;
    }

    private static void updateStyle(JButton button) {
        boolean active = GuiDebugMode.isEnabled();
        button.setForeground(active ? new Color(0xD0, 0xD0, 0xD0) : ACCENT);
        button.setBackground(active ? ACTIVE_BG : BTN_BG);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(
                new LineBorder(active ? ACTIVE_BG : BORDER, 1),
                new EmptyBorder(4, 9, 4, 9)));
    }
}
