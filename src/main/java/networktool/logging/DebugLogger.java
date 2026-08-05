package main.java.networktool.logging;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * Öffentliche API für technische Debug-Logs. Getrennt von AuditLogger (Sicherheits-Log).
 * Persistiert dateibasiert, überlebt daher Neustarts/Instanzen.
 */
public final class DebugLogger {

    private static final class Holder { static final DebugLogger INSTANCE = new DebugLogger(); }
    public static DebugLogger getInstance() { return Holder.INSTANCE; }

    private volatile DebugLogFile logFile;
    private volatile ExecutorService writer = newWriter();

    private DebugLogger() {}

    public void init(Path dataDir) {
        flushAndShutdown();
        this.logFile = new DebugLogFile(dataDir);
        this.writer  = newWriter();
    }

    public void debug(String msg) { log("DEBUG", msg); }
    public void info(String msg)  { log("INFO", msg); }
    public void warn(String msg)  { log("WARN", msg); }
    public void error(String msg) { log("ERROR", msg); }

    public void log(String level, String msg) {
        if (logFile == null) return;
        DebugLogEntry entry = new DebugLogEntry(DebugLogFile.nowFormatted(), level, msg);
        writer.submit(() -> logFile.append(entry));
    }

    public List<DebugLogEntry> readRecent(int maxLines) {
        flush();
        if (logFile == null) return List.of();
        return logFile.readRecent(maxLines);
    }

    public void clear() {
        flush();
        if (logFile != null) logFile.clear();
    }

    public void flush() {
        ExecutorService w = writer;
        if (w == null || w.isShutdown()) return;
        try { w.submit(() -> {}).get(3, TimeUnit.SECONDS); } catch (Exception ignored) {}
    }

    public void shutdown() {
        flush();
        flushAndShutdown();
        writer = newWriter();
    }

    private void flushAndShutdown() {
        ExecutorService w = writer;
        if (w == null) return;
        try { w.shutdown(); w.awaitTermination(3, TimeUnit.SECONDS); } catch (Exception ignored) {}
    }

    private static ExecutorService newWriter() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DebugLogger");
            t.setDaemon(true);
            return t;
        });
    }
}
