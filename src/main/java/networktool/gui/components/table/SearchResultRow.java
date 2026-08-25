package main.java.networktool.gui.components.table;

import main.java.networktool.gui.hostdetails.HostDetailsPanel;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.network.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import main.java.networktool.theme.GuiTheme;
import static main.java.networktool.theme.GuiTheme.*;

/**
 * Treffer-Zeile und Match-Logik der {@link GuiSearchBar}.
 */
final class SearchResultRow {

    private SearchResultRow() {}

    static boolean matches(HostResult h, String q) {
        return contains(h.ip, q) || contains(h.hostname, q)
                || contains(h.os, q) || contains(h.notes, q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }

    static JPanel build(HostResult h) {
        Color rowBg  = GuiTheme.isDark() ? new Color(0x0C, 0x10, 0x0D) : new Color(0xF4, 0xF2, 0xEE);
        Color rowHov = GuiTheme.isDark() ? new Color(0x18, 0x22, 0x18) : new Color(0xE4, 0xE2, 0xDC);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(rowBg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(4, 10, 4, 10)));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel ipLbl = new JLabel(h.ip);
        ipLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        ipLbl.setForeground(ACCENT);
        ipLbl.setPreferredSize(new Dimension(120, 20));

        String hn = h.hostname != null && h.hostname.contains(" [")
                ? h.hostname.substring(0, h.hostname.indexOf(" [")) : h.hostname;
        JLabel hnLbl = new JLabel(hn != null ? hn : "");
        hnLbl.setFont(MONO_XS);
        hnLbl.setForeground(GuiTheme.isDark() ? new Color(0xC0, 0xBC, 0xB0) : new Color(0x30, 0x32, 0x2E));

        JLabel osLbl = new JLabel(h.os != null ? h.os : "");
        osLbl.setFont(MONO_XS);
        osLbl.setForeground(osColor(h.os));
        osLbl.setPreferredSize(new Dimension(130, 20));
        osLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(ipLbl, BorderLayout.WEST);
        row.add(hnLbl, BorderLayout.CENTER);
        row.add(osLbl, BorderLayout.EAST);
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(rowHov); }
            public void mouseExited(MouseEvent e)  { row.setBackground(rowBg); }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String cat = NetworkStore.getInstance().findNetwork(h.ip);
                    HostDetailsPanel.show(h.ip, h.hostname, h.os, cat);
                }
            }
        });
        return row;
    }
}
