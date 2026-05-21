package networktool_v3.logic;

import main.java.networktool_v3.logic.scan.*;
import main.java.networktool_v3.model.ScanResult;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanNetworkTest {

    // ══════════════════════════════════════════════════════════════
    //  NetworkScanner (CIDR smoke)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkScannerTest {

        @Test
        void scanCIDR_loopback_doesNotThrow() {
            assertDoesNotThrow(() -> NetworkScanner.scanCIDR("127.0.0.0/30"));
        }

        @Test
        void scanCIDR_returnsListNotNull() {
            List<ScanResult> r = NetworkScanner.scanCIDR("127.0.0.0/30");
            assertNotNull(r);
        }

        @Test
        void scanCIDR_localhostFound() {
            List<ScanResult> r = NetworkScanner.scanCIDR("127.0.0.0/30");
            assertTrue(r.stream().anyMatch(h -> h.getIp().equals("127.0.0.1")));
        }

        @Test
        void scanCIDR_addsToHistory() {
            ScanHistory.getInstance().clear();
            NetworkScanner.scanCIDR("127.0.0.0/30");
            assertTrue(ScanHistory.getInstance().size() > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkHostScanner
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkHostScannerTest {

        @Test
        void scan_loopbackSubnet_findsLocalhost() {
            var r = NetworkHostScanner.scan(List.of("127.0.0"));
            assertNotNull(r);
            assertTrue(r.stream().anyMatch(h -> h.ip.equals("127.0.0.1")));
        }

        @Test
        void readMacFromArp_localhost_doesNotThrow() {
            assertDoesNotThrow(() -> NetworkHostScanner.readMacFromArp("127.0.0.1"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RemoteNetScanner – ReachResult record
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ReachResultTest {

        @Test
        void reachResult_fields() {
            var r = new RemoteNetScanner.ReachResult(true, 3, 42L);
            assertTrue(r.reachable);
            assertEquals(3, r.respondedProbes);
            assertEquals(42L, r.avgMs);
        }

        @Test
        void reachResult_unreachable() {
            var r = new RemoteNetScanner.ReachResult(false, 0, 0L);
            assertFalse(r.reachable);
        }

        @Test
        void detectDefaultGateway_doesNotThrow() {
            assertDoesNotThrow(RemoteNetScanner::detectDefaultGateway);
        }

        @Test
        void parallelProbe_invalidCidr_doesNotThrow() {
            assertDoesNotThrow(() ->
                    RemoteNetScanner.parallelProbe("192.0.2.0/30"));
        }

        @Test
        void printRoutingHints_doesNotThrow() {
            assertDoesNotThrow(() ->
                    RemoteNetScanner.printRoutingHints("192.168.99.0/24"));
        }
    }
}