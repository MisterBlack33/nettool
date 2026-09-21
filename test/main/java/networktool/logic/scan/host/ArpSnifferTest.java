package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ArpSnifferTest {

    private static final String MAC_A = "AA:BB:CC:DD:EE:01";
    private static final String MAC_B = "AA:BB:CC:DD:EE:02";

    private final Map<String, String> table = new ConcurrentHashMap<>();
    private final List<ArpSighting> seen = new CopyOnWriteArrayList<>();
    private final ArpSniffer sniffer = new ArpSniffer(() -> table);

    @AfterEach void stop() { sniffer.stop(); }

    @Test void poll_afterBaseline_unchangedTable_reportsNothing() {
        table.put("10.0.0.1", MAC_A);
        sniffer.start(seen::add);
        sniffer.poll();
        assertTrue(seen.isEmpty());
    }

    @Test void start_seedsBaselineSilently() {
        table.put("10.0.0.1", MAC_A);
        sniffer.start(seen::add);
        assertEquals(MAC_A, sniffer.snapshot().get("10.0.0.1"));
        assertTrue(seen.isEmpty());
    }

    @Test void poll_newHost_isReported() {
        sniffer.start(seen::add);
        table.put("10.0.0.2", MAC_A);
        sniffer.poll();
        assertEquals(1, seen.size());
        assertTrue(seen.get(0).isNewHost());
        assertEquals("10.0.0.2", seen.get(0).ip());
    }

    @Test void poll_newHost_reportedOnlyOnce() {
        sniffer.start(seen::add);
        table.put("10.0.0.2", MAC_A);
        sniffer.poll();
        sniffer.poll();
        assertEquals(1, seen.size());
    }

    @Test void poll_macChange_carriesPreviousMac() {
        table.put("10.0.0.3", MAC_A);
        sniffer.start(seen::add);
        table.put("10.0.0.3", MAC_B);
        sniffer.poll();
        assertFalse(seen.get(0).isNewHost());
        assertEquals(MAC_A, seen.get(0).previousMac());
        assertEquals(MAC_B, seen.get(0).mac());
    }

    @Test void poll_withoutListener_doesNotThrow() {
        table.put("10.0.0.4", MAC_A);
        assertDoesNotThrow(sniffer::poll);
    }

    @Test void poll_listenerThrows_isContained() {
        sniffer.start(s -> { throw new IllegalStateException("boom"); });
        table.put("10.0.0.5", MAC_A);
        assertDoesNotThrow(sniffer::poll);
    }

    @Test void poll_sourceThrows_isContained() {
        ArpSniffer failing = new ArpSniffer(() -> { throw new IllegalStateException("boom"); });
        assertDoesNotThrow(failing::poll);
        assertTrue(failing.snapshot().isEmpty());
    }

    @Test void lifecycle_startStop() {
        assertFalse(sniffer.isActive());
        sniffer.start(seen::add);
        assertTrue(sniffer.isActive());
        sniffer.stop();
        assertFalse(sniffer.isActive());
    }

    @Test void start_twice_keepsFirstListener() {
        List<ArpSighting> second = new CopyOnWriteArrayList<>();
        sniffer.start(seen::add);
        sniffer.start(second::add);
        table.put("10.0.0.6", MAC_A);
        sniffer.poll();
        assertEquals(1, seen.size());
        assertTrue(second.isEmpty());
    }

    @Test void stop_whenInactive_doesNotThrow() { assertDoesNotThrow(sniffer::stop); }

    @Test void stop_dropsListener() {
        sniffer.start(seen::add);
        sniffer.stop();
        table.put("10.0.0.7", MAC_A);
        sniffer.poll();
        assertTrue(seen.isEmpty());
    }

    @Test void snapshot_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> sniffer.snapshot().put("x", "y"));
    }

    @Test void getInstance_isSingleton() { assertSame(ArpSniffer.getInstance(), ArpSniffer.getInstance()); }

    @Test void sighting_isNewHost_dependsOnPreviousMac() {
        assertTrue(new ArpSighting("1.1.1.1", MAC_A, null).isNewHost());
        assertFalse(new ArpSighting("1.1.1.1", MAC_A, MAC_B).isNewHost());
    }
}
