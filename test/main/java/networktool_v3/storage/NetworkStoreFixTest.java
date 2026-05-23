package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für NetworkStore-Fix: AutoBackup außerhalb synchronized, kein Deadlock. */
class NetworkStoreFixTest {

    static final String NET = "__fix_test__";
    NetworkStore store = NetworkStore.getInstance();

    @BeforeEach
    void setup() {
        if (!store.getNetworkNames().contains(NET))
            store.createNetwork(NET, "88.88.");
    }

    @AfterEach
    void teardown() {
        store.deleteNetwork(NET);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void save_doesNotDeadlock() {
        // Vor dem Fix: AutoBackup.triggerNow() war synchronized — Deadlock möglich
        HostResult h = new HostResult("88.88.0.1", "host", "Linux");
        assertDoesNotThrow(() -> store.save(h, NET));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void save_concurrentGetAllHosts_noDeadlock() throws InterruptedException {
        HostResult h = new HostResult("88.88.0.2", "host2", "Win");
        Thread saver = new Thread(() -> store.save(h, NET));
        Thread reader = new Thread(() -> store.getAllHosts());
        saver.start(); reader.start();
        saver.join(3000); reader.join(3000);
        assertFalse(saver.isAlive(), "saver hängt (Deadlock?)");
        assertFalse(reader.isAlive(), "reader hängt (Deadlock?)");
    }

    @Test
    void save_returnsFalse_wrongPrefix() {
        HostResult h = new HostResult("10.0.0.1", "wrong", "Linux");
        assertFalse(store.save(h, NET));
    }

    @Test
    void save_returnsFalse_nullHost() {
        assertFalse(store.save(null, NET));
    }

    @Test
    void remove_outsideSynchronized_doesNotDeadlock() throws InterruptedException {
        store.save(new HostResult("88.88.0.3", "h", "Linux"), NET);
        Thread remover = new Thread(() -> store.remove("88.88.0.3", NET));
        Thread reader  = new Thread(() -> store.getAllHosts());
        remover.start(); reader.start();
        remover.join(3000); reader.join(3000);
        assertFalse(remover.isAlive());
        assertFalse(reader.isAlive());
    }
}