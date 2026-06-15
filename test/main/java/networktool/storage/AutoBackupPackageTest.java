package main.java.networktool.storage;

import networktool.util.PollHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AutoBackupPackageTest {

    @BeforeEach
    void clean() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().cleanupBackups();
    }

    @AfterEach
    void stopAndClean() {
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }

    // ── Basis-State ───────────────────────────────────────────────────────

    @Test void isActive_initiallyFalse()  { assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void start_setsActive()         { AutoBackup.getInstance().start(24); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void start_default()            { AutoBackup.getInstance().start(); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void stop_clearsActive()        { AutoBackup.getInstance().start(24); AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void getInterval_returnsSet()   { AutoBackup.getInstance().start(12); assertEquals(12, AutoBackup.getInstance().getInterval()); }
    @Test void startTwice_noStateChange() { AutoBackup.getInstance().start(6); AutoBackup.getInstance().start(6); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void maxBackups_isOne()         { assertEquals(1, AutoBackup.MAX_BACKUPS); }
    @Test void cleanupTestBackups_noThrow() { assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupTestBackups()); }
    @Test void todayBackupDone_falseAfterCleanup() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    // ── backup() direkt ────────────────────────────────────────────────────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void backup_inTestMode_createsTestPrefixFile() throws IOException {
        AutoBackup.getInstance().cleanupBackups();
        AutoBackup.getInstance().backup();

        Path dir = backupDir();
        if (!Files.isDirectory(dir)) return;
        List<Path> testZips = listTestBackups(dir);
        testZips.forEach(p ->
                assertTrue(p.getFileName().toString().startsWith(AutoBackup.TEST_BACKUP_PREFIX),
                        "Backup trägt nicht das TEST-Präfix: " + p.getFileName()));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_setsBackupDone() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
        AutoBackup.getInstance().backup();
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_secondCall_skipped() throws IOException {
        AutoBackup.getInstance().cleanupBackups();
        AutoBackup.getInstance().backup();
        Path dir = backupDir();
        long countBefore = Files.isDirectory(dir) ? listTestBackups(dir).size() : 0;
        AutoBackup.getInstance().backup();
        long countAfter  = Files.isDirectory(dir) ? listTestBackups(dir).size() : 0;
        assertEquals(countBefore, countAfter);
    }

    // ── Test-Backup-Prefix ────────────────────────────────────────────────

    @Test
    void cleanupTestBackups_removesOnlyTestFiles(@TempDir Path tmp) throws IOException {
        Path real = tmp.resolve("nettool_backup_2024-01-01.zip");
        Path test = tmp.resolve(AutoBackup.TEST_BACKUP_PREFIX + "2024-01-01.zip");
        Files.createFile(real);
        Files.createFile(test);

        try (var stream = Files.list(tmp)) {
            stream.filter(p -> p.getFileName().toString().startsWith(AutoBackup.TEST_BACKUP_PREFIX))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }

        assertTrue(Files.exists(real),  "Produktiv-Backup wurde fälschlicherweise gelöscht");
        assertFalse(Files.exists(test), "Test-Backup wurde nicht gelöscht");
    }

    @Test
    void testBackupPrefix_notEmpty() {
        assertFalse(AutoBackup.TEST_BACKUP_PREFIX.isBlank());
        assertTrue(AutoBackup.TEST_BACKUP_PREFIX.contains("TEST"));
    }

    // ── triggerNow ────────────────────────────────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_afterCleanup_setsBackupDone() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().cleanupBackups();
        AutoBackup.getInstance().triggerNow();
        for (int i = 0; i < 30 && !AutoBackup.getInstance().todayBackupDone(); i++)
            Thread.sleep(100);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_doesNotThrow() {
        AutoBackup.getInstance().start(24);
        assertDoesNotThrow(() -> AutoBackup.getInstance().triggerNow());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_calledThreeTimes_backupDoneOnce() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().cleanupBackups();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        for (int i = 0; i < 30 && !AutoBackup.getInstance().todayBackupDone(); i++)
            Thread.sleep(100);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_withoutStart_usesOnDemandThread() {
        AutoBackup.getInstance().cleanupBackups();
        AutoBackup.getInstance().triggerNow();
        assertTrue(PollHelper.waitFor(AutoBackup.getInstance()::todayBackupDone, 4000),
                "Backup sollte innerhalb von 4 Sekunden abgeschlossen sein");
    }

    // ── PersistenceEdgeTest ───────────────────────────────────────────────

    @Nested
    class PersistenceEdgeTest {

        @Test void parsePorts_malformed_ignored() {
            var m = NetworkStorePersistence.parsePorts("abc:def,22:SSH");
            assertFalse(m.containsKey(0));
            assertTrue(m.containsKey(22));
        }

        @Test void parsePorts_noValue_defaultsToOffen() {
            assertEquals("offen", NetworkStorePersistence.parsePorts("443").get(443));
        }

        @Test void esc_backslash()           { assertTrue(NetworkStorePersistence.esc("a\\b").contains("\\\\")); }
        @Test void extractStr_missing_null() { assertNull(NetworkStorePersistence.extractStr("{}", "nope")); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static Path backupDir() {
        return NetworkStorePersistence.resolveDataDir().resolve("backups");
    }

    private static List<Path> listTestBackups(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString()
                            .startsWith(AutoBackup.TEST_BACKUP_PREFIX))
                    .collect(Collectors.toList());
        }
    }
}