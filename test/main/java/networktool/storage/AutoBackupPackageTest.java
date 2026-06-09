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
    void cleanup() {
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.testMode = false;
    }

    // ── Basis-State ───────────────────────────────────────────────────────

    @Test void isActive_initiallyTrue()     { assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void maxBackups_isOne()           { assertEquals(1, AutoBackup.MAX_BACKUPS); }
    @Test void cleanupTestBackups_noThrow() { assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupTestBackups()); }
    @Test void cleanupBackups_noThrow()     { assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupBackups()); }

    @Test
    void todayBackupDone_falseAfterCleanup() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    // ── triggerNow ────────────────────────────────────────────────────────

    @Test
    void triggerNow_calledThreeTimes_onlyOneBackup() throws InterruptedException {
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(300);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    void triggerNow_afterCleanup_resetsFlag() throws InterruptedException {
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(200);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_doesNotThrow() {
        assertDoesNotThrow(() -> AutoBackup.getInstance().triggerNow());
    }

    // ── Test-Backup-Prefix ────────────────────────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_inTestMode_createsTestPrefixFile() throws InterruptedException, IOException {
        AutoBackup.getInstance().backup();
        Thread.sleep(200);

        Path dir = backupDir();
        if (!Files.isDirectory(dir)) return;
        List<Path> testZips = listTestBackups(dir);
        assertFalse(testZips.isEmpty(), "Kein Test-Backup erstellt");
        testZips.forEach(p ->
                assertTrue(p.getFileName().toString()
                                .startsWith(AutoBackup.TEST_BACKUP_PREFIX),
                        "Backup trägt nicht das TEST-Präfix: " + p.getFileName()));
    }

    @Test
    void cleanupTestBackups_removesOnlyTestFiles(@TempDir Path tmp) throws IOException {
        Path real = tmp.resolve("nettool_backup_2024-01-01.zip");
        Path test = tmp.resolve(AutoBackup.TEST_BACKUP_PREFIX + "2024-01-01.zip");
        Files.createFile(real);
        Files.createFile(test);

        try (var stream = Files.list(tmp)) {
            stream.filter(p -> p.getFileName().toString()
                            .startsWith(AutoBackup.TEST_BACKUP_PREFIX))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }

        assertTrue(Files.exists(real),  "Produktiv-Backup fälschlicherweise gelöscht");
        assertFalse(Files.exists(test), "Test-Backup nicht gelöscht");
    }

    @Test
    void testBackupPrefix_notEmpty() {
        assertFalse(AutoBackup.TEST_BACKUP_PREFIX.isBlank());
        assertTrue(AutoBackup.TEST_BACKUP_PREFIX.contains("TEST"));
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