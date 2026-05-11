package main.java.networktool_v3.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Haupt-Ausgabebereich der GUI.
 *
 * Dark Mode:  Tiefschwarz-Hintergrund, helle Schrift – hoher Kontrast
 * Light Mode: Reines Weiß-Hintergrund, schwarze Schrift – hoher Kontrast
 */
public class GuiOutputPanel {

    private final JTextPane    output;
    final         StyledDocument doc;

    public GuiOutputPanel() {
        output = new JTextPane();
        output.setEditable(false);
        // Theme-abhängige Hintergrundfarbe
        output.setBackground(terminalBg());
        output.setCaretColor(ACCENT);
        output.setFont(MONO);
        output.setMargin(new Insets(10, 14, 10, 14));
        doc = output.getStyledDocument();
    }

    /** Terminal-Hintergrund: Dark=Schwarz, Light=Weiß */
    private static Color terminalBg() {
        return GuiTheme.isDark()
                ? new Color(0x04, 0x06, 0x05)   // fast reines Schwarz
                : new Color(0xFF, 0xFF, 0xFE);   // reines Weiß
    }

    /** Primärer Textton für Standard-Output */
    private static Color terminalFg() {
        return GuiTheme.isDark()
                ? new Color(0xE8, 0xE4, 0xD8)   // warmes Weiß
                : new Color(0x10, 0x12, 0x10);   // fast Schwarz
    }

    // ── Panel-Builder ─────────────────────────────────────────────────────

    public JPanel buildTopBar() {
        Color barBg  = GuiTheme.isDark() ? new Color(0x0C, 0x0F, 0x0D) : new Color(0xE8, 0xE6, 0xE0);
        Color barFg  = GuiTheme.isDark() ? new Color(0x55, 0x60, 0x55) : new Color(0x60, 0x62, 0x5E);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(barBg);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        bar.setPreferredSize(new Dimension(0, 38));

        JLabel lbl = new JLabel("  TERMINAL OUTPUT");
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
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
                GuiTheme.isDark() ? new Color(0x10, 0x14, 0x12) : new Color(0xE0, 0xDE, 0xD8));
        return sp;
    }

    public JTextPane getOutputPane() { return output; }

    // ── Text-Ausgabe ──────────────────────────────────────────────────────

    public void appendText(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                StyleConstants.setFontFamily(attrs, "JetBrains Mono");
                StyleConstants.setFontSize(attrs, 13);
                doc.insertString(doc.getLength(), text, attrs);
                output.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void printBanner() {
        appendText(
                "╔═══════════════════════════════════════════╗\n" +
                        "║  // NetTool  —  Netzwerkanalyse Suite     ║\n" +
                        "║     Java · Swing GUI · v3.0               ║\n" +
                        "╚═══════════════════════════════════════════╝\n\n", ACCENT);
        appendText("  Wähle einen Menüpunkt auf der linken Seite.\n\n",
                GuiTheme.isDark() ? new Color(0x72, 0x78, 0x70) : new Color(0x55, 0x58, 0x52));
    }

    // ── Stream-Umleitung ──────────────────────────────────────────────────

    public void redirectStreams() {
        System.setOut(buildColoredStream(false));
        System.setErr(buildColoredStream(true));
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private JButton buildClearButton(Color barBg) {
        Color dimCol = GuiTheme.isDark() ? new Color(0x55, 0x60, 0x55) : new Color(0x70, 0x72, 0x6E);
        JButton btn = new JButton("LEEREN");
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        btn.setForeground(dimCol);
        btn.setBackground(barBg);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(4, 14, 4, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> clearOutput());
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setForeground(WARN); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setForeground(dimCol); }
        });
        return btn;
    }

    private void clearOutput() {
        try { doc.remove(0, doc.getLength()); }
        catch (BadLocationException ignored) {}
    }

    private PrintStream buildColoredStream(boolean isError) {
        return new PrintStream(new OutputStream() {
            private final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();

            public void write(int b) {
                if (b == '\r') return;
                buf.write(b);
                if (b == '\n') flush();
            }

            public void flush() {
                if (buf.size() == 0) return;
                String line = buf.toString(StandardCharsets.UTF_8);
                buf.reset();
                Color color = isError ? WARN : classifyColor(line);
                appendText(line, color);
            }
        }, true, StandardCharsets.UTF_8);
    }

    /**
     * Wählt Textfarbe nach Zeileninhalt.
     * Im Light Mode: dunklere Varianten der Akzentfarben für Lesbarkeit auf Weiß.
     */
    private static Color classifyColor(String line) {
        boolean dark = GuiTheme.isDark();

        if (line.contains("erreichbar") || line.contains("erfolgreich") || line.contains("Aktiv:"))
            return dark ? ACCENT2 : new Color(0x18, 0x90, 0x38);
        if (line.contains("===") || line.contains("═") || line.contains("╔") || line.contains("[ "))
            return dark ? ACCENT : new Color(0x9A, 0x6C, 0x08);
        if (line.contains("Fehler") || line.contains("NICHT") || line.contains("ERROR"))
            return WARN;
        if (line.contains("Windows"))
            return dark ? WIN_COL : new Color(0x18, 0x60, 0xB8);
        if (line.contains("Linux") || line.contains("Android"))
            return dark ? LIN_COL : new Color(0x18, 0x80, 0x28);
        if (line.contains("Apple") || line.contains("iOS") || line.contains("macOS"))
            return dark ? APL_COL : new Color(0x50, 0x50, 0x60);
        // Standard: hoher Kontrast je nach Theme
        return terminalFg();
    }
}