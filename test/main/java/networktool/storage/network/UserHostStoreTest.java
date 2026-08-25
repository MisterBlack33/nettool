package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.TestConstants;
import main.java.networktool.storage.network.NetworkStoreTestBase;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserHostStoreTest extends NetworkStoreTestBase {

    NetworkStore store = NetworkStore.getInstance();
    final String NET = TestConstants.NET_STANDARD;
    final String PFX = TestConstants.PREFIX_88;
    final String IP  = TestConstants.IP_1;

    @BeforeEach void setup() {
        if (!store.getAllNetworkNames().contains(NET)) store.createNetwork(NET, PFX);
    }

    @AfterEach void teardown() {
        UserHostStore.remove(IP, NET, "user3");
        UserHostStore.remove(IP, NET, "user4");
        store.deleteNetwork(NET);
    }

    @Test void save_visibleToOwner() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        assertTrue(UserHostStore.getAll(NET, "user3").stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void save_notVisibleToOtherUser() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        assertFalse(UserHostStore.getAll(NET, "user4").stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void save_sameHostBySecondUser_storedOnceButVisibleToBoth() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user4");
        assertEquals(1, store.getAll(NET).stream().filter(h -> h.ip.equals(IP)).count());
        assertTrue(UserHostStore.getAll(NET, "user3").stream().anyMatch(h -> h.ip.equals(IP)));
        assertTrue(UserHostStore.getAll(NET, "user4").stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void remove_withRemainingOwner_dataStays() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user4");
        UserHostStore.remove(IP, NET, "user3");
        assertTrue(store.getAll(NET).stream().anyMatch(h -> h.ip.equals(IP)));
        assertFalse(UserHostStore.getAll(NET, "user3").stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void remove_lastOwner_dataDeleted() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        UserHostStore.remove(IP, NET, "user3");
        assertFalse(store.getAll(NET).stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void save_nullUsername_returnsFalse() {
        assertFalse(UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, null));
    }

    @Test void save_nullHost_returnsFalse() {
        assertFalse(UserHostStore.save(null, NET, "user3"));
    }

    @Test void getAll_nullUsername_empty() {
        assertTrue(UserHostStore.getAll(NET, null).isEmpty());
    }

    @Test void getAllHosts_acrossNetworks_containsSaved() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        List<HostResult> all = UserHostStore.getAllHosts("user3");
        assertTrue(all.stream().anyMatch(h -> h.ip.equals(IP)));
    }

    @Test void getAllHosts_nullUsername_empty() {
        assertTrue(UserHostStore.getAllHosts(null).isEmpty());
    }

    @Test void getCoOwners_returnsBothUsers() {
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user3");
        UserHostStore.save(new HostResult(IP, TestConstants.HOST_1, "Linux"), NET, "user4");
        assertEquals(2, UserHostStore.getCoOwners(IP, NET).size());
    }
}