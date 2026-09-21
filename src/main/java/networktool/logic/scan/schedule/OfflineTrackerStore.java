package main.java.networktool.logic.scan.schedule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Persistiert "zuletzt gesehen" je Host als "ip TAB ms"-Zeilen, damit Neustarts den Zähler nicht zurücksetzen. */
final class OfflineTrackerStore {

    private static final String SEPARATOR = "\t";
    private static final int FIELD_COUNT  = 2;

    private OfflineTrackerStore() {}

    static Map<String, Long> load(Path file) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (!Files.exists(file)) return result;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) parseInto(line, result);
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
        return result;
    }

    static void save(Path file, Map<String, Long> lastSeen) throws IOException {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(lastSeen).forEach((ip, ms) -> sb.append(ip).append(SEPARATOR).append(ms).append('\n'));
        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void parseInto(String line, Map<String, Long> target) {
        String[] parts = line.split(SEPARATOR);
        if (parts.length != FIELD_COUNT) return;
        try {
            target.put(parts[0], Long.parseLong(parts[1].trim()));
        } catch (NumberFormatException ignored) {
            // beschädigte Zeile überspringen, Rest bleibt nutzbar
        }
    }
}
