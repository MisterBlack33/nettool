package main.java.networktool.gui.panels.tags;

import main.java.networktool.storage.JsonCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Lädt/speichert {@code saves/networkdata/hostTags.json}. Package-private I/O für {@link HostTagStore}. */
final class HostTagPersistence {

    private static final String FILE_NAME = "hostTags.json";

    private HostTagPersistence() {}

    static Map<String, HostTagEntry> load(Path dataDir) {
        Map<String, HostTagEntry> result = new LinkedHashMap<>();
        Path file = dataDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return result;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            int arrStart = JsonCodec.findArrayStart(json, "tags");
            if (arrStart < 0) return result;
            for (String obj : JsonCodec.extractObjects(json, arrStart)) {
                String ip = JsonCodec.extractStr(obj, "ip");
                if (ip == null || ip.isBlank()) continue;
                Set<String> tags = new LinkedHashSet<>(JsonCodec.extractStringArray(obj, "tags"));
                boolean fav = "true".equalsIgnoreCase(JsonCodec.extractStr(obj, "favorite"));
                result.put(ip, new HostTagEntry(tags, fav));
            }
        } catch (IOException e) {
            System.err.println("[HostTagStore] Laden: " + e.getMessage());
        }
        return result;
    }

    static void save(Path dataDir, Map<String, HostTagEntry> entries) {
        try {
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve(FILE_NAME), toJson(entries), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[HostTagStore] Speichern: " + e.getMessage());
        }
    }

    private static String toJson(Map<String, HostTagEntry> entries) {
        StringBuilder sb = new StringBuilder("{\n  \"tags\": [\n");
        int i = 0, n = entries.size();
        for (Map.Entry<String, HostTagEntry> e : entries.entrySet()) {
            sb.append("    {\"ip\":\"").append(JsonCodec.esc(e.getKey())).append("\",")
                    .append("\"tags\":[").append(tagArray(e.getValue().tags())).append("],")
                    .append("\"favorite\":").append(e.getValue().favorite()).append("}");
            if (++i < n) sb.append(",");
            sb.append("\n");
        }
        return sb.append("  ]\n}").toString();
    }

    private static String tagArray(Set<String> tags) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = tags.size();
        for (String t : tags) {
            sb.append("\"").append(JsonCodec.esc(t)).append("\"");
            if (++i < n) sb.append(",");
        }
        return sb.toString();
    }

    /** Unveränderlicher Ladezustand eines Hosts: Tags + Favoriten-Flag. */
    record HostTagEntry(Set<String> tags, boolean favorite) {
        HostTagEntry {
            tags = tags != null ? new LinkedHashSet<>(tags) : new LinkedHashSet<>();
        }
    }
}
