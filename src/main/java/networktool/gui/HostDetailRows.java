package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

/** Wiederverwendbare Zeilen-Layouts für Host-Detail-Tabs (Label + Wert). */
final class HostDetailRows {

    private HostDetailRows() {}

    static void addRow(JPanel panel, String label, String value, Color valColor, Color bg) {
        addRowWithLabel(panel, label, label(value, valColor), bg);
    }

    static void addRowWithLabel(JPanel panel, String label, JLabel valueLbl, Color bg) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(bg);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(6, 4, 6, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel keyLbl = new JLabel(label);
        keyLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        keyLbl.setForeground(FG_DIM);
        keyLbl.setPreferredSize(new Dimension(140, 20));

        row.add(keyLbl,   BorderLayout.WEST);
        row.add(valueLbl, BorderLayout.CENTER);
        panel.add(row);
    }

    static JLabel label(String text, Color color) {
        JLabel label = new JLabel(text != null ? text : "–");
        label.setFont(MONO_S);
        label.setForeground(color);
        return label;
    }

    static JButton detailButton(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x1E, 0x18) : new Color(0xE0, 0xDE, 0xD8));
        btn.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(GuiTheme.isDark() ? new Color(0x25, 0x2E, 0x25) : new Color(0xD0, 0xCE, 0xC8));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(GuiTheme.isDark() ? new Color(0x18, 0x1E, 0x18) : new Color(0xE0, 0xDE, 0xD8));
            }
        });
        return btn;
    }
}