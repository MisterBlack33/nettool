package main.java.networktool.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Datei-I/O für das Audit-Log.
 * Format: NDJSON, eine Zeile pro Eintrag, Version 1.
 *   {"v":1,"ts":"2024-01-01 10:00:00","user":"alice","action":"LOGIN","detail":""}
 *
 * Rotation: ab MAX_LINES wird die aktuelle Datei archiviert.
 */
public final class AuditLogFile {

    static final int MAX_LINES = 200_000;
    public static final String FILE_NAME = "audit.log";

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ROT_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path logFile;

    public AuditLogFile(Path dataDir) {
        this.logFile = dataDir.resolve(FILE_NAME);
    }

    // ── Schreiben ─────────────────────────────────────────────────────────

    private volatile boolean testDirRemoved = false;

    public synchronized void append(AuditLogEntry entry) {
        try {
            Path parent = logFile.getParent();
            if (!Files.isDirectory(parent)) { testDirRemoved = true; return; }
            if (Files.exists(logFile) && countLines() >= MAX_LINES) rotate();
            Files.writeString(logFile, entry.toNdjson() + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AuditLog] write: " + e.getMessage());
        }
    }

    /** Löscht die aktuelle Log-Datei. Nur nach Berechtigungsprüfung aufrufen. */
    public synchronized void clear() {
        try { Files.deleteIfExists(logFile); }
        catch (IOException ignored) {}
    }

    // ── Lesen ─────────────────────────────────────────────────────────────

    public List<AuditLogEntry> readRecent(int maxLines) {
        if (!Files.exists(logFile)) return Collections.emptyList();
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<AuditLogEntry> result = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && result.size() < maxLines; i--) {
                AuditLogEntry e = parse(lines.get(i));
                if (e != null) result.add(e);
            }
            return Collections.unmodifiableList(result);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    // ── Parsen (NDJSON v1) ────────────────────────────────────────────────

    public static AuditLogEntry parse(String line) {
        if (line == null || line.isBlank() || !line.startsWith("{")) return null;
        String ts     = extractStr(line, "ts");
        String user   = extractStr(line, "user");
        String action = extractStr(line, "action");
        String detail = extractStr(line, "detail");
        if (ts == null || action == null) return null;
        return new AuditLogEntry(ts, user, action, detail);
    }

    static String extractStr(String json, String field) {
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
                    default   -> { sb.append(c); sb.append(nx); }
                }
            } else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private void rotate() throws IOException {
        Path rotated = logFile.resolveSibling(
                "audit_" + LocalDateTime.now().format(ROT_FMT) + ".log");
        Files.move(logFile, rotated, StandardCopyOption.REPLACE_EXISTING);
    }

    private long countLines() throws IOException {
        try (var r = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            return r.lines().count();
        }
    }

    static String nowFormatted() {
        return LocalDateTime.now().format(FMT);
    }
}