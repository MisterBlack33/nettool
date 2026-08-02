package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PsNetScanResolverTest {

    @Test void sweep_doesNotThrow() { assertDoesNotThrow(() -> PsNetScanResolver.sweep("192.0.2")); }
    @Test void sweep_notNull() { assertNotNull(PsNetScanResolver.sweep("192.0.2")); }
    @Test void sweep_null_returnsEmpty() { assertTrue(PsNetScanResolver.sweep(null).isEmpty()); }
    @Test void sweep_blank_returnsEmpty() { assertTrue(PsNetScanResolver.sweep("  ").isEmpty()); }

    @Test void sweep_entriesValidFormat() {
        for (String ip : PsNetScanResolver.sweep("192.0.2"))
            assertTrue(ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$"));
    }
}