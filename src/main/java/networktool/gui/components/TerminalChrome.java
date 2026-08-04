package networktool.gui.components;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wiederverwendete UI-Bausteine für das SSH-Terminal.
 */
final class TerminalChrome {

    private static final Logger LOG = Logger.getLogger(TerminalChrome.class.getName());

    private TerminalChrome() {}

    static void appendTerm(StyledDocument doc, String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setForeground(a, color);
                StyleConstants.setFontFamily(a, "JetBrains Mono");
                StyleConstants.setFontSize(a, 13);
                doc.insertString(doc.getLength(), text, a);
            } catch (BadLocationException ex) {
                LOG.log(Level.FINE, "Terminal-Text konnte nicht eingefügt werden", ex);
            }
        });
    }

    static JButton termBtn(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        b.setForeground(fg);
        b.setBackground(new Color(0x10, 0x18, 0x10));
        b.setBorder(new CompoundBorder(new LineBorder(fg.darker(), 1),
                new EmptyBorder(3, 8, 3, 8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
