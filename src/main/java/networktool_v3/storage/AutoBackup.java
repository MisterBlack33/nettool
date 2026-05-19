package main.java.networktool_v3.storage;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Erstellt automatisch ZIP-Backups alle N Stunden in txt/backups/.
 * Hält maximal MAX_BACKUPS Dateien vor (älteste werden gelöscht).
 */
public final class AutoBackup {

    private static final class Holder { static final AutoBackup INSTANCE = new AutoBackup(); }
    public static AutoBackup getInstance() { return Holder.INSTANCE; }

    public static final int  MAX_BACKUPS   = 10;
    private static final int DEFAULT_HOURS = 6;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ScheduledExecutorService scheduler;
    private volatile boolean active = false;
    private int intervalHours = DEFAULT_HOURS;

    private AutoBackup() {}

    // ── Public API ────────────────────────────────────────────────────────

    public synchronized void start(int hours) {
        if (active) return;
        intervalHours = hours > 0 ? hours : DEFAULT_HOURS;
        active = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoBackup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::backup, intervalHours, intervalHours, TimeUnit.HOURS);
        System.out.println("[AutoBackup] Gestartet (alle " + intervalHours + "h)");
    }

    public synchronized void start() { start(DEFAULT_HOURS); }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        System.out.println("[AutoBackup] Gestoppt.");
    }

    /** Sofortiges Backup (z.B. nach save/remove). */
    public void triggerNow() {
        if (scheduler != null && !scheduler.isShutdown())
            scheduler.submit(this::backup);
        else
            new Thread(this::backup, "AutoBackup-OnDemand").start();
    }

    public boolean isActive()      { return active; }
    public int     getInterval()   { return intervalHours; }

    // ── Backup logic ──────────────────────────────────────────────────────

    void backup() {
        try {
            Path backupDir = NetworkStorePersistence.resolveTxtDir().resolve("backups");
            Files.createDirectories(backupDir);
            Path zipFile = backupDir.resolve("backup_" + LocalDateTime.now().format(FMT) + ".zip");
            DataExporter.exportBackup(zipFile.getParent());
            pruneOldBackups(backupDir);
        } catch (IOException e) {
            System.err.println("[AutoBackup] Fehler: " + e.getMessage());
        }
    }

    private void pruneOldBackups(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted((a, b) -> {
                        try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                        catch (IOException e) { return 0; }
                    })
                    .skip(MAX_BACKUPS)
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }
}