package main.java.networktool.gui;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static main.java.networktool.gui.GuiTheme.*;

/**
 * Speichert einen Host in einem gewählten Netzwerk.
 * Fragt bei mehreren Netzwerken nach Auswahl, warnt bei Präfix-Mismatch.
 */
final class HostSaveDialog {

    private HostSaveDialog() {}

    static void save(String ip, String hostname, String os, String portsDisplay, GuiOutputPanel output) {
        Map<Integer, String> ports = parsePorts(portsDisplay);
        HostResult host = new HostResult(ip, hostname, os, null, ports, "");

        String targetNetwork = resolveTargetNetwork(ip);
        if (targetNetwork == null) return;
        if (!confirmPrefixMismatch(ip, targetNetwork)) return;

        int caret = output.getOutputPane().getCaretPosition();
        boolean saved = NetworkStore.getInstance().save(host, targetNetwork);
        reportResult(ip, targetNetwork, saved, caret, output);
    }

    private static String resolveTargetNetwork(String ip) {
        List<String> networks = NetworkStore.getInstance().getNetworkNames();
        if (networks.size() == 1) return networks.get(0);

        List<String> matching = NetworkStore.getInstance().matchingNetworks(ip);
        String[] options = networks.toArray(new String[0]);
        String defaultChoice = matching.isEmpty() ? options[0] : matching.get(0);

        Object chosen = JOptionPane.showInputDialog(null,
                "IP " + ip + " in welches Netzwerk speichern?",
                "Netzwerk wählen", JOptionPane.QUESTION_MESSAGE,
                null, options, defaultChoice);
        return chosen != null ? chosen.toString() : null;
    }

    private static boolean confirmPrefixMismatch(String ip, String targetNetwork) {
        if (NetworkStore.getInstance().ipMatchesNetwork(ip, targetNetwork)) return true;

        String prefix = NetworkStore.getInstance().getPrefix(targetNetwork);
        int choice = JOptionPane.showConfirmDialog(null,
                "<html>IP <b>" + ip + "</b> passt nicht zum Präfix"
                        + " <b>\"" + prefix + "\"</b> von <b>\"" + targetNetwork + "\"</b>.<br>"
                        + "Trotzdem speichern?</html>",
                "Präfix-Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private static void reportResult(String ip, String targetNetwork, boolean saved,
                                     int caret, GuiOutputPanel output) {
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.text.SimpleAttributeSet a = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(a, saved ? ACCENT2 : WARN);
                javax.swing.text.StyleConstants.setFontFamily(a, "JetBrains Mono");
                javax.swing.text.StyleConstants.setFontSize(a, 13);
                String msg = saved
                        ? "  ★ " + ip + " gespeichert in \"" + targetNetwork + "\"\n"
                        : "  ✕ Speichern fehlgeschlagen\n";
                output.doc.insertString(output.doc.getLength(), msg, a);
                JTextPane pane = output.getOutputPane();
                pane.setCaretPosition(Math.min(caret, output.doc.getLength()));
            } catch (Exception ignored) {
                /* Dokument wurde zwischenzeitlich geändert/geleert */
            }
        });
    }

    private static Map<Integer, String> parsePorts(String display) {
        Map<Integer, String> map = new TreeMap<>();
        if (display == null || display.isBlank()) return map;
        String clean = display.replaceAll("[\\[\\]\\s]", "");
        for (String part : clean.split(",")) {
            try { map.put(Integer.parseInt(part), "offen"); }
            catch (NumberFormatException ignored) { /* kein gültiger Port */ }
        }
        return map;
    }
}