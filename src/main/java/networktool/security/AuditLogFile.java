package main.java.networktool.security;

import main.java.networktool.logging.LogEntry;
import main.java.networktool.logging.LogFileBase;

import java.nio.file.Path;
import java.util.List;

/**
 * Datei-I/O für das Audit-Log. Delegiert an {@link LogFileBase} (gemeinsamer
 * Log-Kern mit DebugLogFile). Alte Audit-v1-NDJSON-Zeilen bleiben lesbar, da
 * deren Feldmenge eine Teilmenge des vereinheitlichten Schemas ist.
 */
public final class AuditLogFile {

    static final int MAX_LINES = 200_000;
    public static final String FILE_NAME = "audit.log";

    private final LogFileBase base;

    public AuditLogFile(Path dataDir) {
        // Kein quellenspezifischer Legacy-Parser nötig: Audit-v1-NDJSON wird
        // bereits von LogFileBase.parseNdjson() korrekt gelesen.
        this.base = new LogFileBase(dataDir, FILE_NAME, MAX_LINES, line -> null);
    }

    public void append(AuditLogEntry entry) {
        base.append(new LogEntry(entry.timestamp(), "AUDIT", entry.user(), "", entry.action(), entry.detail()));
    }

    public void clear() { base.clear(); }

    public List<AuditLogEntry> readRecent(int maxLines) {
        return base.readRecent(maxLines).stream()
                .map(e -> new AuditLogEntry(e.timestamp(), e.user(), e.action(), e.detail()))
                .toList();
    }

    public static AuditLogEntry parse(String line) {
        LogEntry e = LogFileBase.parseNdjson(line);
        return e == null ? null : new AuditLogEntry(e.timestamp(), e.user(), e.action(), e.detail());
    }

    static String nowFormatted() { return LogFileBase.nowFormatted(); }
}