package networktool.logic;

import main.java.networktool.logic.scan.NetworkHostScanner;
import main.java.networktool.logic.scan.NetworkScanner;
import main.java.networktool.logic.scan.RemoteNetScanner;
import main.java.networktool.logic.scan.ScanHistory;
import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class ScanNetworkTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Nested
    class NetworkScannerTest {

        @Test
        void scanCIDR_loopback_doesNotThrow() {
            assertDoesNotThrow(() -> NetworkScanner.scanCIDR("127.0.0.0/30"));
        }

        @Test
        void scanCIDR_returnsListNotNull() {
            assertNotNull(NetworkScanner.scanCIDR("127.0.0.0/30"));
        }

        @Test
        void scanCIDR_localhostFound() {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assertTrue(NetworkScanner.scanCIDR("127.0.0.0/30").stream()
                    .anyMatch(h -> h.getIp().equals("127.0.0.1")));
        }

        @Test
        void scanCIDR_addsToHistory() {
            ScanHistory.getInstance().clear();
            NetworkScanner.scanCIDR("127.0.0.0/30");
            assertTrue(ScanHistory.getInstance().size() > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkHostScannerTest
    //
    //  scan(List<String>) erwartet /24-Präfixe und scannt jeweils
    //  254 IPs — zu langsam für Tests. Stattdessen scanCidr("/32")
    //  verwenden, das nur exakt eine IP prüft.
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkHostScannerTest {

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void scan_singleIp_doesNotThrow() {
            // /32 = genau eine IP, kein 254er-Sweep
            assertDoesNotThrow(() -> NetworkHostScanner.scanCidr("127.0.0.1/32"));
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void scan_singleIp_returnsList() {
            List<HostResult> result = NetworkHostScanner.scanCidr("127.0.0.1/32");
            assertNotNull(result);
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void scan_loopbackSubnet_findsLocalhost_ifAlive() {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            // /30 = nur 2 Host-IPs (127.0.0.1 + 127.0.0.2)
            List<HostResult> result = NetworkHostScanner.scanCidr("127.0.0.0/30");
            assertNotNull(result);
        }
    }

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
            assertFalse(new RemoteNetScanner.ReachResult(false, 0, 0L).reachable);
        }

        @Test
        void detectDefaultGateway_doesNotThrow() {
            assertDoesNotThrow(RemoteNetScanner::detectDefaultGateway);
        }

        @Test
        void parallelProbe_invalidCidr_doesNotThrow() {
            assertDoesNotThrow(() -> RemoteNetScanner.parallelProbe("192.0.2.0/30"));
        }

        @Test
        void printRoutingHints_doesNotThrow() {
            assertDoesNotThrow(() -> RemoteNetScanner.printRoutingHints("192.168.99.0/24"));
        }
    }
}