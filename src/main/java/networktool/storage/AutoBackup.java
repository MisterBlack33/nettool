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
 * MAX_BACKUPS=1: ältere Produktiv-Backups werden gelöscht.
 * Sichert nur den savedHostsTags-Unterordner.
 */
public final class AutoBackup {

    private static final class Holder { static final AutoBackup INSTANCE = new AutoBackup(); }
    public static AutoBackup getInstance() { return Holder.INSTANCE; }

    public  static final int    MAX_BACKUPS        = 1;
    public  static final String TEST_BACKUP_PREFIX = "TEST_BACKUP_";
    private static final int    DEFAULT_HOURS      = 6;

    private ScheduledExecutorService scheduler;
    private volatile boolean  active        = false;
    private int               intervalHours = DEFAULT_HOURS;

    private final AtomicBoolean backupScheduled = new AtomicBoolean(false);
    private volatile LocalDate  lastBackupDate  = null;

    static volatile boolean testMode = false;

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
        scheduler.scheduleAtFixedRate(
                this::backup, intervalHours, intervalHours, TimeUnit.HOURS);
        System.out.println("[AutoBackup] Gestartet (alle " + intervalHours + "h)");
    }

    public synchronized void start() { start(DEFAULT_HOURS); }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        System.out.println("[AutoBackup] Gestoppt.");
    }

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

    public void cleanupBackups() {
        lastBackupDate = null;
        backupScheduled.set(false);
        deleteByFilter(p -> p.getFileName().toString().endsWith(".zip"));
    }

    public void cleanupTestBackups() {
        deleteByFilter(p -> {
            String n = p.getFileName().toString();
            return n.startsWith(TEST_BACKUP_PREFIX) && n.endsWith(".zip");
        });
    }

    public boolean isActive()    { return active; }
    public int     getInterval() { return intervalHours; }

    // ── Backup logic ──────────────────────────────────────────────────────

    void backup() {
        if (todayBackupDone()) return;
        lastBackupDate = LocalDate.now();
        try {
            Path dir    = backupDir();
            Path srcDir = NetworkStorePersistence.savedDir(
                    NetworkStorePersistence.resolveTxtDir());
            Files.createDirectories(dir);
            DataExporter.exportBackup(dir, srcDir, buildExportName());
            if (!testMode) pruneOldBackups(dir);
        } catch (IOException e) {
            lastBackupDate = null;
            System.err.println("[AutoBackup] Fehler: " + e.getMessage());
        }
    }

    boolean todayBackupDone() {
        return LocalDate.now().equals(lastBackupDate);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private String buildExportName() {
        String ts = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return testMode
                ? TEST_BACKUP_PREFIX + ts + ".zip"
                : "nettool_backup_" + ts + ".zip";
    }

    private void pruneOldBackups(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".zip") && !n.startsWith(TEST_BACKUP_PREFIX);
                    })
                    .sorted((a, b) -> {
                        try { return Files.getLastModifiedTime(b)
                                .compareTo(Files.getLastModifiedTime(a)); }
                        catch (IOException e) { return 0; }
                    })
                    .skip(MAX_BACKUPS)
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    private void deleteByFilter(java.util.function.Predicate<Path> filter) {
        try {
            Path dir = backupDir();
            if (!Files.isDirectory(dir)) return;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(filter)
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        } catch (IOException ignored) {}
    }

    private Path backupDir() {
        return NetworkStorePersistence.resolveTxtDir().resolve("backups");
    }
}