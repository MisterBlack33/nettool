package main.java.networktool.storage;

import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NetworkStoreFixTest {

    static final String NET = TestConstants.NET_FIX;    // "__junit__fix"
    static final String PFX = TestConstants.PREFIX_88;  // "88.88."
    NetworkStore store = NetworkStore.getInstance();

    @BeforeEach
    void setup() {
        if (!store.getAllNetworkNames().contains(NET))
            store.createNetwork(NET, PFX);
    }

    @AfterEach
    void teardown() {
        store.deleteNetwork(NET);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void save_doesNotDeadlock() {
        assertDoesNotThrow(() -> store.save(
                new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux"), NET));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void save_concurrentGetAllHosts_noDeadlock() throws InterruptedException {
        HostResult h = new HostResult(TestConstants.IP_2, TestConstants.HOST_2, "Win");
        Thread saver  = new Thread(() -> store.save(h, NET));
        Thread reader = new Thread(() -> store.getAllHosts());
        saver.start(); reader.start();
        saver.join(3000); reader.join(3000);
        assertFalse(saver.isAlive(),  "saver hängt (Deadlock?)");
        assertFalse(reader.isAlive(), "reader hängt (Deadlock?)");
    }

    @Test void save_returnsFalse_wrongPrefix() {
        assertFalse(store.save(new HostResult("10.0.0.1", TestConstants.HOST_3, "Linux"), NET));
    }

    @Test void save_returnsFalse_nullHost() {
        assertFalse(store.save(null, NET));
    }

    @Test void testNetwork_hidden_from_gui() {
        assertFalse(store.getNetworkNames().contains(NET));
        store.save(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux"), NET);
        assertFalse(store.getAllHosts().stream().anyMatch(x -> TestConstants.IP_1.equals(x.ip)));
        store.remove(TestConstants.IP_1, NET);
    }

    @Test
    void remove_outsideSynchronized_doesNotDeadlock() throws InterruptedException {
        store.save(new HostResult(TestConstants.IP_3, TestConstants.HOST_4, "Linux"), NET);
        Thread remover = new Thread(() -> store.remove(TestConstants.IP_3, NET));
        Thread reader  = new Thread(() -> store.getAllHosts());
        remover.start(); reader.start();
        remover.join(3000); reader.join(3000);
        assertFalse(remover.isAlive());
        assertFalse(reader.isAlive());
    }
}