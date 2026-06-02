package main.java.networktool.security;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Öffentliche API für das Audit-Log.
 *
 * Berechtigungen:
 *   - Schreiben: alle authentifizierten User.
 *   - clear():   nur User mit Rolle "admin" (toLowerCase-Vergleich).
 *
 * Persistenz:
 *   - Logs bleiben über Instanzen/Neustarts erhalten (NDJSON-Datei).
 *   - Altes Tab-Format und Legacy-JSON werden weiterhin gelesen.
 *   - init() muss nach jedem Start aufgerufen werden.
 */
public final class AuditLogger {

    private static final class Holder {
        static final AuditLogger INSTANCE = new AuditLogger();
    }
    public static AuditLogger getInstance() { return Holder.INSTANCE; }

    private volatile AuditLogFile logFile;

    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AuditLogger");
        t.setDaemon(true);
        return t;
    });

    private AuditLogger() {}

    /** Muss beim Start mit dem txt-Verzeichnis aufgerufen werden. */
    public void init(Path txtDir) {
        this.logFile = new AuditLogFile(txtDir);
    }

    // ── Schreiben ─────────────────────────────────────────────────────────

    public void log(String action) {
        log(action, "");
    }

    public void log(String action, String detail) {
        if (logFile == null) return;
        String user = currentUser();
        String ts   = AuditLogFile.nowFormatted();
        AuditLogEntry entry = new AuditLogEntry(ts, user, sanitize(action), sanitize(detail));
        writer.submit(() -> logFile.append(entry));
    }

    // ── Lesen ─────────────────────────────────────────────────────────────

    public List<AuditLogEntry> readRecent(int maxLines) {
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

    /**
     * Löscht das Audit-Log.
     * Nur für User mit Rolle "admin" erlaubt.
     * @throws SecurityException wenn kein Admin eingeloggt ist.
     */
    public void clear() {
        if (!isCurrentUserAdmin()) {
            throw new SecurityException("clear() erfordert Admin-Rechte.");
        }
        if (logFile == null) return;
        logFile.clear();
        log("AUDIT_LOG_CLEARED");
    }

    /** Stille Variante für interne Nutzung (z.B. Tests mit @TempDir). */
    public void clearInternal(Path txtDir) {
        AuditLogFile tmp = new AuditLogFile(txtDir);
        tmp.clear();
    }

    // ── Legacy-Kompatibilität ─────────────────────────────────────────────

    /** Parst eine einzelne Log-Zeile (alle Formate). Für Tests und externe Nutzung. */
    public static AuditLogEntry parse(String line) {
        return AuditLogFile.parse(line);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

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

    // ── Rückwärtskompatibilität (alte Tests / GUI nutzen LogEntry) ─────────

    /** @deprecated Nutze {@link AuditLogEntry} (Record). Nur für Kompatibilität. */
    @Deprecated
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

        public static LogEntry from(AuditLogEntry e) {
            return new LogEntry(e.timestamp(), e.user(), e.action(), e.detail());
        }
    }
}