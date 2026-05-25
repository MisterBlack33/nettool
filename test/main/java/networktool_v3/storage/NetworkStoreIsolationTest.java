package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that __junit__ networks and their hosts are completely
 * invisible to normal GUI code paths.
 */
class NetworkStoreIsolationTest {

    NetworkStore store = NetworkStore.getInstance();
    final String NET = TestConstants.NET_STANDARD;
    final String PFX = TestConstants.PREFIX_88;

    @BeforeEach void setup() {
        if (!store.getAllNetworkNames().contains(NET))
            store.createNetwork(NET, PFX);
    }

    @AfterEach void teardown() {
        store.deleteNetwork(NET);
    }

    @Test void testNetwork_absent_from_getNetworkNames() {
        assertFalse(store.getNetworkNames().contains(NET));
    }

    @Test void testNetwork_present_in_getAllNetworkNames() {
        assertTrue(store.getAllNetworkNames().contains(NET));
    }

    @Test void testHost_absent_from_getAllHosts() {
        store.save(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux"), NET);
        List<HostResult> gui = store.getAllHosts();
        assertFalse(gui.stream().anyMatch(h -> h.ip.equals(TestConstants.IP_1)),
                "Test host must not appear in getAllHosts() (GUI path)");
        store.remove(TestConstants.IP_1, NET);
    }

    @Test void testHost_absent_from_getAll_allCategory() {
        store.save(new HostResult(TestConstants.IP_2, TestConstants.HOST_2, "Win"), NET);
        List<HostResult> all = store.getAll(NetworkStore.ALL_CATEGORY);
        assertFalse(all.stream().anyMatch(h -> h.ip.equals(TestConstants.IP_2)),
                "Test host must not appear in getAll(ALL_CATEGORY)");
        store.remove(TestConstants.IP_2, NET);
    }

    @Test void testHost_present_in_getAllHostsInternal() {
        store.save(new HostResult(TestConstants.IP_3, TestConstants.HOST_3, "Linux"), NET);
        assertTrue(store.getAllHostsInternal().stream().anyMatch(h -> h.ip.equals(TestConstants.IP_3)));
        store.remove(TestConstants.IP_3, NET);
    }

    @Test void testHost_accessible_via_getAll_with_testNet() {
        store.save(new HostResult(TestConstants.IP_4, TestConstants.HOST_4, "Linux"), NET);
        assertTrue(store.getAll(NET).stream().anyMatch(h -> h.ip.equals(TestConstants.IP_4)));
        store.remove(TestConstants.IP_4, NET);
    }

    @Test void isTestNetwork_prefix_check() {
        assertTrue(NetworkStore.isTestNetwork(TestConstants.TEST_PREFIX + "anything"));
        assertFalse(NetworkStore.isTestNetwork("Standard"));
        assertFalse(NetworkStore.isTestNetwork(null));
    }

    @Test void multipleTestNets_allHidden_from_gui() {
        store.createNetwork(TestConstants.NET_EXT, TestConstants.PREFIX_99);
        store.save(new HostResult(TestConstants.IP_7, TestConstants.HOST_5, "Win"), TestConstants.NET_EXT);

        assertFalse(store.getNetworkNames().contains(TestConstants.NET_EXT));
        assertFalse(store.getAllHosts().stream().anyMatch(h -> h.ip.equals(TestConstants.IP_7)));

        store.remove(TestConstants.IP_7, TestConstants.NET_EXT);
        store.deleteNetwork(TestConstants.NET_EXT);
    }

    @Test void realNetwork_still_visible() {
        // Ensure filtering only targets __junit__ prefix, not all networks
        List<String> names = store.getNetworkNames();
        assertFalse(names.isEmpty(), "Real networks must remain visible");
        // ALL_CATEGORY is always present
        assertTrue(names.contains(NetworkStore.ALL_CATEGORY));
    }
}