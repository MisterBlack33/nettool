package main.java.networktool.logic.scan.schedule;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OfflineTrackerTest {

    OfflineTracker t = new OfflineTracker();

    @Test void belowThreshold_noAlert() {
        t.register("a", 0);
        assertTrue(t.newlyOffline(99, 100).isEmpty());
    }

    @Test void atThreshold_alertOnce() {
        t.register("a", 0);
        assertEquals(1, t.newlyOffline(100, 100).size());
        assertTrue(t.newlyOffline(200, 100).isEmpty());
    }

    @Test void register_keepsFirstTimestamp() {
        t.register("a", 0);
        t.register("a", 500);
        assertEquals(1000, t.offlineMs("a", 1000));
    }

    @Test void markSeen_resetsAndAllowsNewAlert() {
        t.register("a", 0);
        t.newlyOffline(100, 100);
        t.markSeen("a", 150);
        assertTrue(t.newlyOffline(200, 100).isEmpty());
        assertEquals(1, t.newlyOffline(250, 100).size());
    }

    @Test void offlineMs_unknown_zero() { assertEquals(0, t.offlineMs("x", 999)); }

    @Test void retainOnly_dropsUnknownHosts() {
        t.register("a", 0);
        t.register("b", 0);
        t.retainOnly(Set.of("a"));
        assertEquals(Set.of("a"), Set.copyOf(t.newlyOffline(100, 100)));
    }

    @Test void clear_removesEverything() {
        t.register("a", 0);
        t.clear();
        assertTrue(t.newlyOffline(1000, 1).isEmpty());
    }

    @Test void snapshot_restore_roundtrip() {
        t.register("a", 5);
        OfflineTracker other = new OfflineTracker();
        other.restore(t.snapshot());
        assertEquals(95, other.offlineMs("a", 100));
    }

    @Test void restore_doesNotOverwriteExisting() {
        t.register("a", 50);
        t.restore(java.util.Map.of("a", 0L));
        assertEquals(50, t.offlineMs("a", 100));
    }
}
