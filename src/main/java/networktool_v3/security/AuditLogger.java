package main.java.networktool_v3.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tab-separated audit log. Format: timestamp\tuser\taction\tdetail
 * Appends across restarts. Rotates at MAX_LINES.
 */
public final class AuditLogger {

    private static final class Holder { static final AuditLogger INSTANCE = new AuditLogger(); }
    public static AuditLogger getInstance() { return Holder.INSTANCE; }

    public static final int MAX_LINES = 200_000;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Path logFile;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AuditLogger");
        t.setDaemon(true);
        return t;
    });

    private AuditLogger() {}

    public void init(Path txtDir) {
        try { Files.createDirectories(txtDir); } catch (IOException ignored) {}
        this.logFile = txtDir.resolve("audit.log");
    }

    public void log(String action)               { log(action, ""); }
    public void log(String action, String detail) {
        if (logFile == null) return;
        String user = Optional.ofNullable(UserAuth.getInstance().getCurrentUser()).orElse("system");
        String ts   = LocalDateTime.now().format(FMT);
        writer.submit(() -> write(ts, user, action, detail != null ? detail : ""));
    }

    public List<LogEntry> readRecent(int maxLines) {
        if (logFile == null || !Files.exists(logFile)) return Collections.emptyList();
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<LogEntry> result = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && result.size() < maxLines; i--) {
                LogEntry e = parse(lines.get(i));
                if (e != null) result.add(e);
            }
            return Collections.unmodifiableList(result);
        } catch (IOException e) { return Collections.emptyList(); }
    }

    public List<LogEntry> readByUser(String username) {
        return readRecent(MAX_LINES).stream()
                .filter(e -> username.equalsIgnoreCase(e.user))
                .toList();
    }

    public synchronized void clear() {
        if (logFile == null) return;
        try { Files.deleteIfExists(logFile); } catch (IOException ignored) {}
        log("AUDIT_LOG_CLEARED");
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private synchronized void write(String ts, String user, String action, String detail) {
        try {
            Files.createDirectories(logFile.getParent());
            if (Files.exists(logFile) && countLines() >= MAX_LINES) rotate();
            String line = ts + "\t" + san(user) + "\t" + san(action) + "\t" + san(detail);
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AuditLogger] write error: " + e.getMessage());
        }
    }

    private void rotate() throws IOException {
        Path rotated = logFile.resolveSibling(
                "audit_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log");
        Files.move(logFile, rotated, StandardCopyOption.REPLACE_EXISTING);
    }

    private long countLines() throws IOException {
        try (var r = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            return r.lines().count();
        }
    }

    public static LogEntry parse(String line) {
        if (line == null || line.isBlank()) return null;
        if (line.startsWith("{")) return parseLegacyJson(line);
        String[] p = line.split("\t", 4);
        if (p.length < 3) return null;
        return new LogEntry(p[0], p[1], p[2], p.length >= 4 ? p[3] : "");
    }

    private static LogEntry parseLegacyJson(String json) {
        String ts     = extractJson(json, "timestamp");
        String user   = extractJson(json, "user");
        String action = extractJson(json, "action");
        String detail = extractJson(json, "detail");
        if (ts == null || user == null || action == null) return null;
        return new LogEntry(ts, user, action, detail);
    }

    private static String extractJson(String json, String field) {
        return UserAuth.extractStr(json, field);
    }

    private static String san(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\n", " ").replace("\r", "");
    }

    // ── Data class ────────────────────────────────────────────────────────

    public static final class LogEntry {
        public final String timestamp;
        public final String user;
        public final String action;
        public final String detail;

        public LogEntry(String timestamp, String user, String action, String detail) {
            this.timestamp = timestamp;
            this.user      = user;
            this.action    = action;
            this.detail    = detail != null ? detail : "";
        }
    }
}