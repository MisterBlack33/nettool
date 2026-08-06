package main.java.networktool.gui.panels;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import javax.swing.*;
import java.util.List;

import static main.java.networktool.theme.GuiTheme.*;

/**
 * Dialog-Kaskade zum manuellen Anlegen eines Hosts (ohne Netzwerk-Scan).
 */
final class SavedHostsManualAdd {

    private SavedHostsManualAdd() {}

    static JButton buildButton(GuiOutputPanel output, Runnable refreshTable) {
        JButton btn = SavedHostsStyle.actionBtn("+ IP manuell speichern", ACCENT2);
        btn.addActionListener(e -> promptManualAdd(output, refreshTable));
        return btn;
    }

    private static void promptManualAdd(GuiOutputPanel output, Runnable refreshTable) {
        String ip = JOptionPane.showInputDialog(null, "IP-Adresse:", "IP manuell speichern", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.isBlank()) return;
        ip = ip.trim();
        String hn    = JOptionPane.showInputDialog(null, "Hostname (leer = IP):", "Hostname", JOptionPane.PLAIN_MESSAGE);
        String os    = JOptionPane.showInputDialog(null, "OS / Gerätetyp:", "OS", JOptionPane.PLAIN_MESSAGE);
        String notes = JOptionPane.showInputDialog(null, "Notiz (optional):", "Notiz", JOptionPane.PLAIN_MESSAGE);
        if (hn == null || hn.isBlank()) hn = ip;
        if (os == null || os.isBlank()) os = "Unbekannt";
        if (notes == null) notes = "";
        List<String> networks = NetworkStore.getInstance().getNetworkNames()
                .stream().filter(n -> !n.equals(NetworkStore.ALL_CATEGORY)).toList();
        if (networks.isEmpty()) { output.appendText("  ✕ Kein Netzwerk vorhanden.\n", WARN); return; }
        String targetNet = networks.size() == 1 ? networks.get(0) : pickNetwork(ip, networks);
        if (targetNet == null) return;
        boolean saved = NetworkStore.getInstance().save(new HostResult(ip, hn, os, null, null, notes), targetNet);
        output.appendText(saved ? "  ★ " + ip + " gespeichert in \"" + targetNet + "\"\n"
                : "  ✕ Speichern fehlgeschlagen\n", saved ? ACCENT2 : WARN);
        refreshTable.run();
    }

    private static String pickNetwork(String ip, List<String> networks) {
        List<String> matching = NetworkStore.getInstance().matchingNetworks(ip);
        Object chosen = JOptionPane.showInputDialog(null, "In welches Netzwerk?", "Netzwerk wählen",
                JOptionPane.QUESTION_MESSAGE, null, networks.toArray(),
                matching.isEmpty() ? networks.get(0) : matching.get(0));
        return chosen == null ? null : chosen.toString();
    }
}
