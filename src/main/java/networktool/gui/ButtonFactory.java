package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Zentrale Fabrik für einheitliche Swing-Buttons.
 * Ersetzt verteilte Build-Methoden in GuiSidebar, GuiAuditPanel, HostDetailsPanel usw.
 */
public final class ButtonFactory {

    private ButtonFactory() {}

    /** Standard-Aktions-Button mit Hover-Effekt. */
    public static JButton action(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(5, 12, 5, 12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(hoverListener(b, BTN_BG, BTN_HOV));
        return b;
    }

    /** Kleiner Icon-Button (z. B. in Toolbars). */
    public static JButton icon(String label, Color fg, Runnable action) {
        JButton b = action(label, fg);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Link-ähnlicher Button ohne Rahmen. */
    public static JButton link(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        b.setForeground(fg);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(ACCENT); }
            public void mouseExited (MouseEvent e) { b.setForeground(fg); }
        });
        return b;
    }

    /** Terminal-style Button (dunkler Hintergrund, farbiger Rand). */
    public static JButton terminal(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(new Color(0x10, 0x18, 0x10));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1), new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Hilfsmethode ─────────────────────────────────────────────────────

    private static MouseAdapter hoverListener(JButton b, Color normal, Color hover) {
        return new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(normal); }
        };
    }
}