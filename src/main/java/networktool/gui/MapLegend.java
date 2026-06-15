package main.java.networktool.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static main.java.networktool.gui.GuiTheme.*;

/** Legende für die Netzwerk-Karte. */
final class MapLegend {

    private MapLegend() {}

    private static final Object[][] ROWS = {
            {"*", "Ich (lokal)",   ACCENT2},
            {"G", "Gateway",       NET_COL},
            {"S", "Switch/Hub",    new Color(0xFF, 0xA0, 0x30)},
            {"D", "DNS-Server",    new Color(0x60, 0xD0, 0xFF)},
            {"H", "DHCP-Server",   new Color(0xFF, 0xC0, 0x40)},
            {"N", "NTP-Server",    new Color(0xA0, 0xFF, 0xC0)},
            {"W", "Windows",       WIN_COL},
            {"L", "Linux",         LIN_COL},
            {"M", "macOS / iOS",   APL_COL},
            {"A", "Android",       AND_COL},
            {"π", "Raspberry Pi",  RPI_COL},
            {"P", "Drucker",       PRN_COL},
            {"?", "Unbekannt",     FG_DIM},
    };

    static JPanel build() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x08, 0x0C, 0x08, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(6, 8, 6, 12));

        for (Object[] row : ROWS) {
            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
            line.setOpaque(false);
            JLabel dot = new JLabel((String) row[0]);
            dot.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
            dot.setForeground((Color) row[2]);
            JLabel lbl = new JLabel((String) row[1]);
            lbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
            lbl.setForeground(new Color(0xC8, 0xC4, 0xB8));
            line.add(dot); line.add(lbl);
            panel.add(line);
        }
        panel.setPreferredSize(new Dimension(170, ROWS.length * 18 + 14));
        return panel;
    }
}