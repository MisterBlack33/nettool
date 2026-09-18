package main.java.networktool.gui.panels.tags;

import main.java.networktool.storage.StorageLocations;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freitext-Tags und Favoriten pro Host-IP. Unabhängig von {@code NetworkStore}-
 * Kategorien, eigene Datei ({@code hostTags.json}), keine Änderung an bestehenden Speicherorten.
 */
public final class HostTagStore {

    private static final class Holder { static final HostTagStore INSTANCE = new HostTagStore(); }
    public static HostTagStore getInstance() { return Holder.INSTANCE; }

    private final Map<String, Set<String>> tagsByIp = new ConcurrentHashMap<>();
    private final Set<String> favorites = ConcurrentHashMap.newKeySet();
    private Path dataDir;

    private HostTagStore() {
        setDataDir(StorageLocations.networkData());
    }

    /** Für Tests: eigenes Verzeichnis setzen und neu laden. */
    public synchronized void setDataDir(Path dir) {
        this.dataDir = dir;
        tagsByIp.clear();
        favorites.clear();
        HostTagPersistence.load(dir).forEach((ip, entry) -> {
            tagsByIp.put(ip, ConcurrentHashMap.newKeySet());
            tagsByIp.get(ip).addAll(entry.tags());
            if (entry.favorite()) favorites.add(ip);
        });
    }

    public Set<String> getTags(String ip) {
        return Set.copyOf(tagsByIp.getOrDefault(ip, Set.of()));
    }

    public synchronized void addTag(String ip, String tag) {
        if (isBlank(ip) || isBlank(tag)) return;
        tagsByIp.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(tag.trim());
        persist();
    }

    public synchronized void removeTag(String ip, String tag) {
        Set<String> tags = tagsByIp.get(ip);
        if (tags == null) return;
        tags.remove(tag);
        persist();
    }

    public boolean isFavorite(String ip) { return favorites.contains(ip); }

    public synchronized void setFavorite(String ip, boolean fav) {
        if (isBlank(ip)) return;
        if (fav) favorites.add(ip); else favorites.remove(ip);
        persist();
    }

    /** Alle IPs, die mindestens einen Tag oder Favoriten-Status haben. */
    public Set<String> getAllTaggedIps() {
        Set<String> ips = new LinkedHashSet<>(tagsByIp.keySet());
        ips.addAll(favorites);
        return Collections.unmodifiableSet(ips);
    }

    public int favoriteCount() { return favorites.size(); }

    private void persist() {
        Map<String, HostTagPersistence.HostTagEntry> snapshot = new LinkedHashMap<>();
        for (String ip : getAllTaggedIps())
            snapshot.put(ip, new HostTagPersistence.HostTagEntry(
                    tagsByIp.getOrDefault(ip, Set.of()), favorites.contains(ip)));
        HostTagPersistence.save(dataDir, snapshot);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
