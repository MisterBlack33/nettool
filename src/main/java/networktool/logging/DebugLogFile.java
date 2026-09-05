package main.java.networktool.logging;

import java.nio.file.Path;
import java.util.List;

/**
 * Datei-I/O für technische Debug-Logs (nicht sicherheitsrelevant, siehe AuditLogFile dafür).
 * Delegiert an {@link LogFileBase}; das alte Tab-Format ("ts\tlevel\tmessage") bleibt lesbar.
 */
public final class DebugLogFile {

    static final int MAX_LINES = 50_000;
    public static final String FILE_NAME = "debug.log";

    private final LogFileBase base;

    public DebugLogFile(Path dataDir) {
        this.base = new LogFileBase(dataDir, FILE_NAME, MAX_LINES, DebugLogFile::parseLegacyLine);
    }

    public void append(DebugLogEntry entry) {
        base.append(new LogEntry(entry.timestamp(), "DEBUG", "", entry.level(), "", entry.message()));
    }

    public void clear() { base.clear(); }

    public List<DebugLogEntry> readRecent(int maxLines) {
        return base.readRecent(maxLines).stream()
                .map(e -> new DebugLogEntry(e.timestamp(), e.level(), e.detail()))
                .toList();
    }

    static DebugLogEntry parse(String line) {
        LogEntry e = parseLegacyLine(line);
        if (e == null) e = LogFileBase.parseNdjson(line);
        return e == null ? null : new DebugLogEntry(e.timestamp(), e.level(), e.detail());
    }

    private static LogEntry parseLegacyLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\t", 3);
        if (p.length < 3) return null;
        return new LogEntry(p[0], "DEBUG", "", p[1], "", p[2]);
    }

    static String nowFormatted() { return LogFileBase.nowFormatted(); }
}