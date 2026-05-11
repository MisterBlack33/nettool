package main.java.networktool_v3.gui;

import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Tab-Leiste für die Netzwerk-Auswahl im {@link GuiSavedHostsPanel}.
 *
 * Enthält:
 *  - Einen Tab-Button je Netzwerk (mit Host-Anzahl)
 *  - [+ Neu]  [✎ Umbenennen]  [✕ Löschen]
 */
public final class GuiNetworkBar {

    private GuiNetworkBar() {}

    /**
     * @param active         aktuell aktives Netzwerk
     * @param onSelect       Callback wenn Tab geklickt wird
     * @param onNew          Callback für "+ Neu"
     * @param onRename       Callback für "✎"
     * @param onDelete       Callback für "✕"
     */
    public static JPanel build(String active,
                                Runnable onNew, Runnable onRename, Runnable onDelete,
                                java.util.function.Consumer<String> onSelect) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bar.setBackground(PANEL_BG);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 0, 1, BORDER),
                new EmptyBorder(6, 8, 6, 8)));

        List<String> names = NetworkStore.getInstance().getNetworkNames();
        for (String name : names) {
            bar.add(buildTab(name, name.equals(active), onSelect));
        }

        bar.add(Box.createHorizontalStrut(8));
        bar.add(iconBtn("+ Neu",  ACCENT2, onNew));
        bar.add(iconBtn("✎",      FG_DIM,  onRename));
        bar.add(iconBtn("✕",      WARN,    onDelete));
        return bar;
    }

    private static JButton buildTab(String name, boolean active,
                                    java.util.function.Consumer<String> onSelect) {
        int count = NetworkStore.getInstance().getAll(name).size();
        String label = count > 0 ? name + " (" + count + ")" : name;

        JButton btn = new JButton(label);
        btn.setFont(active
                ? new Font("JetBrains Mono", Font.BOLD,  12)
                : new Font("JetBrains Mono", Font.PLAIN, 12));
        btn.setForeground(active ? ACCENT : FG_DIM);
        btn.setBackground(active ? new Color(0x0A, 0x20, 0x30) : BTN_BG);
        btn.setBorder(new CompoundBorder(
                new LineBorder(active ? ACCENT : BORDER, 1),
                new EmptyBorder(4, 12, 4, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onSelect.accept(name));
        return btn;
    }

    static JButton iconBtn(String label, Color fg, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBackground(BTN_BG);
        btn.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBorder(new CompoundBorder(
                    new LineBorder(fg, 1), new EmptyBorder(4, 8, 4, 8))); }
            public void mouseExited(MouseEvent e)  { btn.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1), new EmptyBorder(4, 8, 4, 8))); }
        });
        return btn;
    }
}
