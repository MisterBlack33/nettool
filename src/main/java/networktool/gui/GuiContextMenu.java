package main.java.networktool.gui;

import main.java.networktool.logic.analysis.IpInspector;
import main.java.networktool.logic.analysis.WakeOnLan;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Rechtsklick-Kontextmenü für alle JTables.
 *
 * Einträge: Details, Schnelldiagnose, Vollanalyse, SSH, Nachricht,
 * IP speichern/entfernen, Wake-on-LAN (wenn MAC bekannt).
 *
 * Aktionslogik ausgelagert: {@link HostSaveDialog}, {@link ContextMenuActions}.
 */
public class GuiContextMenu {

    private static final Color MENU_BG       = new Color(0x13, 0x19, 0x21);
    private static final Color MENU_HOVER_BG = new Color(0x1E, 0x2D, 0x3D);

    private final GuiMenuHandler menuHandler;
    private final GuiOutputPanel output;

    public GuiContextMenu(GuiMenuHandler menuHandler, GuiOutputPanel output) {
        this.menuHandler = menuHandler;
        this.output      = output;
    }

    public void attach(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShow(e, table); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e, table); }
        });
    }

    /** Delegiert an {@link NtfyTopicPrompt}; bleibt hier für Aufrufkompatibilität. */
    public static String promptNtfyTopic() {
        return NtfyTopicPrompt.prompt();
    }

    // ── Aufbau ────────────────────────────────────────────────────────────

    private void maybeShow(MouseEvent e, JTable table) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);

        String ip   = cellValue(table, row, 0);
        String hn   = cellValue(table, row, 1);
        String os   = cellValue(table, row, 2);
        String col3 = cellValue(table, row, 3); // Ports oder Datum

        buildPopup(ip, hn, os, col3).show(e.getComponent(), e.getX(), e.getY());
    }

    private JPopupMenu buildPopup(String ip, String hn, String os, String col3) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(MENU_BG);
        popup.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

        addAnalysisItems(popup, ip, hn, os);
        popup.addSeparator();
        addSaveOrRemoveItem(popup, ip, hn, os, col3);
        addWakeOnLanItem(popup, ip, hn);
        return popup;
    }

    private void addAnalysisItems(JPopupMenu popup, String ip, String hn, String os) {
        popup.add(menuItem("🔍  Details anzeigen", ACCENT, () ->
                HostDetailsPanel.show(ip, hn, os, NetworkStore.getInstance().findNetwork(ip))));
        popup.add(menuItem("▶  Schnelldiagnose", new Color(0x80, 0xC8, 0xFF), () ->
                menuHandler.runAsync(() -> IpInspector.quickScan(ip, 5000))));
        popup.add(menuItem("⛐  Vollanalyse", FG, () ->
                menuHandler.runAsync(() -> IpInspector.inspect(ip))));
        popup.add(menuItem("⌨  SSH-Terminal", new Color(0x00, 0xFF, 0x80), () ->
                GuiSshTerminal.open(ip)));
        popup.add(menuItem("✉  Nachricht senden", new Color(0xFF, 0xD5, 0x4F), () ->
                ContextMenuActions.promptAndSendMessage(ip, menuHandler)));
    }

    private void addSaveOrRemoveItem(JPopupMenu popup, String ip, String hn, String os, String col3) {
        String inNetwork = NetworkStore.getInstance().findNetwork(ip);
        if (inNetwork != null) {
            popup.add(menuItem("✕  Aus \"" + inNetwork + "\" entfernen", WARN, () ->
                    NetworkStore.getInstance().removeFromAll(ip)));
        } else {
            popup.add(menuItem("★  IP speichern", ACCENT2, () ->
                    HostSaveDialog.save(ip, hn, os, col3, output)));
        }
    }

    private void addWakeOnLanItem(JPopupMenu popup, String ip, String hn) {
        String mac = WakeOnLan.extractMacFromHostname(hn);
        if (mac == null) return;
        popup.addSeparator();
        popup.add(menuItem("⚡  Wake-on-LAN", new Color(0xFF, 0xD5, 0x4F), () ->
                ContextMenuActions.sendWakeOnLan(ip, mac, menuHandler, output)));
    }

    private JMenuItem menuItem(String text, Color fg, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        item.setForeground(fg);
        item.setBackground(MENU_BG);
        item.setBorder(new EmptyBorder(6, 16, 6, 24));
        item.setOpaque(true);
        item.addActionListener(e -> action.run());
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(MENU_HOVER_BG); }
            public void mouseExited(MouseEvent e)  { item.setBackground(MENU_BG); }
        });
        return item;
    }

    private static String cellValue(JTable table, int row, int col) {
        if (col >= table.getColumnCount()) return "";
        Object val = table.getValueAt(row, col);
        return val != null ? String.valueOf(val) : "";
    }
}