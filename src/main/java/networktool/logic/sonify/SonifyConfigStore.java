package main.java.networktool.logic.sonify;

import main.java.networktool.storage.StorageUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persistiert {@link SonifyConfig} über App-Neustarts hinweg (data/sonifyConfig.json). */
public final class SonifyConfigStore {

    private static final String FILE_NAME = "sonifyConfig.json";
    private static volatile SonifyConfig cached;

    private SonifyConfigStore() {}

    public static synchronized SonifyConfig load() {
        if (cached != null) return cached.copy();
        Path file = StorageUtils.resolveDataDir().resolve(FILE_NAME);
        SonifyConfig cfg = new SonifyConfig();
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                cfg.highHz = extractInt(json, "highHz", cfg.highHz);
                cfg.lowHz  = extractInt(json, "lowHz",  cfg.lowHz);
                cfg.toneMs = extractInt(json, "toneMs", cfg.toneMs);
            } catch (IOException ignored) {}
        }
        cached = cfg;
        return cfg.copy();
    }

    public static synchronized void save(SonifyConfig cfg) {
        cached = cfg.copy();
        Path file = StorageUtils.resolveDataDir().resolve(FILE_NAME);
        String json = "{\n  \"highHz\": " + cfg.highHz + ",\n  \"lowHz\": " + cfg.lowHz
                + ",\n  \"toneMs\": " + cfg.toneMs + "\n}";
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[SonifyConfigStore] Speichern: " + e.getMessage());
        }
    }

    private static int extractInt(String json, String key, int fallback) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }
}