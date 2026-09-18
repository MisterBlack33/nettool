package main.java.networktool.logic.analysis.probe;

import main.java.networktool.storage.StorageLocations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistiert Bandbreiten-Messungen (Down/Up-Mbps) je Host als NDJSON,
 * eine Datei pro IP unter saves/cache/bandwidthHistory/. Rotation hält
 * die letzten {@link #MAX_ENTRIES} Einträge je Host.
 */
public final class BandwidthHistoryStore {

    private static final class Holder { static final BandwidthHistoryStore INSTANCE = new BandwidthHistoryStore(); }
    public static BandwidthHistoryStore getInstance() { return Holder.INSTANCE; }

    private static final Logger LOG = Logger.getLogger(BandwidthHistoryStore.class.getName());
    public static final int MAX_ENTRIES = 500;
    private static final String SUBDIR = "bandwidthHistory";

    private BandwidthHistoryStore() {}

    public synchronized void record(String ip, double downMbps, double upMbps) {
        if (ip == null || ip.isBlank()) return;
        BandwidthHistoryEntry entry = new BandwidthHistoryEntry(
                System.currentTimeMillis(), ip, downMbps, upMbps);
        Path file = fileFor(ip);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.toNdjson() + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            rotateIfNeeded(file);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Bandbreiten-Historie konnte nicht geschrieben werden: " + ip, e);
        }
    }

    public synchronized List<BandwidthHistoryEntry> getHistory(String ip) {
        Path file = fileFor(ip);
        if (!Files.exists(file)) return List.of();
        try {
            List<BandwidthHistoryEntry> result = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                BandwidthHistoryEntry e = BandwidthHistoryEntry.parse(line);
                if (e != null) result.add(e);
            }
            return Collections.unmodifiableList(result);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Bandbreiten-Historie konnte nicht gelesen werden: " + ip, e);
            return List.of();
        }
    }

    public synchronized void clear(String ip) {
        try { Files.deleteIfExists(fileFor(ip)); }
        catch (IOException e) { LOG.log(Level.WARNING, "Bandbreiten-Historie konnte nicht gelöscht werden: " + ip, e); }
    }

    private void rotateIfNeeded(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() <= MAX_ENTRIES) return;
        List<String> trimmed = lines.subList(lines.size() - MAX_ENTRIES, lines.size());
        Files.write(file, trimmed, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Path fileFor(String ip) {
        return StorageLocations.cache().resolve(SUBDIR).resolve(sanitize(ip) + ".ndjson");
    }

    private static String sanitize(String ip) {
        return ip.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
}
