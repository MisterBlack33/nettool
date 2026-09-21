package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RogueDhcpDetectorTest {

    static final Set<String> TRUSTED = Set.of("10.0.0.1");

    SecurityFindingsCollector sink = SecurityFindingsCollector.getInstance();
    AtomicReference<Set<String>> servers = new AtomicReference<>(Set.of());
    RogueDhcpDetector detector =
            new RogueDhcpDetector(servers::get, new DhcpOfferTracker(TRUSTED), sink);

    @BeforeEach void clear() { sink.clear(); }
    @AfterEach  void cleanup() { detector.stop(); sink.clear(); }

    @Test void runOnce_trustedServer_noFinding() {
        servers.set(Set.of("10.0.0.1"));
        detector.runOnce();
        assertTrue(sink.getAll().isEmpty());
    }

    @Test void runOnce_rogueAppears_findingStored() {
        servers.set(Set.of("10.0.0.1", "10.0.0.66"));
        detector.runOnce();
        assertEquals(1, sink.getAll().size());
        assertEquals("10.0.0.66", sink.getAll().get(0).ip());
    }

    @Test void runOnce_rogueRepeated_notDuplicated() {
        servers.set(Set.of("10.0.0.66"));
        detector.runOnce();
        detector.runOnce();
        assertEquals(1, sink.getAll().size());
    }

    @Test void startStop_lifecycle() {
        assertFalse(detector.isActive());
        assertTrue(detector.start(3600, TRUSTED));
        assertTrue(detector.isActive());
        assertFalse(detector.start(3600, TRUSTED));
        assertTrue(detector.stop());
        assertFalse(detector.stop());
    }

    @Test void start_withoutTrustedServers_false() {
        assertFalse(detector.start(3600, Set.of()));
        assertFalse(detector.start(3600, null));
        assertFalse(detector.isActive());
    }

    @Test void start_invalidInterval_false() {
        assertFalse(detector.start(0, TRUSTED));
    }

    @Test void getInstance_isSingleton() {
        assertSame(RogueDhcpDetector.getInstance(), RogueDhcpDetector.getInstance());
    }
}
