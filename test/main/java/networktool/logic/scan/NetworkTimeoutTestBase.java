package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.OsDetectorPorts;
import main.java.networktool.logic.ports.PortScanner;
import org.junit.jupiter.api.*;

public abstract class NetworkTimeoutTestBase {

    /** Hoch genug, dass Tests praktisch nie auf den Rate-Limiter warten. */
    private static final double TEST_RATE_PPS = 10_000;
    private static final double TEST_BURST    = 10_000;

    @BeforeAll static void shrinkTimeouts() {
        HostAliveChecker.setTestTimeouts(150, 100);
        HostAliveChecker.setRateLimit(TEST_RATE_PPS, TEST_BURST);
        PortScanner.setRateLimit(TEST_RATE_PPS, TEST_BURST);
        OsDetectorPorts.setTestTimeout(100);
        PortScanner.TIMEOUT_LAN  = 200;
        PortScanner.TIMEOUT_FAST = 100;
    }
}