package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArpNeighborSourceTest {

    private static final String HEADER =
            "IP address       HW type     Flags       HW address            Mask     Device";

    @Test void parse_validEntries() {
        Map<String, String> result = ArpNeighborSource.parseProcNetArp(List.of(HEADER,
                "192.168.1.1      0x1         0x2         aa:bb:cc:dd:ee:01     *        eth0",
                "192.168.1.7      0x1         0x2         aa:bb:cc:dd:ee:02     *        eth0"));
        assertEquals(2, result.size());
        assertEquals("AA:BB:CC:DD:EE:01", result.get("192.168.1.1"));
    }

    @Test void parse_skipsHeader() {
        assertTrue(ArpNeighborSource.parseProcNetArp(List.of(HEADER)).isEmpty());
    }

    @Test void parse_skipsIncompleteEntries() {
        assertTrue(ArpNeighborSource.parseProcNetArp(List.of(HEADER,
                "192.168.1.9      0x1         0x0         aa:bb:cc:dd:ee:03     *        eth0")).isEmpty());
    }

    @Test void parse_skipsZeroMac() {
        assertTrue(ArpNeighborSource.parseProcNetArp(List.of(HEADER,
                "192.168.1.9      0x1         0x2         00:00:00:00:00:00     *        eth0")).isEmpty());
    }

    @Test void parse_skipsShortLines() {
        assertTrue(ArpNeighborSource.parseProcNetArp(List.of(HEADER, "garbage", "")).isEmpty());
    }

    @Test void parse_emptyInput() {
        assertTrue(ArpNeighborSource.parseProcNetArp(List.of()).isEmpty());
    }

    @Test void read_doesNotThrow() {
        assertNotNull(assertDoesNotThrow(ArpNeighborSource::read));
    }
}
