package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsPortScanResolverTest {

    @Test void isOpen_unreachable_false() { assertFalse(PsPortScanResolver.isOpen("192.0.2.1", 80)); }
    @Test void isOpen_doesNotThrow() { assertDoesNotThrow(() -> PsPortScanResolver.isOpen("192.0.2.1", 80)); }
    @Test void isOpen_nullIp_false() { assertFalse(PsPortScanResolver.isOpen(null, 80)); }

    @Test void scanPorts_returnsAllKeys() {
        Map<Integer, Boolean> m = PsPortScanResolver.scanPorts("192.0.2.1", List.of(80, 443));
        assertEquals(2, m.size());
        assertTrue(m.containsKey(80));
        assertTrue(m.containsKey(443));
    }

    @Test void scanPorts_emptyList_emptyMap() {
        assertTrue(PsPortScanResolver.scanPorts("192.0.2.1", List.of()).isEmpty());
    }
}