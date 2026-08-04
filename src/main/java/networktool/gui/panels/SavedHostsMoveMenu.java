package networktool.gui.panels;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

import static networktool.theme.GuiTheme.*;

/**
 * Rechtsklick-Kontextmenü zum Verschieben eines einzelnen Hosts
 * in ein anderes Netzwerk (nur außerhalb der "Alle"-Ansicht aktiv).
 */
final class SavedHostsMoveMenu {

    private static final int COL_IP = 1;

    private SavedHostsMoveMenu() {}

    static void install(JTable table, Supplier<String> activeNetwork,
                         GuiOutputPanel output, Runnable refreshTable) {
        MouseAdapter listener = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) { tryShow(e, table, activeNetwork, output, refreshTable); }
            @Override public void mousePressed(MouseEvent e)  { tryShow(e, table, activeNetwork, output, refreshTable); }
        };
        table.addMouseListener(listener);
    }

    private static void tryShow(MouseEvent e, JTable table, Supplier<String> activeNetwork,
                                 GuiOutputPanel output, Runnable refreshTable) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        Object ipVal = table.getValueAt(row, COL_IP);
        if (ipVal == null || "–".equals(ipVal.toString())) return;
        String ip = ipVal.toString();
        String current = activeNetwork.get();
        List<String> others = NetworkStore.getInstance().getNetworkNames().stream()
                .filter(n -> !n.equals(current) && !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (others.isEmpty()) return;

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x13, 0x19, 0x21));
        menu.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));
        JMenuItem hdr = new JMenuItem("Verschieben nach:");
        hdr.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        hdr.setForeground(FG_DIM);
        hdr.setBackground(new Color(0x13, 0x19, 0x21));
        hdr.setEnabled(false);
        menu.add(hdr);
        menu.addSeparator();

        for (String target : others) {
            JMenuItem item = new JMenuItem("→  " + target);
            item.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
            item.setForeground(ACCENT);
            item.setBackground(new Color(0x13, 0x19, 0x21));
            item.setBorder(new EmptyBorder(6, 16, 6, 24));
            item.setOpaque(true);
            item.addActionListener(ev -> {
                NetworkStore.getInstance().moveHost(ip, current, target);
                output.appendText("  → " + ip + " → \"" + target + "\"\n", ACCENT2);
                refreshTable.run();
            });
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent ev) { item.setBackground(new Color(0x1E, 0x2D, 0x3D)); }
                public void mouseExited(MouseEvent ev)  { item.setBackground(new Color(0x13, 0x19, 0x21)); }
            });
            menu.add(item);
        }
        menu.show(e.getComponent(), e.getX() + 160, e.getY());
    }
}
