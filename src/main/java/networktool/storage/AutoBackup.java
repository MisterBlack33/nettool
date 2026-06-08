package main.java.networktool.storage;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Erstellt automatisch ZIP-Backups – maximal einmal pro Tag.
 * MAX_BACKUPS=1: ältere Backups werden gelöscht.
 *
 * triggerNow() ist thread-sicher idempotent via AtomicBoolean.
 * cleanupBackups() löscht alle Backups (für Test-Teardown).
 */
public final class AutoBackup {

    private static final class Holder { static final AutoBackup INSTANCE = new AutoBackup(); }
    public static AutoBackup getInstance() { return Holder.INSTANCE; }

    public  static final int  MAX_BACKUPS  = 1;
    private static final int  DEFAULT_HOURS = 6;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ScheduledExecutorService scheduler;
    private volatile boolean  active          = false;
    private int               intervalHours   = DEFAULT_HOURS;

    /** Verhindert Race-Condition bei parallelen triggerNow()-Aufrufen. */
    private final AtomicBoolean backupScheduled = new AtomicBoolean(false);
    private volatile LocalDate  lastBackupDate  = null;

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

    /**
     * Sofortiges Backup – nur einmal pro Tag.
     * AtomicBoolean verhindert mehrfaches Einreihen bei parallelen Aufrufen.
     */
    public void triggerNow() {
        if (todayBackupDone()) return;
        if (!backupScheduled.compareAndSet(false, true)) return;
        Runnable task = () -> {
            try { backup(); }
            finally { backupScheduled.set(false); }
        };
        if (scheduler != null && !scheduler.isShutdown())
            scheduler.submit(task);
        else
            new Thread(task, "AutoBackup-OnDemand").start();
    }

    /** Löscht alle Backups im backups/-Verzeichnis und setzt lastBackupDate zurück. */
    public void cleanupBackups() {
        lastBackupDate = null;
        backupScheduled.set(false);
        try {
            Path dir = backupDir();
            if (!Files.isDirectory(dir)) return;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.getFileName().toString().endsWith(".zip"))
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        } catch (IOException ignored) {}
    }

    public boolean isActive()    { return active; }
    public int     getInterval() { return intervalHours; }

    // ── Backup logic ──────────────────────────────────────────────────────

    void backup() {
        if (todayBackupDone()) return;
        LocalDate today = LocalDate.now();
        lastBackupDate = today;          // Flag sofort setzen → kein zweites Backup
        try {
            Path dir = backupDir();
            Files.createDirectories(dir);
            DataExporter.exportBackup(dir);
            pruneOldBackups(dir);
        } catch (IOException e) {
            lastBackupDate = null;       // Bei Fehler zurücksetzen
            System.err.println("[AutoBackup] Fehler: " + e.getMessage());
        }
    }

    boolean todayBackupDone() {
        return LocalDate.now().equals(lastBackupDate);
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

    private Path backupDir() {
        return NetworkStorePersistence.resolveTxtDir().resolve("backups");
    }
}