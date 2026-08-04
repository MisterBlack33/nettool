package networktool.gui.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import networktool.theme.GuiTheme;
import static networktool.theme.GuiTheme.*;

/**
 * Wiederverwendete Swing-Bausteine für {@link GuiPrivacyPanel}.
 */
final class PrivacyPanelStyle {

    private PrivacyPanelStyle() {}

    static JPanel buildSection(String title, Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        p.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(BORDER, 1), "  " + title + "  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("JetBrains Mono", Font.BOLD, 11), ACCENT),
                new EmptyBorder(8, 12, 12, 12)));
        return p;
    }

    static JLabel statusLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    static JButton actionBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(5, 12, 5, 12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited (MouseEvent e) { b.setBackground(BTN_BG); }
        });
        return b;
    }

    static JTextArea buildLogArea(Color bg) {
        JTextArea a = new JTextArea();
        a.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        a.setForeground(new Color(0xA0, 0xE0, 0xA0));
        a.setBackground(GuiTheme.isDark() ? new Color(0x06,0x09,0x06) : new Color(0xF0,0xF8,0xF0));
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(6, 8, 6, 8));
        return a;
    }

    static JPanel wrapRow(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton b : btns) p.add(b);
        return p;
    }
}
