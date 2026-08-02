package networktool.gui.panels;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import networktool.storage.NetworkStore;

import javax.swing.*;
import java.util.List;

/**
 * JOptionPane-Dialoge fÃƒÆ’Ã‚Â¼r Netzwerk-Verwaltungsaktionen im {@link GuiSavedHostsPanel}.
 *
 *  promptNew()    ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ neues Netzwerk anlegen (Name + PrÃƒÆ’Ã‚Â¤fix)
 *  promptRename() ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ aktives Netzwerk umbenennen
 *  confirmDelete()ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ aktives Netzwerk lÃƒÆ’Ã‚Â¶schen (mit Schutz)
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
                "<html>IP-PrÃƒÆ’Ã‚Â¤fix (optional):<br>"
                + "<small>Nur IPs die mit diesem PrÃƒÆ’Ã‚Â¤fix beginnen werden akzeptiert.<br>"
                + "Beispiele: 192.168.1.  |  10.0.0.  |  172.16.<br>"
                + "Leer = kein Filter</small></html>",
                "IP-PrÃƒÆ’Ã‚Â¤fix fÃƒÆ’Ã‚Â¼r " + name, JOptionPane.PLAIN_MESSAGE);
        if (prefix == null) return null;

        return new String[]{name.trim(), prefix.trim()};
    }

    /**
     * @return neuer Name oder null wenn abgebrochen
     */
    public static String promptRename(String currentName) {
        String newName = JOptionPane.showInputDialog(null,
                "Neuer Name fÃƒÆ’Ã‚Â¼r \"" + currentName + "\":",
                "Netzwerk umbenennen", JOptionPane.PLAIN_MESSAGE);
        return (newName == null || newName.isBlank()) ? null : newName.trim();
    }

    /**
     * @return true wenn LÃƒÆ’Ã‚Â¶schen bestÃƒÆ’Ã‚Â¤tigt wurde
     */
    public static boolean confirmDelete(String name) {
        List<String> names = NetworkStore.getInstance().getNetworkNames();
        if (names.size() <= 1) {
            JOptionPane.showMessageDialog(null,
                    "Das letzte Netzwerk kann nicht gelÃƒÆ’Ã‚Â¶scht werden.",
                    "Nicht mÃƒÆ’Ã‚Â¶glich", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return JOptionPane.showConfirmDialog(null,
                "Netzwerk \"" + name + "\" und alle darin gespeicherten Hosts lÃƒÆ’Ã‚Â¶schen?",
                "Netzwerk lÃƒÆ’Ã‚Â¶schen",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }
}


