// src/main/java/networktool/gui/MapContextMenu.java
package main.java.networktool.gui;

import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Rechtsklick-Kontextmenü für Netzwerk-Karte.
 */
final class MapContextMenu {

    private static final Color MENU_BG  = new Color(0x0F, 0x13, 0x10);
    private static final Color MENU_HOV = new Color(0x1A, 0x22, 0x1A);

    private MapContextMenu() {}

    static void show(GuiNetworkMap.Node node, Component comp, int x, int y, MapCanvas canvas) {
        boolean isSwitch = node.type == GuiNetworkMap.NodeType.SWITCH;
        JPopupMenu menu = buildMenu(node, isSwitch, canvas);
        menu.show(comp, x, y);
    }

    private static JPopupMenu buildMenu(GuiNetworkMap.Node node, boolean isSwitch, MapCanvas canvas) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(MENU_BG);
        menu.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

        JMenuItem header = item(clip(node.ip + "  " + node.hostname, 28), FG_DIM);
        header.setEnabled(false);
        menu.add(header);
        menu.addSeparator();

        String switchLabel = isSwitch ? "✕  Kein Switch" : "S  Als Switch markieren";
        Color  switchColor = isSwitch ? WARN : new Color(0xFF, 0xA0, 0x30);
        JMenuItem switchItem = item(switchLabel, switchColor);
        switchItem.addActionListener(e -> {
            if (isSwitch) MapSwitchStore.remove(node.ip); else MapSwitchStore.add(node.ip);
            canvas.reload();
        });
        menu.add(switchItem);

        JMenuItem details = item("🔍  Details", ACCENT);
        details.addActionListener(e -> HostDetailsPanel.show(node.ip, node.hostname, node.os,
                NetworkStore.getInstance().findNetwork(node.ip)));
        menu.add(details);
        return menu;
    }

    private static JMenuItem item(String text, Color fg) {
        JMenuItem i = new JMenuItem(text);
        i.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        i.setForeground(fg); i.setBackground(MENU_BG);
        i.setBorder(new EmptyBorder(6, 14, 6, 20)); i.setOpaque(true);
        i.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { i.setBackground(MENU_HOV); }
            public void mouseExited(MouseEvent e)  { i.setBackground(MENU_BG); }
        });
        return i;
    }

    private static String clip(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}