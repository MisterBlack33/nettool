package main.java.networktool.storage;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Erstellt maximal einmal pro Tag ein ZIP-Backup – aber nur wenn
 * tatsächlich Daten geändert wurden (triggerNow() aufgerufen wurde).
 *
 * Kein automatischer Scheduler. Backup wird asynchron ausgeführt.
 * MAX_BACKUPS=1: ältere Produktiv-Backups werden gelöscht.
 *
 * Test-Backups tragen TEST_BACKUP_PREFIX und werden von
 * cleanupTestBackups() rückstandslos entfernt.
 */
public final class AutoBackup {

    private static final class Holder { static final AutoBackup INSTANCE = new AutoBackup(); }
    public static AutoBackup getInstance() { return Holder.INSTANCE; }

    public  static final int    MAX_BACKUPS        = 1;
    public  static final String TEST_BACKUP_PREFIX = "TEST_BACKUP_";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoBackup");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean backupScheduled = new AtomicBoolean(false);
    private volatile LocalDate  lastBackupDate  = null;
    private volatile boolean    active          = true;

    static volatile boolean testMode = false;

    private AutoBackup() {}

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Löst ein Backup aus – maximal einmal pro Tag.
     * Wird von NetworkStore nach jeder Datenänderung aufgerufen.
     */
    public void triggerNow() {
        if (!active || todayBackupDone()) return;
        if (!backupScheduled.compareAndSet(false, true)) return;
        executor.submit(() -> {
            try { backup(); }
            finally { backupScheduled.set(false); }
        });
    }

    public void stop() {
        active = false;
        executor.shutdownNow();
    }

    /** Löscht alle Backups und setzt Datum zurück. */
    public void cleanupBackups() {
        lastBackupDate = null;
        backupScheduled.set(false);
        deleteByFilter(p -> p.getFileName().toString().endsWith(".zip"));
    }

    /** Löscht ausschließlich Test-Backups (TEST_BACKUP_PREFIX). */
    public void cleanupTestBackups() {
        deleteByFilter(p -> {
            String name = p.getFileName().toString();
            return name.startsWith(TEST_BACKUP_PREFIX) && name.endsWith(".zip");
        });
    }

    public boolean isActive()       { return active; }
    public boolean todayBackupDone(){ return LocalDate.now().equals(lastBackupDate); }

    // ── Backup ────────────────────────────────────────────────────────────

    void backup() {
        if (todayBackupDone()) return;
        lastBackupDate = LocalDate.now();
        try {
            Path dir = backupDir();
            Files.createDirectories(dir);
            DataExporter.exportBackup(dir, buildFilename());
            if (!testMode) pruneOldBackups(dir);
        } catch (IOException e) {
            lastBackupDate = null;
            System.err.println("[AutoBackup] Fehler: " + e.getMessage());
        }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private String buildFilename() {
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
                        try {
                            return Files.getLastModifiedTime(b)
                                    .compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) { return 0; }
                    })
                    .skip(MAX_BACKUPS)
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    private void deleteByFilter(java.util.function.Predicate<Path> filter) {
        try {
            Path dir = backupDir();
            if (!Files.isDirectory(dir)) return;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(filter)
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}
    }

    private Path backupDir() {
        return NetworkStorePersistence.resolveTxtDir().resolve("backups");
    }
}