package main.java.networktool.gui.dashboard;

import main.java.networktool.gui.core.GuiDebugMode;
import main.java.networktool.gui.panels.GuiOutputPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import main.java.networktool.theme.GuiTheme;
import static main.java.networktool.theme.GuiTheme.*;

/** Startbildschirm mit Kennzahlen-Kacheln (Menü-ID "29", Test-Suite). */
public final class GuiDashboardPanel {

    private GuiDashboardPanel() {}

    public static void show(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embedPanel(output));
    }

    public static void showDebug(GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> embedPanel(output, true));
    }

    private static void embedPanel(GuiOutputPanel output) {
        embedPanel(output, false);
    }

    private static void embedPanel(GuiOutputPanel output, boolean debug) {
        output.appendText("\nDashboard\n\n", ACCENT);

        Color bg = GuiTheme.isDark() ? new Color(0x08, 0x0B, 0x09) : new Color(0xF4, 0xF2, 0xEE);
        GuiDashboardStats.Snapshot snap = debug
                ? new GuiDashboardStats.Snapshot(250, "DEBUG-CIDR (250 Hosts)", 12, 25)
                : GuiDashboardStats.capture();

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setBackground(bg);
        grid.setBorder(new EmptyBorder(8, 0, 8, 0));
        grid.add(tile("Hosts gespeichert", String.valueOf(snap.hostCount()), ACCENT2));
        grid.add(tile("Letzter Scan", snap.lastScanLabel(), INFO));
        grid.add(tile("Offene Findings", String.valueOf(snap.findingsCount()),
                snap.findingsCount() > 0 ? WARN : ACCENT2));
        grid.add(tile("Favoriten", String.valueOf(snap.favoriteCount()), ACCENT));

        JTextPane pane = output.getOutputPane();
        pane.setEditable(true);
        pane.setCaretPosition(output.doc.getLength());
        pane.insertComponent(grid);
        pane.setEditable(false);
        output.appendText("\n\n", FG);
    }

    private static JPanel tile(String label, String value, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(GuiTheme.isDark() ? new Color(0x0F, 0x13, 0x10) : new Color(0xE8, 0xE6, 0xE0));
        p.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(12, 14, 12, 14)));

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 22));
        valueLbl.setForeground(accent);

        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        labelLbl.setForeground(FG_DIM);

        p.add(valueLbl, BorderLayout.CENTER);
        p.add(labelLbl, BorderLayout.SOUTH);
        return p;
    }
}
