package main.java.networktool_v3.gui;

import main.java.networktool_v3.storage.StorageUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Persistiert manuell markierte Switch-IPs in txt/mapSwitches.json.
 * Lädt beim Start automatisch.
 */
final class MapSwitchStore {

    private MapSwitchStore() {}

    static final Set<String> SWITCHES = Collections.synchronizedSet(new HashSet<>());
    private static final String FILE = "mapSwitches.json";

    static {
        load();
    }

    static void add(String ip) {
        SWITCHES.add(ip);
        persist();
    }

    static void remove(String ip) {
        SWITCHES.remove(ip);
        persist();
    }

    static void clear() {
        SWITCHES.clear();
        persist();
    }

    static boolean contains(String ip) {
        return SWITCHES.contains(ip);
    }

    private static void load() {
        try {
            Path file = StorageUtils.resolveTxtDir().resolve(FILE);
            if (!Files.exists(file)) return;
            String raw = Files.readString(file, StandardCharsets.UTF_8)
                    .trim().replaceAll("^\\[|]$", "");
            for (String part : raw.split(",")) {
                String ip = part.trim().replaceAll("^\"|\"$", "");
                if (!ip.isBlank()) SWITCHES.add(ip);
            }
        } catch (Exception ignored) {}
    }

    private static void persist() {
        try {
            Path dir = StorageUtils.resolveTxtDir();
            Files.createDirectories(dir);
            List<String> sorted = new ArrayList<>(SWITCHES);
            Collections.sort(sorted);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(sorted.get(i)).append("\"");
            }
            Files.writeString(dir.resolve(FILE), sb.append("]").toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }
}