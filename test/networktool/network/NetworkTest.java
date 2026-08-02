package networktool.network;

import main.java.networktool.logic.analysis.OuiDatabase;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.logic.scan.NetworkTimeoutTestBase;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.AutoBackup;
import main.java.networktool.storage.NetworkStore;
import main.java.networktool.storage.TestConstants;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest extends NetworkTimeoutTestBase {

    // ══════════════════════════════════════════════════════════════
    //  OuiDatabase
    // ══════════════════════════════════════════════════════════════

    @Nested
    class OuiDatabaseTest {

        @Test void lookup_apple_oui()          { assertNotNull(OuiDatabase.lookup("00:03:93")); }
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
    //  NetworkStore CRUD
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkStoreCrudTest {

        NetworkStore store = NetworkStore.getInstance();
        final String NET = TestConstants.NET_NETWORK;
        final String PFX = TestConstants.PREFIX_99;

        @BeforeEach void setup() {
            AutoBackup.getInstance().cleanupBackups();
            if (!store.getAllNetworkNames().contains(NET))
                store.createNetwork(NET, PFX);
        }

        @AfterEach void teardown() {
            store.deleteNetwork(NET);
            AutoBackup.getInstance().cleanupBackups();
        }

        @Test void createNetwork_appears_in_allNames()             { assertTrue(store.getAllNetworkNames().contains(NET)); }
        @Test void testNetwork_hidden_from_gui_getNetworkNames()   { assertFalse(store.getNetworkNames().contains(NET)); }

        @Test void saveHost_and_retrieve() {
            store.save(new HostResult(TestConstants.IP_7, TestConstants.HOST_1, "Linux", null, null, ""), NET);
            assertTrue(store.getAll(NET).stream().anyMatch(x -> TestConstants.IP_7.equals(x.ip)));
        }

        @Test void savedTestHost_hidden_from_getAllHosts() {
            store.save(new HostResult(TestConstants.IP_8, TestConstants.HOST_2, "Linux"), NET);
            assertFalse(store.getAllHosts().stream().anyMatch(x -> TestConstants.IP_8.equals(x.ip)));
            store.remove(TestConstants.IP_8, NET);
        }

        @Test void removeHost() {
            store.save(new HostResult(TestConstants.IP_8, TestConstants.HOST_2, "Linux"), NET);
            store.remove(TestConstants.IP_8, NET);
            assertFalse(store.getAll(NET).stream().anyMatch(x -> TestConstants.IP_8.equals(x.ip)));
        }

        @Test void findNetwork_returnsCategoryName() {
            store.save(new HostResult(TestConstants.IP_9, TestConstants.HOST_3, "Win"), NET);
            assertEquals(NET, store.findNetwork(TestConstants.IP_9));
            store.remove(TestConstants.IP_9, NET);
        }

        @Test void updateNotes_persists() {
            store.save(new HostResult(TestConstants.IP_10, TestConstants.HOST_4, "Lin"), NET);
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

        @Test void getPrefix_returnsCorrect() { assertEquals(PFX, store.getPrefix(NET)); }

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

        @Test void defaultPorts_notEmpty() { assertFalse(PortScanner.DEFAULT_PORTS.isEmpty()); }
        @Test void fastPorts_notEmpty()    { assertFalse(PortScanner.FAST_PORTS.isEmpty()); }

        @Test void setActivePorts_null_resetsToDefault() {
            PortScanner.setActivePorts(null);
            assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
        }

        @Test void setActivePorts_custom() {
            PortScanner.setActivePorts(List.of(22, 80, 443));
            assertEquals(3, PortScanner.getActivePorts().size());
            PortScanner.setActivePorts(null);
        }

        @Test void setActivePorts_empty_resetsToDefault() {
            PortScanner.setActivePorts(List.of());
            assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
        }
    }
}