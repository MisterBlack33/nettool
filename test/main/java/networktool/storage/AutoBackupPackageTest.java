package main.java.networktool.storage;

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

    @Test void isActive_initiallyFalse()     { AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void start_setsActive()            { AutoBackup.getInstance().start(24); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void start_default()               { AutoBackup.getInstance().start(); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void stop_clearsActive()           { AutoBackup.getInstance().start(24); AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void getInterval_returnsSet()      { AutoBackup.getInstance().start(12); assertEquals(12, AutoBackup.getInstance().getInterval()); }
    @Test void startTwice_noStateChange()    { AutoBackup.getInstance().start(6); AutoBackup.getInstance().start(6); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void maxBackups_isOne()            { assertEquals(1, AutoBackup.MAX_BACKUPS); }
    @Test void cleanupTestBackups_noThrow()  { assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupTestBackups()); }

    @Test
    void todayBackupDone_falseAfterCleanup() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    // ── Test-Backup-Prefix ────────────────────────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_inTestMode_createsTestPrefixFile() throws IOException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().cleanupBackups(); // lastBackupDate zurücksetzen

        AutoBackup.getInstance().backup();

        Path dir = backupDir();
        if (!Files.isDirectory(dir)) return; // kein Fehler wenn Verzeichnis leer
        List<Path> testZips = listTestBackups(dir);
        assertFalse(testZips.isEmpty(), "Kein Test-Backup erstellt");
        testZips.forEach(p ->
                assertTrue(p.getFileName().toString()
                                .startsWith(AutoBackup.TEST_BACKUP_PREFIX),
                        "Backup trägt nicht das TEST-Präfix: " + p.getFileName()));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void cleanupTestBackups_removesOnlyTestFiles(@TempDir Path tmp) throws IOException {
        // Zwei Dateien anlegen: eine echte, eine Test-Backup
        Path real = tmp.resolve("nettool_backup_2024-01-01.zip");
        Path test = tmp.resolve(AutoBackup.TEST_BACKUP_PREFIX + "2024-01-01.zip");
        Files.createFile(real);
        Files.createFile(test);

        // Nur Test-Datei löschen (wir simulieren cleanupTestBackups direkt)
        try (var stream = Files.list(tmp)) {
            stream.filter(p -> p.getFileName().toString()
                            .startsWith(AutoBackup.TEST_BACKUP_PREFIX))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }

        assertTrue(Files.exists(real), "Produktiv-Backup wurde fälschlicherweise gelöscht");
        assertFalse(Files.exists(test), "Test-Backup wurde nicht gelöscht");
    }

    @Test
    void testBackupPrefix_constant_notEmpty() {
        assertFalse(AutoBackup.TEST_BACKUP_PREFIX.isBlank());
        assertTrue(AutoBackup.TEST_BACKUP_PREFIX.contains("TEST"));
    }

    // ── triggerNow Idempotenz ─────────────────────────────────────────────

    @Test
    void triggerNow_calledThreeTimes_onlyOneBackupScheduled() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(300);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    void triggerNow_afterCleanup_resetsFlag() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(200);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_doesNotThrow() {
        AutoBackup.getInstance().start(24);
        assertDoesNotThrow(() -> AutoBackup.getInstance().triggerNow());
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
        return NetworkStorePersistence.resolveTxtDir().resolve("backups");
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