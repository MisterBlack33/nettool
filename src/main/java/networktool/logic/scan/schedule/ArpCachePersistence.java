package main.java.networktool.logic.scan.schedule;

import main.java.networktool.storage.backup.CacheCrypto;
import main.java.networktool.storage.StorageLocations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optionale, verschlüsselte Persistenz für den ARP-Cache (IP↔MAC).
 * Speicherort: saves/cache/arp_cache.enc.
 *
 * Default: nur In-Memory (siehe {@link HostAliveChecker}) – Persistenz muss
 * explizit über {@link #enable(String)} aktiviert werden.
 *
 * Sicherheitsregeln:
 *  - Nie Klartext auf Platte. Bei jedem Crypto-/IO-Fehler wird der Eintrag
 *    verworfen statt unverschlüsselt geschrieben zu werden.
 *  - Eine harte TTL-Obergrenze (24h) gilt unabhängig vom Opt-in-Zustand.
 */
public final class ArpCachePersistence {

    private static final long   HARD_TTL_MS = 24 * 60 * 60 * 1000L;
    private static final String FILE_NAME   = "arp_cache.enc";

    private static volatile boolean enabled  = false;
    private static volatile String  password = null;

    /** Nur für Tests: überschreibt das Zielverzeichnis. */
    static volatile Path testDataDir = null;

    private ArpCachePersistence() {}

    // ── Opt-in ────────────────────────────────────────────────────────────

    public static void enable(String pw) {
        if (pw == null || pw.isBlank()) throw new IllegalArgumentException("Passwort erforderlich");
        password = pw;
        enabled  = true;
    }

    public static void disable() {
        enabled  = false;
        password = null;
        deleteQuietly();
    }

    public static boolean isEnabled() { return enabled; }

    // ── Speichern ─────────────────────────────────────────────────────────

    public static void save(Map<String, String> ipToMac) {
        if (!enabled || password == null) return;
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        ipToMac.forEach((ip, mac) ->
                sb.append(ip).append('\t').append(mac).append('\t').append(now).append('\n'));
        try {
            String encrypted = CacheCrypto.encrypt(sb.toString(), password);
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, encrypted, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            deleteQuietly();
        }
    }

    // ── Laden ─────────────────────────────────────────────────────────────

    /** Lädt gespeicherte Einträge; abgelaufene (>24h) Einträge werden immer verworfen. */
    public static Map<String, String> load(String pw) {
        Path file = file();
        if (!Files.exists(file)) return Map.of();
        try {
            String raw       = Files.readString(file, StandardCharsets.UTF_8);
            String decrypted = CacheCrypto.decrypt(raw, pw);
            return parseValid(decrypted);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, String> parseValid(String raw) {
        long now = System.currentTimeMillis();
        Map<String, String> result = new LinkedHashMap<>();
        boolean anyExpired = false;
        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", 3);
            if (parts.length < 3) continue;
            long ts;
            try { ts = Long.parseLong(parts[2]); } catch (NumberFormatException nfe) { continue; }
            if (new ArpCacheEntry(parts[0], parts[1], ts).isExpired(HARD_TTL_MS, now)) {
                anyExpired = true;
                continue;
            }
            result.put(parts[0], parts[1]);
        }
        if (anyExpired) deleteQuietly();
        return result;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static Path file() {
        Path dir = testDataDir != null ? testDataDir : StorageLocations.cache();
        return dir.resolve(FILE_NAME);
    }

    private static void deleteQuietly() {
        try { Files.deleteIfExists(file()); } catch (IOException ignored) {}
    }
}