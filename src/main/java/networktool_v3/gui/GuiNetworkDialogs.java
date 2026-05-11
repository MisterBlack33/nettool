package main.java.networktool_v3.gui;

import main.java.networktool_v3.storage.NetworkStore;

import javax.swing.*;
import java.util.List;

/**
 * JOptionPane-Dialoge für Netzwerk-Verwaltungsaktionen im {@link GuiSavedHostsPanel}.
 *
 *  promptNew()    – neues Netzwerk anlegen (Name + Präfix)
 *  promptRename() – aktives Netzwerk umbenennen
 *  confirmDelete()– aktives Netzwerk löschen (mit Schutz)
 */
public final class GuiNetworkDialogs {

    private GuiNetworkDialogs() {}

    /**
     * @return Name des neuen Netzwerks oder null wenn abgebrochen
     */
    public static String[] promptNew() {
        String name = JOptionPane.showInputDialog(null,
                "Name des neuen Netzwerks (z.B. Schule, Heim, Arbeit):",
                "Netzwerk anlegen", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return null;

        String prefix = JOptionPane.showInputDialog(null,
                "<html>IP-Präfix (optional):<br>"
                + "<small>Nur IPs die mit diesem Präfix beginnen werden akzeptiert.<br>"
                + "Beispiele: 192.168.1.  |  10.0.0.  |  172.16.<br>"
                + "Leer = kein Filter</small></html>",
                "IP-Präfix für " + name, JOptionPane.PLAIN_MESSAGE);
        if (prefix == null) return null;

        return new String[]{name.trim(), prefix.trim()};
    }

    /**
     * @return neuer Name oder null wenn abgebrochen
     */
    public static String promptRename(String currentName) {
        String newName = JOptionPane.showInputDialog(null,
                "Neuer Name für \"" + currentName + "\":",
                "Netzwerk umbenennen", JOptionPane.PLAIN_MESSAGE);
        return (newName == null || newName.isBlank()) ? null : newName.trim();
    }

    /**
     * @return true wenn Löschen bestätigt wurde
     */
    public static boolean confirmDelete(String name) {
        List<String> names = NetworkStore.getInstance().getNetworkNames();
        if (names.size() <= 1) {
            JOptionPane.showMessageDialog(null,
                    "Das letzte Netzwerk kann nicht gelöscht werden.",
                    "Nicht möglich", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return JOptionPane.showConfirmDialog(null,
                "Netzwerk \"" + name + "\" und alle darin gespeicherten Hosts löschen?",
                "Netzwerk löschen",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }
}
