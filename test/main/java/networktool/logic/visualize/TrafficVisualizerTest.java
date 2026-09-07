package main.java.networktool.logic.visualize;

import main.java.networktool.logic.sonify.InterfaceStatsReader;
import networktool.util.PollHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TrafficVisualizerTest {

    TrafficVisualizer v = TrafficVisualizer.getInstance();

    @AfterEach void cleanup() {
        v.stop();
        v.clear();
        v.setCapacity(120);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Test void isActive_initiallyFalse() { assertFalse(v.isActive()); }

    @Test void start_setsActive() {
        v.start("__test_iface__");
        assertTrue(v.isActive());
    }

    @Test void start_setsActiveInterface() {
        v.start("eth-test-1");
        assertEquals("eth-test-1", v.getActiveInterface());
    }

    @Test void stop_clearsActive() {
        v.start("__t__");
        v.stop();
        assertFalse(v.isActive());
    }

    @Test void startTwice_doesNotThrow_secondStartIgnored() {
        v.start("first");
        v.start("second");
        assertEquals("first", v.getActiveInterface());
    }

    @Test void stop_whenInactive_doesNotThrow() {
        assertDoesNotThrow(v::stop);
    }

    // ── Buffer / snapshot ─────────────────────────────────────────────────

    @Test void getSnapshot_initiallyEmpty() {
        assertTrue(v.getSnapshot().isEmpty());
    }

    @Test void offerSample_appearsInSnapshot() {
        v.offerSample(new TrafficSample(1, 10, 20));
        List<TrafficSample> snap = v.getSnapshot();
        assertEquals(1, snap.size());
        assertEquals(10, snap.get(0).rxDelta());
    }

    @Test void clear_removesAllSamples() {
        v.offerSample(new TrafficSample(1, 1, 1));
        v.clear();
        assertTrue(v.getSnapshot().isEmpty());
    }

    @Test void getSnapshot_isUnmodifiable() {
        v.offerSample(new TrafficSample(1, 1, 1));
        List<TrafficSample> snap = v.getSnapshot();
        assertThrows(UnsupportedOperationException.class, () -> snap.add(new TrafficSample(2, 2, 2)));
    }

    @Test void getSnapshot_isCopy_notLiveView() {
        v.offerSample(new TrafficSample(1, 1, 1));
        List<TrafficSample> snap = v.getSnapshot();
        v.offerSample(new TrafficSample(2, 2, 2));
        assertEquals(1, snap.size());
    }

    @Test void samples_orderedOldestFirst() {
        v.offerSample(new TrafficSample(1, 1, 1));
        v.offerSample(new TrafficSample(2, 2, 2));
        List<TrafficSample> snap = v.getSnapshot();
        assertEquals(1, snap.get(0).timestampMs());
        assertEquals(2, snap.get(1).timestampMs());
    }

    // ── Capacity ──────────────────────────────────────────────────────────

    @Test void setCapacity_trimsExistingBuffer() {
        for (int i = 0; i < 5; i++) v.offerSample(new TrafficSample(i, i, i));
        v.setCapacity(2);
        assertEquals(2, v.getSnapshot().size());
    }

    @Test void setCapacity_keepsNewestSamples() {
        for (int i = 0; i < 5; i++) v.offerSample(new TrafficSample(i, i, i));
        v.setCapacity(2);
        List<TrafficSample> snap = v.getSnapshot();
        assertEquals(3, snap.get(0).timestampMs());
        assertEquals(4, snap.get(1).timestampMs());
    }

    @Test void setCapacity_zeroOrNegative_ignored() {
        v.offerSample(new TrafficSample(1, 1, 1));
        v.setCapacity(0);
        v.setCapacity(-5);
        assertEquals(1, v.getSnapshot().size());
    }

    @Test void offerSample_beyondCapacity_dropsOldest() {
        v.setCapacity(3);
        for (int i = 0; i < 5; i++) v.offerSample(new TrafficSample(i, i, i));
        List<TrafficSample> snap = v.getSnapshot();
        assertEquals(3, snap.size());
        assertEquals(2, snap.get(0).timestampMs());
    }

    // ── Real acquisition loop (best-effort, skipped if stats unreadable) ──

    @Test void start_realInterface_eventuallyProducesSample() {
        assumeTrue(InterfaceStatsReader.read("lo") != null, "Loopback-Statistiken nicht lesbar in dieser Umgebung");
        v.start("lo");
        assertTrue(PollHelper.waitFor(() -> !v.getSnapshot().isEmpty(), 3000),
                "Sollte innerhalb von 3s mindestens ein Sample erfassen");
    }

    @Test void start_unknownInterface_doesNotThrow_noSamples() throws InterruptedException {
        v.start("__nonexistent_iface_xyz__");
        Thread.sleep(600);
        assertTrue(v.getSnapshot().isEmpty());
    }
}