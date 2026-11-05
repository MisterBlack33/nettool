package main.java.networktool.logic.scan.host;

import main.java.networktool.logic.ScanOutcome;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkScannerInputValidationTest {

    @Test
    void estimatedHostCount_matchesUsableIpv4RangeSize() {
        assertEquals(254, NetworkScanner.estimatedHostCount("192.168.1.0/24"));
        assertEquals(0, NetworkScanner.estimatedHostCount("192.168.1.0/31"));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkScanner.estimatedHostCount("not-a-cidr"));
    }

    @Test
    void scanCIDROutcome_rejectsInvalidAndOversizedTargetsBeforeScanning() {
        ScanOutcome<List<ScanResult>> invalid = NetworkScanner.scanCIDROutcome("bad/target");
        ScanOutcome<List<ScanResult>> oversized = NetworkScanner.scanCIDROutcome("10.0.0.0/8");

        assertInstanceOf(ScanOutcome.Failure.class, invalid);
        assertInstanceOf(ScanOutcome.Failure.class, oversized);
        assertTrue(((ScanOutcome.Failure<List<ScanResult>>) oversized).reason()
                .contains("Maximal " + NetworkScanner.MAX_SCAN_HOSTS));
    }

    @Test
    void scanCIDROutcome_preservesCancellationBeforeStartingWork() {
        Thread.currentThread().interrupt();
        try {
            ScanOutcome<List<ScanResult>> outcome = NetworkScanner.scanCIDROutcome("192.168.1.0/24");
            assertInstanceOf(ScanOutcome.Failure.class, outcome);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void scanCIDROutcome_rejectsInvalidExplicitPortListsBeforeScanning() {
        ScanOutcome<List<ScanResult>> outcome = NetworkScanner.scanCIDROutcome(
                "192.168.1.0/24", List.of(80, 65_536));

        assertInstanceOf(ScanOutcome.Failure.class, outcome);
        assertTrue(((ScanOutcome.Failure<List<ScanResult>>) outcome).reason().contains("Portliste"));
    }
}
