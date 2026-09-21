package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DhcpOfferTrackerTest {

    DhcpOfferTracker tracker = new DhcpOfferTracker(Set.of("10.0.0.1"));

    @Test void empty_noFindings() {
        assertTrue(tracker.evaluate(Set.of()).isEmpty());
    }

    @Test void trustedServer_noFinding() {
        assertTrue(tracker.evaluate(Set.of("10.0.0.1")).isEmpty());
    }

    @Test void unknownServer_flagged() {
        List<SecurityFinding> f = tracker.evaluate(Set.of("10.0.0.1", "10.0.0.66"));
        assertEquals(1, f.size());
        assertEquals("10.0.0.66", f.get(0).ip());
        assertEquals(SecurityFinding.Category.ROGUE_DHCP, f.get(0).category());
        assertEquals(SecurityFinding.Severity.CRITICAL, f.get(0).severity());
    }

    @Test void sameRogue_reportedOnce() {
        tracker.evaluate(Set.of("10.0.0.66"));
        assertTrue(tracker.evaluate(Set.of("10.0.0.66")).isEmpty());
    }

    @Test void noTrusted_firstResponderIsNotAutoTrusted() {
        assertEquals(1, new DhcpOfferTracker().evaluate(Set.of("10.0.0.66")).size());
    }

    @Test void trust_addsServers() {
        tracker.trust(Set.of("10.0.0.2"));
        assertTrue(tracker.evaluate(Set.of("10.0.0.2")).isEmpty());
    }
}
