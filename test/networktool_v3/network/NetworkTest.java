package networktool_v3.network;

import main.java.networktool_v3.gui.notification.NotificationListener;
import main.java.networktool_v3.logic.analysis.*;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.storage.TestConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {

    // ══════════════════════════════════════════════════════════════
    //  OuiDatabase
    // ══════════════════════════════════════════════════════════════

    @Nested
    class OuiDatabaseTest {

        @Test void lookup_apple_oui() {
            String v = OuiDatabase.lookup("00:03:93");
            assertNotNull(v);
            assertTrue(v.contains("Apple") || v.contains("iOS") || v.contains("macOS"));
        }

        @Test void lookup_raspberry()          { assertTrue(OuiDatabase.lookup("B8:27:EB").contains("Raspberry")); }
        @Test void lookup_samsung()            { assertTrue(OuiDatabase.lookup("00:12:47").contains("Samsung")); }
        @Test void lookup_nothing_phone()      { assertTrue(OuiDatabase.lookup("7C:96:D2").contains("Nothing")); }
        @Test void lookup_unknown_returnsNull(){ assertNull(OuiDatabase.lookup("FF:FF:FF")); }
        @Test void lookup_null_returnsNull()   { assertNull(OuiDatabase.lookup(null)); }

        @Test void lookup_caseInsensitive() {
            assertEquals(OuiDatabase.lookup("B8:27:EB"), OuiDatabase.lookup("b8:27:eb"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkStore – CRUD  (isolated to test.eintrag.N + __junit__ prefix)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkStoreCrudTest {

        NetworkStore store = NetworkStore.getInstance();
        final String NET = TestConstants.NET_NETWORK;     // "__junit__network"
        final String PFX = TestConstants.PREFIX_99;       // "99.99."

        @BeforeEach
        void setup() {
            if (!store.getAllNetworkNames().contains(NET))
                store.createNetwork(NET, PFX);
        }

        @AfterEach
        void teardown() {
            store.deleteNetwork(NET);
        }

        @Test void createNetwork_appears_in_allNames() {
            assertTrue(store.getAllNetworkNames().contains(NET));
        }

        @Test void testNetwork_hidden_from_gui_getNetworkNames() {
            assertFalse(store.getNetworkNames().contains(NET),
                    "Test network must not appear in GUI network list");
        }

        @Test void saveHost_and_retrieve() {
            HostResult h = new HostResult(TestConstants.IP_7, TestConstants.HOST_1, "Linux", null, null, "");
            store.save(h, NET);
            assertTrue(store.getAll(NET).stream().anyMatch(x -> TestConstants.IP_7.equals(x.ip)));
        }

        @Test void savedTestHost_hidden_from_getAllHosts() {
            store.save(new HostResult(TestConstants.IP_8, TestConstants.HOST_2, "Linux"), NET);
            assertFalse(store.getAllHosts().stream().anyMatch(x -> TestConstants.IP_8.equals(x.ip)),
                    "Test hosts must not appear in GUI getAllHosts()");
            store.remove(TestConstants.IP_8, NET);
        }

        @Test void removeHost() {
            HostResult h = new HostResult(TestConstants.IP_8, TestConstants.HOST_2, "Linux");
            store.save(h, NET);
            store.remove(TestConstants.IP_8, NET);
            assertFalse(store.getAll(NET).stream().anyMatch(x -> TestConstants.IP_8.equals(x.ip)));
        }

        @Test void findNetwork_returnsCategoryName() {
            HostResult h = new HostResult(TestConstants.IP_9, TestConstants.HOST_3, "Win");
            store.save(h, NET);
            assertEquals(NET, store.findNetwork(TestConstants.IP_9));
            store.remove(TestConstants.IP_9, NET);
        }

        @Test void updateNotes_persists() {
            HostResult h = new HostResult(TestConstants.IP_10, TestConstants.HOST_4, "Lin");
            store.save(h, NET);
            store.updateNotes(TestConstants.IP_10, NET, "my new note");
            assertTrue(store.getAll(NET).stream()
                    .filter(x -> TestConstants.IP_10.equals(x.ip))
                    .anyMatch(x -> "my new note".equals(x.notes)));
            store.remove(TestConstants.IP_10, NET);
        }

        @Test void ipMatchesNetwork_true()  { assertTrue(store.ipMatchesNetwork(TestConstants.IP_7, NET)); }
        @Test void ipMatchesNetwork_false() { assertFalse(store.ipMatchesNetwork("10.0.0.1", NET)); }
        @Test void allCategory_alwaysTrue() { assertTrue(store.ipMatchesNetwork("1.2.3.4", NetworkStore.ALL_CATEGORY)); }

        @Test void save_wrongPrefix_rejected() {
            assertFalse(store.save(new HostResult("10.0.0.99", TestConstants.HOST_5, "Win"), NET));
        }

        @Test void renameNetwork() {
            store.createNetwork(TestConstants.NET_RENAME_SRC, "");
            store.renameNetwork(TestConstants.NET_RENAME_SRC, TestConstants.NET_RENAME_DST);
            assertFalse(store.getAllNetworkNames().contains(TestConstants.NET_RENAME_SRC));
            assertTrue(store.getAllNetworkNames().contains(TestConstants.NET_RENAME_DST));
            store.deleteNetwork(TestConstants.NET_RENAME_DST);
        }

        @Test void getPrefix_returnsCorrect() {
            assertEquals(PFX, store.getPrefix(NET));
        }

        @Test void getAllHostsInternal_includesTestEntries() {
            store.save(new HostResult(TestConstants.IP_9, TestConstants.HOST_6, "Linux"), NET);
            assertTrue(store.getAllHostsInternal().stream().anyMatch(x -> TestConstants.IP_9.equals(x.ip)));
            store.remove(TestConstants.IP_9, NET);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PortScanner static state
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PortScannerStaticTest {

        @Test void defaultPorts_notEmpty() { assertFalse(main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS.isEmpty()); }
        @Test void fastPorts_notEmpty()    { assertFalse(main.java.networktool_v3.logic.ports.PortScanner.FAST_PORTS.isEmpty()); }

        @Test void setActivePorts_null_resetsToDefault() {
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(null);
            assertEquals(main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS,
                    main.java.networktool_v3.logic.ports.PortScanner.getActivePorts());
        }

        @Test void setActivePorts_custom() {
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(List.of(22, 80, 443));
            assertEquals(3, main.java.networktool_v3.logic.ports.PortScanner.getActivePorts().size());
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(null);
        }

        @Test void setActivePorts_empty_resetsToDefault() {
            main.java.networktool_v3.logic.ports.PortScanner.setActivePorts(List.of());
            assertEquals(main.java.networktool_v3.logic.ports.PortScanner.DEFAULT_PORTS,
                    main.java.networktool_v3.logic.ports.PortScanner.getActivePorts());
        }
    }
}