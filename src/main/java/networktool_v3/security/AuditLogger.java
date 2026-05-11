package main.java.networktool_v3.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Audit-Logger: Protokolliert alle Benutzeraktionen.
 *
 * Speicherort: txt/audit.log (JSON-Lines, eine JSON-Zeile pro Eintrag)
 *
 * Jeder Eintrag:
 * {"timestamp":"2026-04-30 14:32:01","user":"admin","action":"CIDR-Scan","detail":"192.168.1.0/24"}
 *
 * Thread-sicher. Schreibt asynchron in einen Hintergrundthread,
 * damit die UI nie blockiert.
 * Singleton.
 */
public final class AuditLogger {

    private static final class Holder { static final AuditLogger INSTANCE = new AuditLogger(); }
    public static AuditLogger getInstance() { return Holder.INSTANCE; }

    private static final String  FILE_NAME  = "audit.log";
    private static final int     MAX_LINES  = 10_000;   // Rotation ab 10k Einträgen
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Path logFile;
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "AuditLogger");
                t.setDaemon(true);
                return t;
            });

    private AuditLogger() {}

    /** Muss vor der ersten Nutzung aufgerufen werden. */
    public void init(Path txtDir) {
        try { Files.createDirectories(txtDir); } catch (IOException ignored) {}
        this.logFile = txtDir.resolve(FILE_NAME);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    /** Loggt eine Aktion ohne Detail. */
    public void log(String action) {
        log(action, "");
    }

    /** Loggt eine Aktion mit Detail (z.B. CIDR, IP, Dateiname). */
    public void log(String action, String detail) {
        if (logFile == null) return;
        String user = UserAuth.getInstance().getCurrentUser();
        if (user == null) user = "system";
        final String u = user;
        final String ts = LocalDateTime.now().format(FMT);
        writer.submit(() -> write(ts, u, action, detail));
    }

    /** Liest die letzten {@code maxLines} Einträge (neueste zuerst). */
    public List<LogEntry> readRecent(int maxLines) {
        if (logFile == null || !Files.exists(logFile)) return Collections.emptyList();
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<LogEntry> entries = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && entries.size() < maxLines; i--) {
                LogEntry e = parse(lines.get(i));
                if (e != null) entries.add(e);
            }
            return Collections.unmodifiableList(entries);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /** Gibt alle Einträge für einen bestimmten Benutzer zurück. */
    public List<LogEntry> readByUser(String username) {
        List<LogEntry> all   = readRecent(MAX_LINES);
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry e : all)
            if (username.equalsIgnoreCase(e.user)) result.add(e);
        return Collections.unmodifiableList(result);
    }

    /** Löscht das Audit-Log (nur für Admins gedacht). */
    public synchronized void clear() {
        if (logFile == null) return;
        try { Files.deleteIfExists(logFile); } catch (IOException ignored) {}
        log("AUDIT_LOG_CLEARED", "");
    }

    // ── Datenklasse ───────────────────────────────────────────────────────

    public static final class LogEntry {
        public final String timestamp;
        public final String user;
        public final String action;
        public final String detail;

        public LogEntry(String timestamp, String user, String action, String detail) {
            this.timestamp = timestamp;
            this.user      = user;
            this.action    = action;
            this.detail    = detail;
        }

        public String formatted() {
            return "[" + timestamp + "]  " + user + "  →  " + action
                    + (detail != null && !detail.isBlank() ? "  (" + detail + ")" : "");
        }
    }

    // ── Schreiben ─────────────────────────────────────────────────────────

    private synchronized void write(String ts, String user, String action, String detail) {
        try {
            Files.createDirectories(logFile.getParent());
            // Log-Rotation wenn zu groß
            if (Files.exists(logFile)) {
                long lines = countLines(logFile);
                if (lines >= MAX_LINES) rotate();
            }
            String entry = buildJson(ts, user, action, detail != null ? detail : "");
            Files.writeString(logFile, entry + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AuditLogger] Schreibfehler: " + e.getMessage());
        }
    }

    private static String buildJson(String ts, String user, String action, String detail) {
        return "{\"timestamp\":\"" + esc(ts) + "\","
                + "\"user\":\""       + esc(user)   + "\","
                + "\"action\":\""     + esc(action) + "\","
                + "\"detail\":\""     + esc(detail) + "\"}";
    }

    private void rotate() throws IOException {
        Path rotated = logFile.resolveSibling(
                "audit_" + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log");
        Files.move(logFile, rotated, StandardCopyOption.REPLACE_EXISTING);
    }

    private static long countLines(Path f) throws IOException {
        try (var reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            return reader.lines().count();
        }
    }

    private static LogEntry parse(String line) {
        if (line == null || !line.startsWith("{")) return null;
        String ts     = extractStr(line, "timestamp");
        String user   = extractStr(line, "user");
        String action = extractStr(line, "action");
        String detail = extractStr(line, "detail");
        if (ts == null || user == null || action == null) return null;
        return new LogEntry(ts, user, action, detail != null ? detail : "");
    }

    private static String extractStr(String json, String field) {
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
            if (c == '\\' && i + 1 < json.length()) { sb.append(json.charAt(++i)); }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}