package main.java.networktool.logic.scan.host;

import main.java.networktool.logic.scan.host.NetworkDiscoverySweep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDiscoverySweepTest {

    @Test void discover_emptyList_returnsEmpty() {
        assertTrue(NetworkDiscoverySweep.discover(List.of()).isEmpty());
    }

    @Test void discover_doesNotThrow() {
        assertDoesNotThrow(() -> NetworkDiscoverySweep.discover(List.of("192.0.2.1")));
    }

    @Test void discover_resultSubsetOfCandidates() {
        List<String> candidates = List.of("192.0.2.1", "192.0.2.2");
        Set<String> result = NetworkDiscoverySweep.discover(candidates);
        assertTrue(candidates.containsAll(result));
    }
}