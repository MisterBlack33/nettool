package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ArpMonitorLoggingTest {
    @Test
    void scan_arpCacheReadFailure_doesNotThrow() {
        assertDoesNotThrow(() -> ArpMonitor.getInstance().addBaseline("1.1.1.1", "AA:BB:CC:DD:EE:FF"));
    }
}