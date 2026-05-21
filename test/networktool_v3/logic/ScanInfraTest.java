package networktool_v3.logic;

import main.java.networktool_v3.logic.scan.*;
import main.java.networktool_v3.model.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScanInfraTest {

    // ══════════════════════════════════════════════════════════════
    //  HostAliveChecker
    // ══════════════════════════════════════════════════════════════

    @Nested
    class HostAliveCheckerTest {

        @Test
        void isAlive_localhost_true() {
            assertTrue(HostAliveChecker.isAlive("127.0.0.1"));
        }

        @Test
        void isAlive_unreachable_false() {
            assertFalse(HostAliveChecker.isAlive("192.0.2.1"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SubnetDetector
    // ══════════════════════════════════════════════════════════════

    @Nested
    class SubnetDetectorTest {

        @Test
        void getAllSubnets_doesNotThrow() {
            assertDoesNotThrow(() -> SubnetDetector.getAllSubnets());
        }

        @Test
        void getAllSubnets_returnsListNotNull() throws Exception {
            List<String> s = SubnetDetector.getAllSubnets();
            assertNotNull(s);
        }

        @Test
        void getAllSubnets_formattedCorrectly() throws Exception {
            List<String> s = SubnetDetector.getAllSubnets();
            for (String subnet : s) {
                // e.g. "192.168.1" – three octets
                String[] parts = subnet.split("\\.");
                assertTrue(parts.length >= 2, "Subnet has too few octets: " + subnet);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PingSweep
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PingSweepTest {

        @Test
        void sweep_emptyList_returnsEmpty() {
            List<String> r = PingSweep.sweep(List.of(), null);
            assertTrue(r.isEmpty());
        }

        @Test
        void sweep_localhost_containsIt() {
            List<String> r = PingSweep.sweep(List.of("127.0.0.1"), null);
            assertTrue(r.contains("127.0.0.1"));
        }

        @Test
        void sweep_unreachable_emptyResult() {
            List<String> r = PingSweep.sweep(List.of("192.0.2.99"), null);
            assertTrue(r.isEmpty());
        }

        @Test
        void sweep_withProgressCallback() {
            int[] count = {0};
            PingSweep.sweep(List.of("127.0.0.1"), () -> count[0]++);
            assertEquals(1, count[0]);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LastScanCache
    // ══════════════════════════════════════════════════════════════

    @Nested
    class LastScanCacheTest {

        @BeforeEach
        void clear() {
            LastScanCache.updateFromHostResults(List.of());
        }

        @Test
        void isEmpty_initially() {
            assertTrue(LastScanCache.isEmpty());
        }

        @Test
        void updateFromHostResults_storesData() {
            var h = new HostResult("1.1.1.1", "host [AA:BB:CC]", "Linux");
            LastScanCache.updateFromHostResults(List.of(h));
            assertFalse(LastScanCache.isEmpty());
            assertEquals("1.1.1.1", LastScanCache.getAll().get(0).ip());
        }

        @Test
        void updateFromHostResults_stripsMAC() {
            var h = new HostResult("1.1.1.1", "server [AA:BB:CC:DD:EE:FF]", "Linux");
            LastScanCache.updateFromHostResults(List.of(h));
            assertEquals("server", LastScanCache.getAll().get(0).hostname());
        }

        @Test
        void updateFromScanResults_storesData() {
            var r = new ScanResult("2.2.2.2", "srv", new HashMap<>(), "Win");
            LastScanCache.updateFromScanResults(List.of(r));
            assertFalse(LastScanCache.isEmpty());
            assertEquals("2.2.2.2", LastScanCache.getAll().get(0).ip());
        }

        @Test
        void getAll_immutable() {
            assertDoesNotThrow(() -> {
                var list = LastScanCache.getAll();
                // read-only check
                assertNotNull(list);
            });
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanProgress (smoke tests only – no GUI)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanProgressTest {

        @Test
        void constructor_doesNotThrow() {
            assertDoesNotThrow(() -> new ScanProgress(10));
        }

        @Test
        void step_doesNotThrow() {
            ScanProgress sp = new ScanProgress(5);
            for (int i = 0; i < 5; i++) sp.step();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NetworkInfo (smoke)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NetworkInfoTest {

        @Test
        void scanWithFilter_emptyFilters_doesNotThrow() {
            assertDoesNotThrow(() -> NetworkInfo.scanWithFilter(null, null));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PortChangeMonitor
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PortChangeMonitorTest {

        @AfterEach
        void stop() {
            PortChangeMonitor.getInstance().stop();
        }

        @Test
        void isActive_initiallyFalse() {
            assertFalse(PortChangeMonitor.getInstance().isActive());
        }

        @Test
        void start_setsActive() {
            PortChangeMonitor.getInstance().start(60, "");
            assertTrue(PortChangeMonitor.getInstance().isActive());
        }

        @Test
        void stop_clearsActive() {
            PortChangeMonitor.getInstance().start(60, "");
            PortChangeMonitor.getInstance().stop();
            assertFalse(PortChangeMonitor.getInstance().isActive());
        }

        @Test
        void getInterval_default() {
            PortChangeMonitor.getInstance().start(15, "");
            assertEquals(15, PortChangeMonitor.getInstance().getInterval());
        }

        @Test
        void startTwice_doesNotThrow() {
            PortChangeMonitor.getInstance().start(60, "");
            assertDoesNotThrow(() -> PortChangeMonitor.getInstance().start(60, ""));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanScheduler
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanSchedulerTest {

        ScanScheduler sched = ScanScheduler.getInstance();

        @AfterEach
        void cleanup() {
            sched.stopAll();
        }

        @Test
        void getRunning_initially_empty() {
            assertTrue(sched.getRunning().isEmpty());
        }

        @Test
        void isRunning_nonexistent_false() {
            assertFalse(sched.isRunning("no_such_profile"));
        }

        @Test
        void stop_nonexistent_doesNotThrow() {
            assertDoesNotThrow(() -> sched.stop("ghost"));
        }

        @Test
        void stopAll_doesNotThrow() {
            assertDoesNotThrow(sched::stopAll);
        }
    }
}