package main.java.networktool.storage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AutoBackupPackageTest {

    @TempDir Path tmpBackup;
    @TempDir Path tmpSrc;

    @BeforeEach
    void setup() {
        AutoBackup.testMode = true;
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().setDirs(tmpBackup, tmpSrc);
        AutoBackup.getInstance().cleanupBackups();
    }

    @AfterEach
    void teardown() {
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().cleanupTestBackups();
        AutoBackup.getInstance().resetDirs();
        AutoBackup.testMode = false;
    }

    // ── Basis-State ───────────────────────────────────────────────────────

    @Test void isActive_initiallyFalse()  { assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void start_setsActive()         { AutoBackup.getInstance().start(24); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void start_default()            { AutoBackup.getInstance().start(); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void stop_clearsActive()        { AutoBackup.getInstance().start(24); AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void getInterval_returnsSet()   { AutoBackup.getInstance().start(12); assertEquals(12, AutoBackup.getInstance().getInterval()); }
    @Test void maxBackups_isOne()         { assertEquals(1, AutoBackup.MAX_BACKUPS); }
    @Test void testBackupPrefix_notEmpty(){ assertFalse(AutoBackup.TEST_BACKUP_PREFIX.isBlank()); }

    @Test
    void todayBackupDone_falseAfterCleanup() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    // ── backup() mit @TempDir — kein echter FS-Zugriff ───────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_createsFile() {
        AutoBackup.getInstance().backup();

        List<Path> zips = listTestZips();
        assertFalse(zips.isEmpty(), "Kein ZIP erstellt");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_testMode_usesTestPrefix() {
        AutoBackup.getInstance().backup();

        List<Path> zips = listTestZips();
        assertFalse(zips.isEmpty());
        zips.forEach(p ->
                assertTrue(p.getFileName().toString()
                                .startsWith(AutoBackup.TEST_BACKUP_PREFIX),
                        "Kein TEST_BACKUP_PREFIX: " + p.getFileName()));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_onlyOncePerDay() {
        AutoBackup.getInstance().backup();
        AutoBackup.getInstance().backup(); // zweiter Aufruf wird ignoriert

        assertEquals(1, listTestZips().size(),
                "Backup darf nur einmal pro Tag erstellt werden");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void backup_afterCleanup_createsAgain() {
        AutoBackup.getInstance().backup();
        AutoBackup.getInstance().cleanupBackups(); // Datum zurücksetzen

        AutoBackup.getInstance().backup();
        assertFalse(listTestZips().isEmpty());
    }

    // ── triggerNow — kein Hängen ──────────────────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_doesNotHang() {
        AutoBackup.getInstance().start(24);
        assertDoesNotThrow(() -> AutoBackup.getInstance().triggerNow());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_idempotent() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(300);
        assertTrue(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void triggerNow_afterCleanup_resetsFlag() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(200);
        assertTrue(AutoBackup.getInstance().todayBackupDone());

        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void cleanupTestBackups_removesOnlyTestFiles() throws IOException {
        // echte + Test-Datei anlegen
        Path real = tmpBackup.resolve("nettool_backup_2024-01-01.zip");
        Path test = tmpBackup.resolve(AutoBackup.TEST_BACKUP_PREFIX + "2024-01-01.zip");
        Files.createFile(real);
        Files.createFile(test);

        AutoBackup.getInstance().cleanupTestBackups();

        assertTrue(Files.exists(real),  "Produktiv-Backup fälschlicherweise gelöscht");
        assertFalse(Files.exists(test), "Test-Backup nicht gelöscht");
    }

    @Test
    void cleanupTestBackups_noThrow() {
        assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupTestBackups());
    }

    // ── PersistenceEdge (package-private Helpers) ─────────────────────────

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

        @Test void esc_backslash() {
            assertTrue(NetworkStorePersistence.esc("a\\b").contains("\\\\"));
        }

        @Test void extractStr_missing_null() {
            assertNull(NetworkStorePersistence.extractStr("{}", "nope"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<Path> listTestZips() {
        try (Stream<Path> files = Files.list(tmpBackup)) {
            return files.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(AutoBackup.TEST_BACKUP_PREFIX) && name.endsWith(".zip");
            }).collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }
}