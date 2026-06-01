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
 * Dateiformat (CSV, Semikolon-getrennt, 6 Felder):
 *   IP;Hostname;OS;Datum;Ports;Notiz
 *
 * Rückwärtskompatibel: 4- und 5-Feld-Zeilen werden korrekt geladen.
 *
 * Speicherort: src/txt/saved_hosts.txt
 * (neben dem networktool_v3-Quellordner, im gleichen src-Verzeichnis)
 */
public final class SavedHostsStore {

    private static final class Holder {
        static final SavedHostsStore INSTANCE = new SavedHostsStore();
    }

    public static SavedHostsStore getInstance() { return Holder.INSTANCE; }

    private final List<HostResult> entries   = new ArrayList<>();
    private final List<Runnable>   listeners = new ArrayList<>();
    private Path filePath;

    private static final String            FILE_NAME   = "saved_hosts.txt";
    private static final String            TXT_SUBDIR  = "txt";
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
     * Pfad-Auflösung:
     *  IDE/Klassen: .../src/networktool_v3/  →  .../src/txt/saved_hosts.txt
     *  JAR:         neben der JAR            →  ./txt/saved_hosts.txt
     *  Fallback:    user.dir/txt/saved_hosts.txt
     */
    private static Path resolveDefaultPath() {
        try {
            URL classUrl  = SavedHostsStore.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path codeBase = Paths.get(classUrl.toURI());

            if (codeBase.toString().endsWith(".jar")) {
                // Neben der JAR → ./txt/
                return codeBase.getParent().resolve(TXT_SUBDIR).resolve(FILE_NAME);
            }

            // IDE/Klassen-Ausgabe-Ordner
            // codeBase = .../out/production/<project>/ oder .../build/classes/java/main/
            // Wir versuchen, den Paketpfad darunter zu finden
            String pkg    = SavedHostsStore.class.getPackageName().replace('.', '/');
            Path classDir = codeBase.resolve(pkg);

            // Hoch zu codeBase (Klassen-Root), dann Geschwister-Verzeichnis "txt"
            Path txtDir   = Files.isDirectory(classDir)
                    ? codeBase.resolve(TXT_SUBDIR)
                    : codeBase.getParent().resolve(TXT_SUBDIR);

            return txtDir.resolve(FILE_NAME);

        } catch (URISyntaxException | SecurityException ignored) {}

        return Paths.get(System.getProperty("user.dir"), TXT_SUBDIR, FILE_NAME);
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

    // ── Persistenz ────────────────────────────────────────────────────────

    private void loadFromFile() {
        if (!Files.exists(filePath)) { createEmptyFile(); return; }
        try {
            for (String line : Files.readAllLines(filePath)) {
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
            System.out.println("[SavedHostsStore] " + entries.size() + " Host(s) geladen.");
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Fehler beim Laden: " + e.getMessage());
        }
    }

    private synchronized void saveToFile() {
        try {
            Files.createDirectories(filePath.getParent());
            List<String> lines = entries.stream()
                    .map(e -> sanitize(e.ip)          + ";"
                            + sanitize(e.hostname)     + ";"
                            + sanitize(e.os)           + ";"
                            + sanitize(e.savedAt)      + ";"
                            + serializePorts(e.ports)  + ";"
                            + sanitizeNotes(e.notes))
                    .toList();
            Files.write(filePath, lines,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void createEmptyFile() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
            System.out.println("[SavedHostsStore] Neue Datei: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("SavedHostsStore: Konnte Datei nicht anlegen: " + e.getMessage());
        }
    }

    // ── Port-Serialisierung ───────────────────────────────────────────────

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
