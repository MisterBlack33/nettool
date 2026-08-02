package networktool.gui.components;

import networktool.util.*;
import networktool.gui.login.*;
import networktool.gui.hostdetails.*;
import networktool.gui.map.*;
import networktool.gui.core.*;
import networktool.gui.components.*;
import networktool.gui.panels.*;
import networktool.storage.NetworkStore;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Einheitlicher ntfy-Topic-Auswahl-Dialog.
 * Wird von {@link GuiContextMenu} und {@link GuiMenuHandler} verwendet.
 */
public final class NtfyTopicPrompt {

    private static final String OPT_NONE = "(kein ntfy-Push)";
    private static final String OPT_NEW  = "+ Neues Topic eingeben...";

    private NtfyTopicPrompt() {}

    /**
     * Zeigt Dropdown mit: kein Push / gespeicherte Topics / neues Topic eingeben.
     *
     * @return gewÃƒÆ’Ã‚Â¤hltes Topic (leer = kein Push), oder null bei Abbruch
     */
    public static String prompt() {
        List<String> options = buildOptions();

        Object chosen = JOptionPane.showInputDialog(null,
                "<html><b>ntfy-Topic wÃƒÆ’Ã‚Â¤hlen</b><br>"
                        + "<small>Handy-Push: ntfy-App installieren, gleiches Topic abonnieren.<br>"
                        + "Topics werden alphabetisch gespeichert.</small></html>",
                "ntfy-Topic", JOptionPane.QUESTION_MESSAGE, null,
                options.toArray(), options.get(0));

        if (chosen == null) return null;
        String selected = chosen.toString();

        if (OPT_NEW.equals(selected))  return promptNewTopic();
        if (OPT_NONE.equals(selected)) return "";
        return selected;
    }

    private static List<String> buildOptions() {
        List<String> saved = new ArrayList<>(NetworkStore.getInstance().getNtfyTopics());
        Collections.sort(saved);

        List<String> options = new ArrayList<>();
        options.add(OPT_NONE);
        options.addAll(saved);
        options.add(OPT_NEW);
        return options;
    }

    private static String promptNewTopic() {
        String topic = JOptionPane.showInputDialog(null,
                "Neues ntfy-Topic eingeben:", "Neues Topic", JOptionPane.PLAIN_MESSAGE);
        return topic != null ? topic.trim() : null;
    }
}

