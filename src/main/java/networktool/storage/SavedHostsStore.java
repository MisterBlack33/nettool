package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Thread-sicherer Speicher für gespeicherte Hosts mit Datei-Persistenz.
 *
 * Früher wurde ein CSV-/TXT-Format verwendet. Das wurde auf ein kompaktes
 * Binärformat umgestellt (DataOutputStream/DataInputStream) für geringe
 * Speichergröße und schnelle Lese-/Schreibvorgänge.
 *
 * Speicherort: src/data/saved_hosts.bin (bzw. ./data/saved_hosts.bin neben JAR)
 * Legacy: saved_hosts.txt wird bei Bedarf automatisch migriert.
 */
public final class SavedHostsStore {

    private static final class Holder {
        static final SavedHostsStore INSTANCE = new SavedHostsStore();
    }

    public static SavedHostsStore getInstance() { return Holder.INSTANCE; }

    private final List<HostResult> entries   = new ArrayList<>();
    private final List<Runnable>   listeners = new ArrayList<>();
    private Path filePath;

    // Neuer, effizienter Binär-Store
    private static final String            FILE_NAME   = "saved_hosts.bin";
    private static final String            DATA_SUBDIR  = "data"; // früher: "txt" (wird migriert)
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

    /**
     * Pfad-Auflösung bleibt kompatibel zu früheren Layouts, liefert aber jetzt
     * den Ordnername "data" statt früher "txt".
     */
    private static Path resolveDefaultPath() {
        try {
            URL classUrl  = SavedHostsStore.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path codeBase = Paths.get(classUrl.toURI());

            if (codeBase.toString().endsWith(".jar")) {
                // Neben der JAR → ./data/
                return codeBase.getParent().resolve(DATA_SUBDIR).resolve(FILE_NAME);
            }

            // IDE/Klassen-Ausgabe-Ordner
            String pkg    = SavedHostsStore.class.getPackageName().replace('.', '/');
            Path classDir = codeBase.resolve(pkg);

            // Hoch zu codeBase (Klassen-Root), dann Geschwister-Verzeichnis "data"
            Path dataDir   = Files.isDirectory(classDir)
                    ? codeBase.resolve(DATA_SUBDIR)
                    : codeBase.getParent().resolve(DATA_SUBDIR);

            return dataDir.resolve(FILE_NAME);

        } catch (URISyntaxException | SecurityException ignored) {}

        return Paths.get(System.getProperty("user.dir"), DATA_SUBDIR, FILE_NAME);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public synchronized void save(HostResult host) {
        if (host == null || isBlank(host.ip)) return;
        boolean exists = entries.stream().anyMatch(e -> e.ip.equals(host.ip));
        if (exists) {
            entries.stream().filter(e -> e.ip.equals(host.ip)).findFirst().ifPresent(e -> {
                if (host.ports != null && !host.ports.isEmpty()) {
                    e.ports.putAll(host.ports);
                    persistAndNotify();
                }
            });
            return;
        }
        host.savedAt = LocalDateTime.now().format(DATE_FORMAT);
        if (host.notes == null) host.notes = "";
        entries.add(host);
        persistAndNotify();
    }

    /** Aktualisiert nur die Notiz eines Eintrags und persistiert sofort. */
    public synchronized void updateNotes(String ip, String notes) {
        if (isBlank(ip)) return;
        entries.stream().filter(e -> e.ip.equals(ip)).findFirst().ifPresent(e -> {
            e.notes = notes != null ? notes : "";
            saveToFile(); // kein notifyListeners() → kein Store-Changed-Banner
        });
    }

    public synchronized void remove(String ip) {
        if (isBlank(ip)) return;
        if (entries.removeIf(e -> e.ip.equals(ip))) persistAndNotify();
    }

    public synchronized List<HostResult> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    // ── Persistenz (binär, speichere kompakt) ─────────────────────────────────────

    private void loadFromFile() {
        // Wenn Binärdatei vorhanden → binär laden
        if (Files.exists(filePath)) {
            try (var in = new java.io.DataInputStream(
                    java.nio.file.Files.newInputStream(filePath))) {
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
                System.out.println("[SavedHostsStore] " + entries.size() + " Host(s) geladen (bin).");
                return;
            } catch (IOException e) {
                System.err.println("SavedHostsStore: Binär-Laden fehlgeschlagen: " + e.getMessage());
                // fallthrough → versuche Legacy-Textmigration
            }
        }

        // Keine Binärdatei → prüfen, ob noch Legacy-Textdatei existiert und migrieren
        Path legacy = filePath.getParent().resolve("saved_hosts.txt");
        if (Files.exists(legacy)) {
            try {
                for (String line : Files.readAllLines(legacy)) {
                    if (isBlank(line)) continue;
                    String[] parts = line.split(";", 6);
                    if (parts.length < 4) continue;
                    String ip = parts[0].trim();
                    if (isBlank(ip)) continue;
                    Map<Integer, String> ports = parts.length >= 5
                            ? parsePorts(parts[4].trim()) : new TreeMap<>();
                    String notes = parts.length >= 6 ? parts[5].trim() : "";
                    entries.add(new HostResult(
                            ip, parts[1].trim(), parts[2].trim(),
                            parts[3].trim(), ports, notes));
                }
                System.out.println("[SavedHostsStore] " + entries.size() + " Host(s) geladen (legacy txt) - migriere zu bin.");
                saveToFile();
                return;
            } catch (IOException e) {
                System.err.println("SavedHostsStore: Fehler beim Laden der Legacy-Datei: " + e.getMessage());
            }
        }

        // Keine Datei gefunden → leere Binärdatei anlegen
        createEmptyFile();
    }

    private synchronized void saveToFile() {
        try {
            Files.createDirectories(filePath.getParent());
            try (var out = new java.io.DataOutputStream(
                    java.nio.file.Files.newOutputStream(filePath,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))) {
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
            // lege leere Binärdatei mit 0 Einträgen an
            try (var out = new java.io.DataOutputStream(
                    java.nio.file.Files.newOutputStream(filePath,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeInt(0);
            }
            System.out.println("[SavedHostsStore] Neue Datei (bin): " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Konnte Datei nicht anlegen: " + e.getMessage());
        }
    }

    // ── Port-Serialisierung (nur noch für Legacy-Parsen) ─────────────────────────

    private static Map<Integer, String> parsePorts(String s) {
        Map<Integer, String> map = new TreeMap<>();
        if (isBlank(s)) return map;
        for (String entry : s.split(",")) {
            String[] kv = entry.split(":", 2);
            try { map.put(Integer.parseInt(kv[0].trim()),
                          kv.length > 1 ? kv[1].trim() : "offen"); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private static String serializePorts(Map<Integer, String> ports) {
        if (ports == null || ports.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        ports.forEach((port, banner) -> {
            if (sb.length() > 0) sb.append(",");
            sb.append(port).append(":").append(banner.replace(",", "|").replace(";", "|"));
        });
        return sb.toString();
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private void persistAndNotify() { saveToFile(); notifyListeners(); }

    private void notifyListeners() {
        for (Runnable l : listeners) l.run();  // direkt statt invokeLater
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    private static String sanitizeNotes(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
