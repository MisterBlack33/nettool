package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.saved.SavedHostsTagFilter;
import main.java.networktool.gui.panels.tags.HostTagStore;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.network.NetworkStore;

import java.util.List;

/** Test-Suite-Aktion "Tag-Filter" (Menü-ID "42"): zeigt gespeicherte Hosts nach Tag oder Favorit. */
public final class GuiTagFilterActions {

    private GuiTagFilterActions() {}

    public static void handle(GuiInputPanel input, GuiTableRenderer tables) {
        input.ask("Tag (leer = nur Favoriten):", raw -> apply(raw, tables));
    }

    static void apply(String rawTag, GuiTableRenderer tables) {
        List<HostResult> hits = SavedHostsTagFilter.select(
                NetworkStore.getInstance().getAllHosts(), rawTag, HostTagStore.getInstance());
        boolean favorites = rawTag == null || rawTag.isBlank();
        tables.showHostTable(hits, favorites ? "Favoriten" : "Tag: " + rawTag.trim());
    }
}
