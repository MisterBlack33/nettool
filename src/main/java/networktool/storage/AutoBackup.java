package main.java.networktool.storage;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Erstellt automatisch ZIP-Backups — maximal einmal pro Tag.
 * MAX_BACKUPS=1: ältere Produktiv-Backups werden gelöscht.
 *
 * Test-Backups tragen TEST_BACKUP_PREFIX und werden von
 * cleanupTestBackups() rückstandslos entfernt.
 *
 * Für Tests: setDirs(backupDir, srcDir) injiziert @TempDir-Pfade,
 * sodass kein echter Dateisystem-Zugriff stattfindet.
 */
public final class AutoBackup {

    private static final class Holder { static final AutoBackup INSTANCE = new AutoBackup(); }
    public static AutoBackup getInstance() { return Holder.INSTANCE; }

    public  static final int    MAX_BACKUPS        = 1;
    public  static final String TEST_BACKUP_PREFIX = "TEST_BACKUP_";
    private static final int    DEFAULT_HOURS      = 6;
    private static final DateTimeFormatter TS_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ScheduledExecutorService scheduler;
    private volatile boolean  active        = false;
    private int               intervalHours = DEFAULT_HOURS;

    private final AtomicBoolean backupScheduled = new AtomicBoolean(false);
    private volatile LocalDate  lastBackupDate  = null;

    /** Gesetzt von Tests: Backups erhalten TEST_BACKUP_PREFIX. */
    static volatile boolean testMode = false;

    /** Injizierbare Verzeichnis-Supplier für Tests (@TempDir). */
    private volatile Supplier<Path> backupDirSupplier = this::defaultBackupDir;
    private volatile Supplier<Path> srcDirSupplier    = NetworkStorePersistence::resolveTxtDir;

    private AutoBackup() {}

    // ── Test-API ──────────────────────────────────────────────────────────

    /**
     * Injiziert Backup- und Quell-Verzeichnis.
     * Muss vor start() aufgerufen werden.
     */
    public void setDirs(Path backupDir, Path srcDir) {
        this.backupDirSupplier = () -> backupDir;
        this.srcDirSupplier    = () -> srcDir;
    }

    /** Setzt Verzeichnisse auf Produktiv-Defaults zurück. */
    public void resetDirs() {
        this.backupDirSupplier = this::defaultBackupDir;
        this.srcDirSupplier    = NetworkStorePersistence::resolveTxtDir;
    }

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

    /**
     * Sofortiges Backup — nur einmal pro Tag.
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

    public boolean isActive()    { return active; }
    public int     getInterval() { return intervalHours; }

    // ── Backup-Logik ──────────────────────────────────────────────────────

    void backup() {
        if (todayBackupDone()) return;
        lastBackupDate = LocalDate.now();
        try {
            Path bDir = backupDirSupplier.get();
            Path sDir = srcDirSupplier.get();
            Files.createDirectories(bDir);
            DataExporter.exportBackup(bDir, sDir, buildFilename());
            if (!testMode) pruneOldBackups(bDir);
        } catch (IOException e) {
            lastBackupDate = null;
            System.err.println("[AutoBackup] Fehler: " + e.getMessage());
        }
    }

    boolean todayBackupDone() {
        return LocalDate.now().equals(lastBackupDate);
    }

    // ── private ───────────────────────────────────────────────────────────

    private String buildFilename() {
        String ts = LocalDateTime.now().format(TS_FMT);
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
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    private void deleteByFilter(java.util.function.Predicate<Path> filter) {
        try {
            Path dir = backupDirSupplier.get();
            if (!Files.isDirectory(dir)) return;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(filter)
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}
    }

    private Path defaultBackupDir() {
        return NetworkStorePersistence.resolveTxtDir().resolve("backups");
    }
}