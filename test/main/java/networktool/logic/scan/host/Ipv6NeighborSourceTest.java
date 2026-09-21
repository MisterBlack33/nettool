package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Ipv6NeighborSourceTest {

    @Test void parse_linuxFormat() {
        Set<String> result = Ipv6NeighborSource.parse(List.of(
                "2001:db8::1 dev eth0 lladdr 00:11:22:33:44:55 router REACHABLE"));
        assertEquals(Set.of("2001:db8:0:0:0:0:0:1"), result);
    }

    @Test void parse_windowsFormat_plainAddressPerLine() {
        assertEquals(Set.of("2001:db8:0:0:0:0:0:3"), Ipv6NeighborSource.parse(List.of("2001:db8::3")));
    }

    @Test void parse_macOsFormat() {
        assertEquals(Set.of("2001:db8:0:0:0:0:0:4"),
                Ipv6NeighborSource.parse(List.of("2001:db8::4 aa:bb:cc:dd:ee:ff en0 23h59m47s S")));
    }

    @Test void parse_skipsLinkLocal() {
        assertTrue(Ipv6NeighborSource.parse(List.of("fe80::1 dev eth0 lladdr 00:11:22:33:44:55 REACHABLE")).isEmpty());
    }

    @Test void parse_skipsFailedEntries() {
        assertTrue(Ipv6NeighborSource.parse(List.of("2001:db8::2 dev eth0  FAILED")).isEmpty());
    }

    @Test void parse_skipsGarbageAndBlankLines() {
        assertTrue(Ipv6NeighborSource.parse(List.of("Neighbor Linklayer Address", "", "not-an-ip dev x")).isEmpty());
    }

    @Test void parse_collapsesDuplicates() {
        assertEquals(1, Ipv6NeighborSource.parse(List.of("2001:db8::1 dev a REACHABLE", "2001:0db8::1 dev b STALE")).size());
    }

    @Test void read_doesNotThrow() {
        assertNotNull(assertDoesNotThrow(Ipv6NeighborSource::read));
    }
}
