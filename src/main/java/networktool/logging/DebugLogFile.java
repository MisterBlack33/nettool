package main.java.networktool.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Datei-I/O für technische Debug-Logs (nicht sicherheitsrelevant, siehe AuditLogFile dafür).
 * Format: eine Zeile pro Eintrag: "ts\tlevel\tmessage".
 * Rotation: ab MAX_LINES wird die Datei archiviert, wie bei AuditLogFile.
 */
public final class DebugLogFile {

    static final int MAX_LINES = 50_000;
    public static final String FILE_NAME = "debug.log";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ROT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path logFile;

    public DebugLogFile(Path dataDir) {
        this.logFile = dataDir.resolve(FILE_NAME);
    }

    public synchronized void append(DebugLogEntry entry) {
        try {
            if (!Files.isDirectory(logFile.getParent())) return;
            if (Files.exists(logFile) && countLines() >= MAX_LINES) rotate();
            Files.writeString(logFile, entry.toLine() + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Debug-Logging darf nie die Anwendung stören.
        }
    }

    public synchronized void clear() {
        try { Files.deleteIfExists(logFile); } catch (IOException ignored) {}
    }

    public List<DebugLogEntry> readRecent(int maxLines) {
        if (!Files.exists(logFile)) return Collections.emptyList();
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<DebugLogEntry> result = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && result.size() < maxLines; i--) {
                DebugLogEntry e = parse(lines.get(i));
                if (e != null) result.add(e);
            }
            return Collections.unmodifiableList(result);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    static DebugLogEntry parse(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\t", 3);
        if (p.length < 3) return null;
        return new DebugLogEntry(p[0], p[1], p[2]);
    }

    private void rotate() throws IOException {
        Path rotated = logFile.resolveSibling("debug_" + LocalDateTime.now().format(ROT_FMT) + ".log");
        Files.move(logFile, rotated, StandardCopyOption.REPLACE_EXISTING);
    }

    private long countLines() throws IOException {
        try (var r = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            return r.lines().count();
        }
    }

    static String nowFormatted() { return LocalDateTime.now().format(FMT); }
}
