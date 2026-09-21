package main.java.networktool.gui.panels.saved;

import main.java.networktool.gui.panels.tags.HostTagStore;
import main.java.networktool.model.HostResult;

import java.util.List;
import java.util.Locale;

/** Reine Filterlogik für Tags/Favoriten gespeicherter Hosts (kein Swing). */
public final class SavedHostsTagFilter {

    private SavedHostsTagFilter() {}

    public static List<HostResult> byTag(List<HostResult> hosts, String tag, HostTagStore store) {
        if (tag == null || tag.isBlank()) return List.of();
        String wanted = tag.trim().toLowerCase(Locale.ROOT);
        return hosts.stream().filter(h -> hasTag(store, h.ip, wanted)).toList();
    }

    public static List<HostResult> favorites(List<HostResult> hosts, HostTagStore store) {
        return hosts.stream().filter(h -> store.isFavorite(h.ip)).toList();
    }

    /** Leerer Tag bedeutet: nur Favoriten. */
    public static List<HostResult> select(List<HostResult> hosts, String rawTag, HostTagStore store) {
        boolean noTag = rawTag == null || rawTag.isBlank();
        return noTag ? favorites(hosts, store) : byTag(hosts, rawTag, store);
    }

    private static boolean hasTag(HostTagStore store, String ip, String wanted) {
        return store.getTags(ip).stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).equals(wanted));
    }
}
