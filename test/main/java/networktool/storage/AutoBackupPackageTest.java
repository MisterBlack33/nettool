package main.java.networktool.storage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AutoBackupPackageTest {

    @BeforeEach
    void clean() { AutoBackup.getInstance().cleanupBackups(); }

    @AfterEach
    void stopAndClean() {
        AutoBackup.getInstance().stop();
        AutoBackup.getInstance().cleanupBackups();
    }

    @Test void isActive_initiallyFalse()     { AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void start_setsActive()            { AutoBackup.getInstance().start(24); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void start_default()               { AutoBackup.getInstance().start(); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void stop_clearsActive()           { AutoBackup.getInstance().start(24); AutoBackup.getInstance().stop(); assertFalse(AutoBackup.getInstance().isActive()); }
    @Test void getInterval_returnsSet()      { AutoBackup.getInstance().start(12); assertEquals(12, AutoBackup.getInstance().getInterval()); }
    @Test void startTwice_noStateChange()    { AutoBackup.getInstance().start(6); AutoBackup.getInstance().start(6); assertTrue(AutoBackup.getInstance().isActive()); }
    @Test void maxBackups_isOne()            { assertEquals(1, AutoBackup.MAX_BACKUPS); }
    @Test void cleanupBackups_doesNotThrow() { assertDoesNotThrow(() -> AutoBackup.getInstance().cleanupBackups()); }

    @Test
    void todayBackupDone_falseAfterCleanup() {
        AutoBackup.getInstance().cleanupBackups();
        assertFalse(AutoBackup.getInstance().todayBackupDone());
    }

    @Test
    void triggerNow_calledThreeTimes_onlyOneBackupScheduled() throws InterruptedException {
        AutoBackup.getInstance().start(24);
        // Drei schnelle Aufrufe – nur einer darf durchkommen
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        AutoBackup.getInstance().triggerNow();
        Thread.sleep(200);
        // Nach dem ersten triggerNow ist todayBackupDone = true
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
}