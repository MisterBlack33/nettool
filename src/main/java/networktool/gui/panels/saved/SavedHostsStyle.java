package main.java.networktool.gui.panels.saved;

import main.java.networktool.model.HostResult;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Wiederverwendete Style-Bausteine (Buttons, Labels) für den Saved-Hosts-Bereich.
 */
final class SavedHostsStyle {

    private SavedHostsStyle() {}

    static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        l.setForeground(FG_DIM);
        return l;
    }

    static JButton actionBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg, 1), new EmptyBorder(4, 12, 4, 12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOV); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        return b;
    }

    static String hostname(HostResult h) { return h.hostname != null ? h.hostname : h.ip; }
    static String orEmpty(String s)      { return s != null ? s : ""; }
}
