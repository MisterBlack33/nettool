package main.java.networktool.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** ntfy topic persistence. Package-private. */
final class NetworkStoreNtfy {

    private NetworkStoreNtfy() {}

    static List<String> loadTopics(Path dataDir) {
        Path file = dataDir.resolve(NetworkStorePersistence.NTFY_FILE);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            return JsonHelper.extractStringArray(
                    Files.readString(file, StandardCharsets.UTF_8), "topics");
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    static void saveTopic(Path dataDir, String topic) {
        if (topic == null || topic.isBlank()) return;
        try {
            Files.createDirectories(dataDir);
            List<String> existing = new ArrayList<>(loadTopics(dataDir));
            if (existing.contains(topic)) return;
            existing.add(topic);
            Collections.sort(existing);
            Files.writeString(dataDir.resolve(NetworkStorePersistence.NTFY_FILE),
                    JsonHelper.buildStringArrayJson("topics", existing),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }
}