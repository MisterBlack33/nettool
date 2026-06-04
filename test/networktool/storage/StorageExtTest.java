package networktool.storage;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.SavedHostsStore;
import main.java.networktool.storage.TestConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageExtTest {

    @Nested
    class SavedHostsStoreTest {

        @TempDir Path tmp;
        SavedHostsStore store;

        @BeforeEach void setup() {
            store = SavedHostsStore.getInstance();
            store.setFilePath(tmp.resolve("saved_hosts.txt"));
        }

        @Test void save_and_getAll() {
            store.save(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux", null));
            assertTrue(store.getAll().stream().anyMatch(h -> TestConstants.IP_1.equals(h.ip)));
        }

        @Test void save_duplicate_updatesPortsOnly() {
            store.save(new HostResult(TestConstants.IP_2, TestConstants.HOST_2, "Linux", null, Map.of(22, "SSH")));
            store.save(new HostResult(TestConstants.IP_2, TestConstants.HOST_2, "Linux", null, Map.of(80, "HTTP")));
            assertEquals(1, store.getAll().stream().filter(h -> TestConstants.IP_2.equals(h.ip)).count());
        }

        @Test void remove_existing() {
            store.save(new HostResult(TestConstants.IP_3, TestConstants.HOST_3, "Win", null));
            store.remove(TestConstants.IP_3);
            assertFalse(store.getAll().stream().anyMatch(h -> TestConstants.IP_3.equals(h.ip)));
        }

        @Test void remove_nonExistent_doesNotThrow() {
            assertDoesNotThrow(() -> store.remove("99.99.99.99"));
        }

        @Test void updateNotes_persists() {
            store.save(new HostResult(TestConstants.IP_4, TestConstants.HOST_4, "Linux", null));
            store.updateNotes(TestConstants.IP_4, "my note");
            assertTrue(store.getAll().stream()
                    .filter(h -> TestConstants.IP_4.equals(h.ip))
                    .anyMatch(h -> "my note".equals(h.notes)));
        }

        @Test void addChangeListener_notifiedOnSave() {
            int[] count = {0};
            store.addChangeListener(() -> count[0]++);
            store.save(new HostResult(TestConstants.IP_5, TestConstants.HOST_5, "Win", null));
            assertTrue(count[0] >= 1);
        }

        @Test void getAll_notNull()             { assertNotNull(store.getAll()); }
        @Test void save_null_doesNotThrow()     { assertDoesNotThrow(() -> store.save(null)); }
        @Test void save_blankIp_doesNotThrow()  { assertDoesNotThrow(() -> store.save(new HostResult("", TestConstants.HOST_6, "Linux", null))); }

        @Test void persistence_survivesReinit() {
            store.save(new HostResult(TestConstants.IP_6, TestConstants.HOST_7, "Linux", null));
            store.setFilePath(tmp.resolve("saved_hosts.txt"));
            assertTrue(store.getAll().stream().anyMatch(h -> TestConstants.IP_6.equals(h.ip)));
        }
    }
}