package networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageExtTest {

    // ══════════════════════════════════════════════════════════════
    //  SavedHostsStore
    // ══════════════════════════════════════════════════════════════

    @Nested
    class SavedHostsStoreTest {

        @TempDir Path tmp;
        SavedHostsStore store;

        @BeforeEach
        void setup() {
            store = SavedHostsStore.getInstance();
            store.setFilePath(tmp.resolve("saved_hosts.txt"));
        }

        @Test
        void save_and_getAll() {
            store.save(new HostResult("10.0.0.1", "host1", "Linux", null));
            List<HostResult> all = store.getAll();
            assertTrue(all.stream().anyMatch(h -> "10.0.0.1".equals(h.ip)));
        }

        @Test
        void save_duplicate_updatesPortsOnly() {
            Map<Integer, String> ports = new HashMap<>();
            ports.put(22, "SSH");
            store.save(new HostResult("10.0.0.2", "host2", "Linux", null, ports));
            store.save(new HostResult("10.0.0.2", "host2", "Linux", null, Map.of(80, "HTTP")));
            List<HostResult> all = store.getAll();
            long count = all.stream().filter(h -> "10.0.0.2".equals(h.ip)).count();
            assertEquals(1, count);
        }

        @Test
        void remove_existing() {
            store.save(new HostResult("10.0.0.3", "h3", "Win", null));
            store.remove("10.0.0.3");
            assertFalse(store.getAll().stream().anyMatch(h -> "10.0.0.3".equals(h.ip)));
        }

        @Test
        void remove_nonExistent_doesNotThrow() {
            assertDoesNotThrow(() -> store.remove("99.99.99.99"));
        }

        @Test
        void updateNotes_persists() {
            store.save(new HostResult("10.0.0.4", "h4", "Linux", null));
            store.updateNotes("10.0.0.4", "my note");
            assertTrue(store.getAll().stream()
                    .filter(h -> "10.0.0.4".equals(h.ip))
                    .anyMatch(h -> "my note".equals(h.notes)));
        }

        @Test
        void addChangeListener_notifiedOnSave() {
            int[] count = {0};
            store.addChangeListener(() -> count[0]++);
            store.save(new HostResult("10.0.0.5", "h5", "Win", null));
            assertTrue(count[0] >= 1);
        }

        @Test
        void getAll_immutable() {
            assertDoesNotThrow(() -> {
                List<HostResult> list = store.getAll();
                assertNotNull(list);
            });
        }

        @Test
        void save_null_doesNotThrow() {
            assertDoesNotThrow(() -> store.save(null));
        }

        @Test
        void save_blankIp_doesNotThrow() {
            assertDoesNotThrow(() -> store.save(new HostResult("", "h", "Linux", null)));
        }

        @Test
        void persistence_survivesReinit() {
            store.save(new HostResult("10.0.0.9", "persist", "Linux", null));
            store.setFilePath(tmp.resolve("saved_hosts.txt"));
            assertTrue(store.getAll().stream().anyMatch(h -> "10.0.0.9".equals(h.ip)));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AutoBackup
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AutoBackupTest {

        @AfterEach
        void stop() {
            AutoBackup.getInstance().stop();
        }

        @Test
        void isActive_initiallyFalse() {
            AutoBackup.getInstance().stop();
            assertFalse(AutoBackup.getInstance().isActive());
        }

        @Test
        void start_setsActive() {
            AutoBackup.getInstance().start(24);
            assertTrue(AutoBackup.getInstance().isActive());
        }

        @Test
        void start_defaultHours() {
            AutoBackup.getInstance().start();
            assertTrue(AutoBackup.getInstance().isActive());
        }

        @Test
        void stop_clearsActive() {
            AutoBackup.getInstance().start(24);
            AutoBackup.getInstance().stop();
            assertFalse(AutoBackup.getInstance().isActive());
        }

        @Test
        void getInterval_returnsSet() {
            AutoBackup.getInstance().start(12);
            assertEquals(12, AutoBackup.getInstance().getInterval());
        }

        @Test
        void startTwice_doesNotChangeState() {
            AutoBackup.getInstance().start(6);
            AutoBackup.getInstance().start(6);
            assertTrue(AutoBackup.getInstance().isActive());
        }

        @Test
        void triggerNow_doesNotThrow() {
            AutoBackup.getInstance().start(24);
            assertDoesNotThrow(() -> AutoBackup.getInstance().triggerNow());
        }

        @Test
        void backup_doesNotThrow() {
            assertDoesNotThrow(() -> AutoBackup.getInstance().backup());
        }

        @Test
        void maxBackups_constant() {
            assertEquals(10, AutoBackup.MAX_BACKUPS);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkStorePersistence – parsePorts edge cases
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PersistenceEdgeTest {

        @Test
        void parsePorts_malformed_ignored() {
            var m = main.java.networktool_v3.storage.NetworkStorePersistence
                    .parsePorts("abc:def,22:SSH");
            assertFalse(m.containsKey(0));
            assertTrue(m.containsKey(22));
        }

        @Test
        void parsePorts_noValue_defaultsToOffen() {
            var m = main.java.networktool_v3.storage.NetworkStorePersistence
                    .parsePorts("443");
            assertEquals("offen", m.get(443));
        }

        @Test
        void esc_backslash() {
            String r = main.java.networktool_v3.storage.NetworkStorePersistence
                    .esc("a\\b");
            assertTrue(r.contains("\\\\"));
        }

        @Test
        void extractStr_missing_null() {
            assertNull(main.java.networktool_v3.storage.NetworkStorePersistence
                    .extractStr("{}", "nope"));
        }
    }
}