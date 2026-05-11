package main.java.networktool_v3.gui;

import main.java.networktool_v3.gui.notification.NotificationListener;
import main.java.networktool_v3.logic.analysis.IpInspector;
import main.java.networktool_v3.logic.analysis.WakeOnLan;
import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static main.java.networktool_v3.gui.GuiTheme.*;

/**
 * Rechtsklick-Kontextmenü für alle JTables.
 *
 * Einträge:
 *  ▶  Schnelldiagnose      (ICMP + Ports + OS)
 *  ⛐  Vollanalyse          (+ ARP/MAC + Traceroute)
 *  ✉  Nachricht senden
 *  ─────────────────────
 *  ★  IP speichern  /  ✕ Entfernen
 *
 * Beim Speichern: Ports aus SCAN-Tabelle (Spalte 3) werden mitgenommen.
 * Nach dem Speichern: Caret bleibt an seiner Position (kein Scroll).
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

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private void maybeShow(MouseEvent e, JTable table) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);

        String ip       = cellValue(table, row, 0);
        String hn       = cellValue(table, row, 1);
        String os       = cellValue(table, row, 2);
        String col3     = cellValue(table, row, 3); // Ports oder Datum

        buildPopup(ip, hn, os, col3).show(e.getComponent(), e.getX(), e.getY());
    }

    private JPopupMenu buildPopup(String ip, String hn, String os, String col3) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(MENU_BG);
        popup.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(4, 0, 4, 0)));

        popup.add(buildMenuItem("🔍  Details anzeigen", ACCENT, () ->
                HostDetailsPanel.show(ip, hn, os,
                        NetworkStore.getInstance().findNetwork(ip))));

        popup.addSeparator();

        popup.add(buildMenuItem("▶  Schnelldiagnose", new Color(0x80,0xC8,0xFF), () ->
                menuHandler.runAsync(() -> IpInspector.quickScan(ip, 5000))));

        popup.add(buildMenuItem("⛐  Vollanalyse", FG, () ->
                menuHandler.runAsync(() -> IpInspector.inspect(ip))));

        popup.add(buildMenuItem("⌨  SSH-Terminal", new Color(0x00, 0xFF, 0x80), () ->
                GuiSshTerminal.open(ip)));

        popup.add(buildMenuItem("✉  Nachricht senden", new Color(0xFF, 0xD5, 0x4F), () ->
                promptAndSendMessage(ip)));

        popup.addSeparator();

        boolean alreadySaved = NetworkStore.getInstance().findNetwork(ip) != null;

        if (alreadySaved) {
            String inNetwork = NetworkStore.getInstance().findNetwork(ip);
            popup.add(buildMenuItem("✕  Aus \"" + inNetwork + "\" entfernen", WARN, () ->
                    NetworkStore.getInstance().removeFromAll(ip)));
        } else {
            popup.add(buildMenuItem("★  IP speichern", ACCENT2, () ->
                    saveHost(ip, hn, os, col3)));
        }

        // Wake-on-LAN (nur wenn MAC bekannt)
        String mac = WakeOnLan.extractMacFromHostname(hn);
        if (mac != null) {
            final String finalMac = mac;
            popup.addSeparator();
            popup.add(buildMenuItem("⚡  Wake-on-LAN", new java.awt.Color(0xFF, 0xD5, 0x4F),
                    () -> sendWakeOnLan(ip, finalMac)));
        }

        return popup;
    }

    private void sendWakeOnLan(String ip, String mac) {
        String broadcast = WakeOnLan.deriveBroadcast(ip, 24);
        String custom = javax.swing.JOptionPane.showInputDialog(null,
                "<html>Broadcast-Adresse für WoL:<br>"
                        + "<small>Standard: " + broadcast + "</small></html>",
                "Wake-on-LAN", javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (custom == null) return;
        String target = custom.isBlank() ? broadcast : custom.trim();
        menuHandler.runAsync(() -> {
            boolean ok = WakeOnLan.send(mac, target);
            output.appendText(ok
                            ? "  ⚡ WoL-Paket gesendet an " + mac + " via " + target + "\n"
                            : "  ✕ WoL fehlgeschlagen\n",
                    ok ? ACCENT2 : WARN);
        });
    }

    /**
     * Zweistufiger JOptionPane-Dialog: Nachricht + ntfy-Topic.
     * Delegiert Topic-Auswahl an {@link #promptNtfyTopic()}.
     */
    private void promptAndSendMessage(String ip) {
        String msg = JOptionPane.showInputDialog(null,
                "Nachricht an " + ip + ":", "Nachricht senden", JOptionPane.PLAIN_MESSAGE);
        if (msg == null || msg.isBlank()) return;

        String topic = promptNtfyTopic();
        if (topic == null) return; // Abgebrochen

        final String finalTopic = topic.trim();
        if (!finalTopic.isEmpty()) {
            NetworkStore.getInstance().saveNtfyTopic(finalTopic);
            NotificationListener.subscribeNewTopic(finalTopic);
        }
        menuHandler.runAsync(() -> MessageSender.send(ip, msg, finalTopic));
    }

    /**
     * Einheitlicher ntfy-Topic-Auswahl-Dialog (statisch, für alle Aufruforte).
     *
     * Zeigt Dropdown mit:
     *   (kein ntfy-Push)
     *   [gespeicherte Topics, alphabetisch]
     *   + Neues Topic eingeben...
     *
     * @return gewähltes Topic (kann leer sein = kein Push), oder null bei Abbruch
     */
    public static String promptNtfyTopic() {
        java.util.List<String> saved = new java.util.ArrayList<>(
                NetworkStore.getInstance().getNtfyTopics());
        java.util.Collections.sort(saved);

        final String OPT_NONE = "(kein ntfy-Push)";
        final String OPT_NEW  = "+ Neues Topic eingeben...";

        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(OPT_NONE);
        options.addAll(saved);
        options.add(OPT_NEW);

        Object chosen = JOptionPane.showInputDialog(null,
                "<html><b>ntfy-Topic wählen</b><br>"
                        + "<small>Handy-Push: ntfy-App installieren, gleiches Topic abonnieren.<br>"
                        + "Topics werden alphabetisch gespeichert.</small></html>",
                "ntfy-Topic", JOptionPane.QUESTION_MESSAGE, null,
                options.toArray(), options.get(0));

        if (chosen == null) return null; // Dialog abgebrochen

        if (OPT_NEW.equals(chosen.toString())) {
            String t = JOptionPane.showInputDialog(null,
                    "Neues ntfy-Topic eingeben:", "Neues Topic", JOptionPane.PLAIN_MESSAGE);
            return t != null ? t.trim() : null;
        }
        if (OPT_NONE.equals(chosen.toString())) return "";
        return chosen.toString();
    }

    /**
     * Speichert den Host in einem gewählten Netzwerk.
     * Netzwerk-Auswahl-Dialog; warnt wenn IP nicht zum Präfix passt.
     * Caret bleibt an seiner Position (kein Scroll).
     */
    private void saveHost(String ip, String hostname, String os, String col3) {
        java.util.Map<Integer, String> ports = parsePortsFromDisplay(col3);
        HostResult host = new HostResult(ip, hostname, os, null, ports, "");

        java.util.List<String> networks = NetworkStore.getInstance().getNetworkNames();
        String targetNetwork;

        if (networks.size() == 1) {
            targetNetwork = networks.get(0);
        } else {
            // Präfix-passende Netzwerke als Vorschlag
            java.util.List<String> matching = NetworkStore.getInstance().matchingNetworks(ip);
            String[] options = networks.toArray(new String[0]);
            String defaultChoice = matching.isEmpty() ? options[0] : matching.get(0);

            Object chosen = JOptionPane.showInputDialog(null,
                    "IP " + ip + " in welches Netzwerk speichern?",
                    "Netzwerk wählen", JOptionPane.QUESTION_MESSAGE,
                    null, options, defaultChoice);
            if (chosen == null) return;
            targetNetwork = chosen.toString();
        }

        // Präfix-Warnung wenn IP nicht passt
        if (!NetworkStore.getInstance().ipMatchesNetwork(ip, targetNetwork)) {
            String prefix = NetworkStore.getInstance().getPrefix(targetNetwork);
            int ok = JOptionPane.showConfirmDialog(null,
                    "<html>IP <b>" + ip + "</b> passt nicht zum Präfix"
                            + " <b>\"" + prefix + "\"</b> von <b>\"" + targetNetwork + "\"</b>.<br>"
                            + "Trotzdem speichern?</html>",
                    "Präfix-Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        JTextPane pane = output.getOutputPane();
        int savedCaret = pane.getCaretPosition();
        boolean saved  = NetworkStore.getInstance().save(host, targetNetwork);

        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.text.SimpleAttributeSet a = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(a, ACCENT2);
                javax.swing.text.StyleConstants.setFontFamily(a, "JetBrains Mono");
                javax.swing.text.StyleConstants.setFontSize(a, 13);
                String msg = saved
                        ? "  ★ " + ip + " gespeichert in \"" + targetNetwork + "\"\n"
                        : "  ✕ Speichern fehlgeschlagen\n";
                output.doc.insertString(output.doc.getLength(), msg, a);
                pane.setCaretPosition(Math.min(savedCaret, output.doc.getLength()));
            } catch (Exception ignored) {}
        });
    }

    private static java.util.Map<Integer, String> parsePortsFromDisplay(String s) {
        java.util.Map<Integer, String> map = new java.util.TreeMap<>();
        if (s == null || s.isBlank()) return map;
        String clean = s.replaceAll("[\\[\\]\\s]", "");
        for (String part : clean.split(",")) {
            try { map.put(Integer.parseInt(part), "offen"); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private JMenuItem buildMenuItem(String text, Color fg, Runnable action) {
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
        if (col < table.getColumnCount()) {
            Object val = table.getValueAt(row, col);
            return val != null ? String.valueOf(val) : "";
        }
        return "";
    }
}