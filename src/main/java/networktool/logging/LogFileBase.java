package main.java.networktool.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gemeinsamer Datei-Log-Kern für Audit- und Debug-Logs.
 * Persistiert Einträge als NDJSON, überlebt Neustarts/Instanzen.
 */
public class LogFileBase {

    private static final Logger LOG = Logger.getLogger(LogFileBase.class.getName());
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path file;
    private final int maxLines;
    private final Function<String, LogEntry> legacyParser;

    public LogFileBase(Path dataDir, String fileName, int maxLines,
                       Function<String, LogEntry> legacyParser) {
        this.file = dataDir.resolve(fileName);
        this.maxLines = maxLines;
        this.legacyParser = legacyParser;
    }

    // ── Schreiben ─────────────────────────────────────────────────────────

    public synchronized void append(LogEntry entry) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.toNdjson() + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            rotateIfNeeded();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Log-Eintrag konnte nicht geschrieben werden: " + file, e);
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Log-Datei konnte nicht gelöscht werden: " + file, e);
        }
    }

    private void rotateIfNeeded() throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() <= maxLines) return;
        List<String> trimmed = lines.subList(lines.size() - maxLines, lines.size());
        Files.write(file, trimmed, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ── Lesen ─────────────────────────────────────────────────────────────

    public synchronized List<LogEntry> readRecent(int maxLines) {
        if (!Files.exists(file)) return List.of();
        try {
            List<LogEntry> parsed = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                LogEntry e = parseLine(line);
                if (e != null) parsed.add(e);
            }
            int from = Math.max(0, parsed.size() - maxLines);
            List<LogEntry> recent = new ArrayList<>(parsed.subList(from, parsed.size()));
            Collections.reverse(recent);
            return recent;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Log-Datei konnte nicht gelesen werden: " + file, e);
            return List.of();
        }
    }

    private LogEntry parseLine(String line) {
        if (line == null || line.isBlank()) return null;
        LogEntry e = parseNdjson(line);
        if (e != null) return e;
        return legacyParser != null ? legacyParser.apply(line) : null;
    }

    // ── NDJSON-Parsing ────────────────────────────────────────────────────
    // Liest sowohl das aktuelle Schema (v2: category/user/level/action/detail)
    // als auch ältere Audit-v1-Zeilen (ts/user/action/detail) — gemeinsame
    // Feldmenge macht einen separaten Legacy-Parser dafür unnötig.

    public static LogEntry parseNdjson(String line) {
        if (line == null || line.isBlank()) return null;
        String t = line.trim();
        if (!t.startsWith("{") || !t.endsWith("}")) return null;
        String ts = extractField(t, "ts");
        if (ts == null) return null;
        return new LogEntry(ts,
                extractField(t, "category"),
                extractField(t, "user"),
                extractField(t, "level"),
                extractField(t, "action"),
                extractField(t, "detail"));
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length() || json.charAt(s) != '"') return null;
        s++;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case 'r'  -> sb.append('\r');
                    default   -> sb.append(nx);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ── Zeitstempel ───────────────────────────────────────────────────────

    public static String nowFormatted() {
        return LocalDateTime.now().format(TS_FMT);
    }
}