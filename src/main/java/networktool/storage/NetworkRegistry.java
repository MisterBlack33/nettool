package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Manages named networks and their IP-prefix mapping. Package-private. */
final class NetworkRegistry {

    static final String ALL_CATEGORY  = "Alle";
    static final String DEFAULT_CAT   = "Standard";

    private final Map<String, List<HostResult>> networks = new LinkedHashMap<>();
    private final Map<String, String>           prefixes = new LinkedHashMap<>();

    NetworkRegistry() {}

    void ensureDefault() {
        if (networks.isEmpty()) networks.put(DEFAULT_CAT, new ArrayList<>());
    }

    boolean create(String name, String prefix) {
        if (name == null || name.isBlank() || name.equals(ALL_CATEGORY)) return false;
        String safe = safe(name);
        if (networks.containsKey(safe)) return false;
        networks.put(safe, new ArrayList<>());
        prefixes.put(safe, prefix != null ? prefix.trim() : "");
        return true;
    }

    boolean rename(String oldName, String newName, Path txtDir) {
        if (!networks.containsKey(oldName) || newName == null || newName.isBlank()) return false;
        String safe = safe(newName);
        if (networks.containsKey(safe)) return false;
        networks.put(safe, networks.remove(oldName));
        prefixes.put(safe, prefixes.remove(oldName));
        try {
            Files.deleteIfExists(NetworkStorePersistence.savedDir(txtDir)
                    .resolve(oldName + NetworkStorePersistence.FILE_EXT));
        } catch (IOException ignored) {}
        return true;
    }

    boolean delete(String name, Path txtDir) {
        if (!networks.containsKey(name)) return false;
        networks.remove(name);
        prefixes.remove(name);
        try {
            Files.deleteIfExists(NetworkStorePersistence.savedDir(txtDir)
                    .resolve(name + NetworkStorePersistence.FILE_EXT));
        } catch (IOException ignored) {}
        ensureDefault();
        return true;
    }

    List<String> names() {
        List<String> n = new ArrayList<>();
        n.add(ALL_CATEGORY);
        n.addAll(networks.keySet());
        return Collections.unmodifiableList(n);
    }

    String prefix(String cat) {
        return prefixes.getOrDefault(cat, "");
    }

    boolean ipMatches(String ip, String cat) {
        if (cat.equals(ALL_CATEGORY)) return true;
        String p = prefixes.getOrDefault(cat, "");
        return p.isBlank() || (ip != null && ip.startsWith(p));
    }

    List<String> matchingNetworks(String ip) {
        List<String> result = new ArrayList<>();
        for (String n : networks.keySet()) {
            if (ipMatches(ip, n)) result.add(n);
        }
        return result;
    }

    boolean contains(String name) {
        return networks.containsKey(name);
    }

    Map<String, List<HostResult>> networks() {
        return networks;
    }

    Map<String, String> prefixes() {
        return prefixes;
    }

    private static String safe(String s) {
        return s.replaceAll("[^a-zA-Z0-9äöüÄÖÜß \\-]", "_").trim();
    }
}