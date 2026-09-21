package main.java.networktool.logic.scan.schedule;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OfflineAliveProbeTest {

    @Test void emptyInput_emptyResult() {
        assertTrue(OfflineAliveProbe.aliveOf(Set.of(), ip -> true).isEmpty());
    }

    @Test void returnsOnlyAliveHosts() {
        Set<String> alive = OfflineAliveProbe.aliveOf(Set.of("a", "b", "c"), ip -> !ip.equals("b"));
        assertEquals(Set.of("a", "c"), alive);
    }

    @Test void throwingPredicate_hostTreatedAsOffline() {
        Set<String> alive = OfflineAliveProbe.aliveOf(Set.of("a", "b"), ip -> {
            if (ip.equals("a")) throw new IllegalStateException("boom");
            return true;
        });
        assertEquals(Set.of("b"), alive);
    }
}
