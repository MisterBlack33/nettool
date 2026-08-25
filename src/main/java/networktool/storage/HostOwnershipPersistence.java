package main.java.networktool.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** JSON-Persistenz für Host-Besitzer-Referenzen. Package-private. */
final class HostOwnershipPersistence {

    private HostOwnershipPersistence() {}

    static final String FILE_NAME = "hostOwnership.json";

    static Map<String, Set<String>> load(Path dataDir) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        Path file = dataDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return result;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            int arrStart = JsonHelper.findArrayStart(json, "entries");
            if (arrStart < 0) return result;
            for (String obj : JsonHelper.extractObjects(json, arrStart)) {
                String key = JsonHelper.extractStr(obj, "key");
                if (key == null) continue;
                result.put(key, new LinkedHashSet<>(JsonHelper.extractStringArray(obj, "owners")));
            }
        } catch (IOException e) {
            System.err.println("[HostOwnership] load: " + e.getMessage());
        }
        return result;
    }

    static void save(Path dataDir, Map<String, Set<String>> data) {
        StringBuilder sb = new StringBuilder("{\n  \"entries\": [\n");
        int i = 0, n = data.size();
        for (Map.Entry<String, Set<String>> e : data.entrySet()) {
            sb.append("    {\"key\": \"").append(JsonHelper.esc(e.getKey())).append("\", \"owners\": [");
            appendOwners(sb, e.getValue());
            sb.append("]}").append(++i < n ? "," : "").append("\n");
        }
        sb.append("  ]\n}");
        try {
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve(FILE_NAME), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[HostOwnership] save: " + e.getMessage());
        }
    }

    private static void appendOwners(StringBuilder sb, Set<String> owners) {
        List<String> list = new ArrayList<>(owners);
        for (int j = 0; j < list.size(); j++) {
            sb.append("\"").append(JsonHelper.esc(list.get(j))).append("\"");
            if (j < list.size() - 1) sb.append(", ");
        }
    }
}