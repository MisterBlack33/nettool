package networktool.gui.panels;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import networktool.theme.GuiTheme;
import static networktool.theme.GuiTheme.*;

/**
 * Haupt-Ausgabebereich der GUI.
 * Text-Limit: MAX_CHARS – älteste Zeichen werden automatisch entfernt.
 */
public class GuiOutputPanel {

    private static final Logger LOG = Logger.getLogger(GuiOutputPanel.class.getName());

    static final int MAX_CHARS = 50_000;

    private final JTextPane    output;
    public final StyledDocument doc;

    public GuiOutputPanel() {
        output = new JTextPane();
        output.setEditable(false);
        output.setBackground(terminalBg());
        output.setCaretColor(ACCENT);
        output.setFont(MONO);
        output.setMargin(new Insets(10, 14, 10, 14));
        doc = output.getStyledDocument();
    }

    // ── Panel-Builder ─────────────────────────────────────────────────────

    public JPanel buildTopBar() {
        Color barBg = GuiTheme.isDark() ? new Color(0x0C,0x0F,0x0D) : new Color(0xE8,0xE6,0xE0);
        Color barFg = GuiTheme.isDark() ? new Color(0x55,0x60,0x55) : new Color(0x60,0x62,0x5E);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        bar.setPreferredSize(new Dimension(0, 34));
        JLabel lbl = new JLabel("  OUTPUT");
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        lbl.setForeground(barFg);
        bar.add(lbl, BorderLayout.WEST);
        bar.add(buildClearButton(barBg), BorderLayout.EAST);
        return bar;
    }

    public JScrollPane buildScrollPane() {
        Color bg = terminalBg();
        JScrollPane sp = new JScrollPane(output);
        sp.setBorder(null);
        sp.getViewport().setBackground(bg);
        sp.getVerticalScrollBar().setBackground(
                GuiTheme.isDark() ? new Color(0x10,0x14,0x12) : new Color(0xE0,0xDE,0xD8));
        return sp;
    }

    public JTextPane getOutputPane() { return output; }

    // ── Text-Ausgabe ──────────────────────────────────────────────────────

    public void appendText(String text, Color color) {
        if (SwingUtilities.isEventDispatchThread()) {
            doAppend(text, color);
        } else {
            SwingUtilities.invokeLater(() -> doAppend(text, color));
        }
    }

    private void doAppend(String text, Color color) {
        try {
            trimIfNeeded();
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, color);
            StyleConstants.setFontFamily(a, "JetBrains Mono");
            StyleConstants.setFontSize(a, 13);
            doc.insertString(doc.getLength(), text, a);
            output.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            LOG.log(Level.FINE, "Text konnte nicht an Ausgabe angehängt werden", e);
        }
    }

    public void printBanner() {
        appendText(
                "╔══════════════════════════════════╗\n" +
                        "║  NetTool v3  ·  Network Suite    ║\n" +
                        "╚══════════════════════════════════╝\n\n", ACCENT);
    }

    // ── Stream-Umleitung ──────────────────────────────────────────────────

    public void redirectStreams() {
        System.setOut(OutputStreamRedirector.build(false, this::appendText));
        System.setErr(OutputStreamRedirector.build(true, this::appendText));
    }

    // ── Private ───────────────────────────────────────────────────────────

    /** Entfernt vorne ~10 % des Inhalts wenn MAX_CHARS überschritten. */
    private void trimIfNeeded() {
        int len = doc.getLength();
        if (len <= MAX_CHARS) return;
        int remove = MAX_CHARS / 10;
        try {
            doc.remove(0, remove);
        } catch (BadLocationException e) {
            LOG.log(Level.FINE, "Alter Ausgabe-Inhalt konnte nicht getrimmt werden", e);
        }
    }

    private JButton buildClearButton(Color barBg) {
        Color dimCol = GuiTheme.isDark() ? new Color(0x55,0x60,0x55) : new Color(0x70,0x72,0x6E);
        JButton btn = new JButton("LEEREN");
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        btn.setForeground(dimCol);
        btn.setBackground(barBg);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(3, 12, 3, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> clearOutput());
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setForeground(WARN); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setForeground(dimCol); }
        });
        return btn;
    }

    private void clearOutput() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            LOG.log(Level.FINE, "Ausgabe konnte nicht geleert werden", e);
        }
    }

    private static Color terminalBg() {
        return GuiTheme.isDark() ? new Color(0x04,0x06,0x05) : new Color(0xFF,0xFF,0xFE);
    }
}