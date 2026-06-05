package main.java.networktool.security;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class AuditLogger {

    private static final class Holder {
        static final AuditLogger INSTANCE = new AuditLogger();
    }
    public static AuditLogger getInstance() { return Holder.INSTANCE; }

    private volatile AuditLogFile logFile;

    private volatile ExecutorService writer = newWriter();

    private AuditLogger() {}

    public void init(Path txtDir) {
        flushAndShutdown();
        this.logFile = new AuditLogFile(txtDir);
        this.writer  = newWriter();
    }

    // ── Schreiben ─────────────────────────────────────────────────────────

    public void log(String action) { log(action, ""); }

    public void log(String action, String detail) {
        if (logFile == null) return;
        String user  = currentUser();
        String ts    = AuditLogFile.nowFormatted();
        AuditLogEntry entry = new AuditLogEntry(ts, user, sanitize(action), sanitize(detail));
        writer.submit(() -> logFile.append(entry));
    }

    // ── Lesen ─────────────────────────────────────────────────────────────

    public List<AuditLogEntry> readRecent(int maxLines) {
        flush();
        if (logFile == null) return Collections.emptyList();
        return logFile.readRecent(maxLines);
    }

    public List<AuditLogEntry> readByUser(String username) {
        if (username == null) return Collections.emptyList();
        return readRecent(AuditLogFile.MAX_LINES).stream()
                .filter(e -> username.equalsIgnoreCase(e.user()))
                .toList();
    }

    // ── Löschen (nur Admin) ───────────────────────────────────────────────

    public void clear() {
        if (!isCurrentUserAdmin())
            throw new SecurityException("clear() erfordert Admin-Rechte.");
        flush();
        if (logFile == null) return;
        logFile.clear();
        log("AUDIT_LOG_CLEARED");
    }

    public void clearInternal(Path txtDir) {
        new AuditLogFile(txtDir).clear();
    }

    /** Wartet bis alle gepufferten Log-Einträge geschrieben sind. */
    public void flush() {
        try {
            Future<?> f = writer.submit(() -> {});
            f.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    /** Schließt den Writer-Thread (für Tests / Shutdown). */
    public void shutdown() {
        flushAndShutdown();
        writer = newWriter();
    }

    // ── Legacy ────────────────────────────────────────────────────────────

    public static AuditLogEntry parse(String line) { return AuditLogFile.parse(line); }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private void flushAndShutdown() {
        try {
            writer.shutdown();
            writer.awaitTermination(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private static ExecutorService newWriter() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuditLogger");
            t.setDaemon(true);
            return t;
        });
    }

    private static String currentUser() {
        String u = UserAuth.getInstance().getCurrentUser();
        return u != null ? u : "system";
    }

    private static boolean isCurrentUserAdmin() {
        String role = UserAuth.getInstance().getCurrentRole();
        return "admin".equals(role != null ? role.toLowerCase() : "");
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\n", " ").replace("\r", "");
    }

    @Deprecated
    public static final class LogEntry {
        public final String timestamp, user, action, detail;
        public LogEntry(String timestamp, String user, String action, String detail) {
            this.timestamp = timestamp; this.user = user;
            this.action = action; this.detail = detail != null ? detail : "";
        }
        public static LogEntry from(AuditLogEntry e) {
            return new LogEntry(e.timestamp(), e.user(), e.action(), e.detail());
        }
    }
}