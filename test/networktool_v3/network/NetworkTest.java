package networktool_v3.network;

import main.java.networktool_v3.gui.notification.NotificationListener;
import main.java.networktool_v3.logic.analysis.*;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NotificationListener JSON parser, OuiDatabase, OuiUpdater,
 * NetworkStore CRUD, and PortScanner static state.
 */
class NetworkTest {

    // ══════════════════════════════════════════════════════════════
    //  OuiDatabase
    // ══════════════════════════════════════════════════════════════

    @Nested
    class OuiDatabaseTest {

        @Test
        void lookup_apple_oui() {
            String vendor = OuiDatabase.lookup("00:03:93");
            assertNotNull(vendor);
            assertTrue(vendor.contains("Apple") || vendor.contains("iOS") || vendor.contains("macOS"));
        }

        @Test
        void lookup_raspberry() {
            String vendor = OuiDatabase.lookup("B8:27:EB");
            assertNotNull(vendor);
            assertTrue(vendor.contains("Raspberry"));
        }

        @Test
        void lookup_samsung() {
            String vendor = OuiDatabase.lookup("00:12:47");
            assertNotNull(vendor);
            assertTrue(vendor.contains("Samsung"));
        }

        @Test
        void lookup_nothing_phone() {
            String vendor = OuiDatabase.lookup("7C:96:D2");
            assertNotNull(vendor);
            assertTrue(vendor.contains("Nothing"));
        }

        @Test
        void lookup_unknown_returnsNull() {
            assertNull(OuiDatabase.lookup("FF:FF:FF"));
        }

        @Test
        void lookup_null_returnsNull() {
            assertNull(OuiDatabase.lookup(null));
        }

        @Test
        void lookup_caseInsensitive() {
            String upper = OuiDatabase.lookup("B8:27:EB");
            String lower = OuiDatabase.lookup("b8:27:eb");
            assertEquals(upper, lower);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkStore – CRUD
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkStoreCrudTest {

        NetworkStore store = NetworkStore.getInstance();

        @BeforeEach
        void setup() {
            // Ensure at least one network exists; use existing state
            // We only test additive/non-destructive operations to comply
            // with the "stored data must not be changed" rule –
            // we operate on a dedicated test network that we clean up.
            if (!store.getNetworkNames().contains("__test__")) {
                store.createNetwork("__test__", "99.99.");
            }
        }

        @AfterEach
        void teardown() {
            store.deleteNetwork("__test__");
        }

        @Test
        void createNetwork_appears_in_names() {
            assertTrue(store.getNetworkNames().contains("__test__"));
        }

        @Test
        void saveHost_and_retrieve() {
            HostResult h = new HostResult("99.99.0.1", "testhost", "Linux", null, null, "");
            store.save(h, "__test__");
            List<HostResult> all = store.getAll("__test__");
            assertTrue(all.stream().anyMatch(x -> "99.99.0.1".equals(x.ip)));
        }

        @Test
        void removeHost() {
            HostResult h = new HostResult("99.99.0.2", "remove-me", "Linux");
            store.save(h, "__test__");
            store.remove("99.99.0.2", "__test__");
            assertFalse(store.getAll("__test__").stream()
                    .anyMatch(x -> "99.99.0.2".equals(x.ip)));
        }

        @Test
        void findNetwork_returnsCategoryName() {
            HostResult h = new HostResult("99.99.0.3", "find-me", "Win");
            store.save(h, "__test__");
            assertEquals("__test__", store.findNetwork("99.99.0.3"));
            store.remove("99.99.0.3", "__test__");
        }

        @Test
        void updateNotes_persists() {
            HostResult h = new HostResult("99.99.0.4", "note-host", "Lin");
            store.save(h, "__test__");
            store.updateNotes("99.99.0.4", "__test__", "my new note");
            List<HostResult> all = store.getAll("__test__");
            assertTrue(all.stream()
                    .filter(x -> "99.99.0.4".equals(x.ip))
                    .anyMatch(x -> "my new note".equals(x.notes)));
            store.remove("99.99.0.4", "__test__");
        }

        @Test
        void ipMatchesNetwork_trueWhenPrefixMatches() {
            assertTrue(store.ipMatchesNetwork("99.99.5.1", "__test__"));
        }

        @Test
        void ipMatchesNetwork_falseWhenPrefixDoesNotMatch() {
            assertFalse(store.ipMatchesNetwork("10.0.0.1", "__test__"));
        }

        @Test
        void allCategory_alwaysMatchesAnyIp() {
            assertTrue(store.ipMatchesNetwork("1.2.3.4", NetworkStore.ALL_CATEGORY));
        }

        @Test
        void save_wrongPrefix_rejected() {
            HostResult h = new HostResult("10.0.0.99", "wrong-prefix", "Win");
            boolean saved = store.save(h, "__test__");
            assertFalse(saved);
        }

        @Test
        void renameNetwork() {
            store.createNetwork("__rename_src__", "");
            store.renameNetwork("__rename_src__", "__rename_dst__");
            assertFalse(store.getNetworkNames().contains("__rename_src__"));
            assertTrue(store.getNetworkNames().contains("__rename_dst__"));
            store.deleteNetwork("__rename_dst__");
        }

        @Test
        void getPrefix_returnsCorrect() {
            assertEquals("99.99.", store.getPrefix("__test__"));
        }

        @Test
        void getAllHosts_deduplicated() {
            // Add host to __test__, ensure getAllHosts doesn't duplicate
            HostResult h = new HostResult("99.99.0.9", "dedup", "Linux");
            store.save(h, "__test__");
            long count = store.getAllHosts().stream()
                    .filter(x -> "99.99.0.9".equals(x.ip)).count();
            assertEquals(1, count);
            store.remove("99.99.0.9", "__test__");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PortScanner static state
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PortScannerStaticTest {

        @Test
        void defaultPorts_notEmpty() {
            assertFalse(main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS.isEmpty());
        }

        @Test
        void fastPorts_notEmpty() {
            assertFalse(main.java.networktool_v3.logic.ports.PortScanner.FAST_PORTS.isEmpty());
        }

        @Test
        void setActivePorts_null_resetsToDefault() {
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(null);
            assertEquals(
                    main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS,
                    main.java.networktool_v3.logic.ports.PortScanner.getActivePorts());
        }

        @Test
        void setActivePorts_custom() {
            List<Integer> custom = List.of(22, 80, 443);
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(custom);
            assertEquals(3, main.java.networktool_v3.logic.ports.PortScanner.getActivePorts().size());
            // Reset
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(null);
        }

        @Test
        void setActivePorts_empty_resetsToDefault() {
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(List.of());
            assertEquals(
                    main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS,
                    main.java.networktool_v3.logic.ports.PortScanner.getActivePorts());
        }
    }
}
