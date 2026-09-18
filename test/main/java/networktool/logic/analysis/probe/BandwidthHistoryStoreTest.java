package main.java.networktool.logic.analysis.probe;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BandwidthHistoryStoreTest {

    private static final String IP = "__junit__203.0.113.5";
    BandwidthHistoryStore store = BandwidthHistoryStore.getInstance();

    @AfterEach void cleanup() { store.clear(IP); }

    @Test void record_and_getHistory() {
        store.record(IP, 10.0, 2.0);
        List<BandwidthHistoryEntry> h = store.getHistory(IP);
        assertEquals(1, h.size());
        assertEquals(10.0, h.get(0).downMbps());
    }

    @Test void getHistory_unknownHost_empty() {
        assertTrue(store.getHistory("__junit__nonexistent__").isEmpty());
    }

    @Test void record_multipleEntries_allPersisted() {
        store.record(IP, 1, 1);
        store.record(IP, 2, 2);
        store.record(IP, 3, 3);
        assertEquals(3, store.getHistory(IP).size());
    }

    @Test void clear_removesHistory() {
        store.record(IP, 5, 5);
        store.clear(IP);
        assertTrue(store.getHistory(IP).isEmpty());
    }

    @Test void record_blankIp_doesNotThrow() {
        assertDoesNotThrow(() -> store.record("", 1, 1));
        assertDoesNotThrow(() -> store.record(null, 1, 1));
    }

    @Test void rotation_keepsMaxEntries() {
        for (int i = 0; i < BandwidthHistoryStore.MAX_ENTRIES + 20; i++) store.record(IP, i, i);
        assertEquals(BandwidthHistoryStore.MAX_ENTRIES, store.getHistory(IP).size());
    }

    @Test void rotation_keepsNewestEntries() {
        for (int i = 0; i < BandwidthHistoryStore.MAX_ENTRIES + 5; i++) store.record(IP, i, i);
        List<BandwidthHistoryEntry> h = store.getHistory(IP);
        assertEquals(BandwidthHistoryStore.MAX_ENTRIES + 4.0, h.get(h.size() - 1).downMbps());
    }

    @Test void differentHosts_separateHistories() {
        String ipB = "__junit__203.0.113.6";
        store.record(IP, 1, 1);
        store.record(ipB, 2, 2);
        assertEquals(1, store.getHistory(IP).size());
        assertEquals(1, store.getHistory(ipB).size());
        store.clear(ipB);
    }
}
