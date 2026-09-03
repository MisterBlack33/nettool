package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Thread-sicherer Speicher für gespeicherte Hosts mit Datei-Persistenz (Binärformat).
 * Speicherort: saves/networkdata/saved_hosts.bin (siehe {@link StorageLocations}).
 */
public final class SavedHostsStore {

    private static final class Holder {
        static final SavedHostsStore INSTANCE = new SavedHostsStore();
    }

    public static SavedHostsStore getInstance() { return Holder.INSTANCE; }

    private final List<HostResult> entries   = new ArrayList<>();
    private final List<Runnable>   listeners = new ArrayList<>();
    private Path filePath;

    private static final String            FILE_NAME   = "saved_hosts.bin";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SavedHostsStore() {
        filePath = resolveDefaultPath();
        System.out.println("[SavedHostsStore] Lade von: " + filePath.toAbsolutePath());
        loadFromFile();
    }

    public synchronized void setFilePath(Path path) {
        this.filePath = path;
        entries.clear();
        loadFromFile();
    }

    private static Path resolveDefaultPath() {
        return StorageLocations.networkData().resolve(FILE_NAME);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public synchronized void save(HostResult host) {
        if (host == null || isBlank(host.ip)) return;
        boolean exists = entries.stream().anyMatch(e -> e.ip.equals(host.ip));
        if (exists) {
            entries.stream().filter(e -> e.ip.equals(host.ip)).findFirst().ifPresent(e -> {
                if (host.ports != null && !host.ports.isEmpty()) {
                    e.ports.putAll(host.ports);
                    saveToFile();
                    notifyListeners();
                }
            });
            return;
        }
        host.savedAt = LocalDateTime.now().format(DATE_FORMAT);
        if (host.notes == null) host.notes = "";
        entries.add(host);
        saveToFile();
        notifyListeners();
    }

    /** Aktualisiert nur die Notiz eines Eintrags und persistiert sofort. */
    public synchronized void updateNotes(String ip, String notes) {
        if (isBlank(ip)) return;
        entries.stream().filter(e -> e.ip.equals(ip)).findFirst().ifPresent(e -> {
            e.notes = notes != null ? notes : "";
            saveToFile();
        });
    }

    public synchronized void remove(String ip) {
        if (isBlank(ip)) return;
        if (entries.removeIf(e -> e.ip.equals(ip))) { saveToFile(); notifyListeners(); }
    }

    public synchronized List<HostResult> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    // ── Persistenz (binär) ────────────────────────────────────────────────

    private void loadFromFile() {
        if (!Files.exists(filePath)) { createEmptyFile(); return; }
        try (var in = new java.io.DataInputStream(Files.newInputStream(filePath))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String ip = in.readUTF();
                if (isBlank(ip)) continue;
                String hostname = in.readUTF();
                String os = in.readUTF();
                String savedAt = in.readUTF();
                int portCount = in.readInt();
                Map<Integer, String> ports = new TreeMap<>();
                for (int p = 0; p < portCount; p++) {
                    int port = in.readInt();
                    String banner = in.readUTF();
                    ports.put(port, banner);
                }
                String notes = in.readUTF();
                entries.add(new HostResult(ip, hostname, os, savedAt, ports, notes));
            }
            System.out.println("[SavedHostsStore] " + entries.size() + " Host(s) geladen.");
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Laden fehlgeschlagen: " + e.getMessage());
        }
    }

    private synchronized void saveToFile() {
        try {
            Files.createDirectories(filePath.getParent());
            try (var out = new java.io.DataOutputStream(
                    Files.newOutputStream(filePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeInt(entries.size());
                for (HostResult e : entries) {
                    out.writeUTF(e.ip != null ? e.ip : "");
                    out.writeUTF(e.hostname != null ? e.hostname : "");
                    out.writeUTF(e.os != null ? e.os : "");
                    out.writeUTF(e.savedAt != null ? e.savedAt : "");
                    if (e.ports != null) {
                        out.writeInt(e.ports.size());
                        for (var entry : e.ports.entrySet()) {
                            out.writeInt(entry.getKey());
                            out.writeUTF(entry.getValue() != null ? entry.getValue() : "");
                        }
                    } else {
                        out.writeInt(0);
                    }
                    out.writeUTF(e.notes != null ? e.notes : "");
                }
            }
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void createEmptyFile() {
        try {
            Files.createDirectories(filePath.getParent());
            try (var out = new java.io.DataOutputStream(
                    Files.newOutputStream(filePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeInt(0);
            }
            System.out.println("[SavedHostsStore] Neue Datei: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Konnte Datei nicht anlegen: " + e.getMessage());
        }
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private void notifyListeners() {
        for (Runnable l : listeners) l.run();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}