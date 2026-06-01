package networktool_v3.logic;

import main.java.networktool.logic.scan.*;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class ScanInfraTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Nested
    class HostAliveCheckerTest {

        @Test
        void isAlive_localhost_true() {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assertTrue(HostAliveChecker.isAlive("127.0.0.1"));
        }

        @Test
        void isAlive_unreachable_false() {
            assertFalse(HostAliveChecker.isAlive("192.0.2.1"));
        }
    }

    @Nested
    class SubnetDetectorTest {

        @Test
        void getAllSubnets_doesNotThrow() {
            assertDoesNotThrow(() -> SubnetDetector.getAllSubnets());
        }

        @Test
        void getAllSubnets_returnsListNotNull() throws Exception {
            assertNotNull(SubnetDetector.getAllSubnets());
        }

        @Test
        void getAllSubnets_formattedCorrectly() throws Exception {
            for (String subnet : SubnetDetector.getAllSubnets()) {
                assertTrue(subnet.split("\\.").length >= 2,
                        "Subnet hat zu wenige Oktette: " + subnet);
            }
        }
    }

    @Nested
    class PingSweepTest {

        @Test
        void sweep_emptyList_returnsEmpty() {
            assertTrue(PingSweep.sweep(List.of(), null).isEmpty());
        }

        @Test
        void sweep_localhost_containsIt() {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assertTrue(PingSweep.sweep(List.of("127.0.0.1"), null).contains("127.0.0.1"));
        }

        @Test
        void sweep_unreachable_emptyResult() {
            assertTrue(PingSweep.sweep(List.of("192.0.2.99"), null).isEmpty());
        }

        @Test
        void sweep_withProgressCallback() {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            int[] count = {0};
            PingSweep.sweep(List.of("127.0.0.1"), () -> count[0]++);
            assertEquals(1, count[0]);
        }
    }

    @Nested
    class LastScanCacheTest {

        @BeforeEach
        void clear() { LastScanCache.updateFromHostResults(List.of()); }

        @Test
        void isEmpty_initially() { assertTrue(LastScanCache.isEmpty()); }

        @Test
        void updateFromHostResults_storesData() {
            LastScanCache.updateFromHostResults(
                    List.of(new HostResult("1.1.1.1", "host [AA:BB:CC]", "Linux")));
            assertFalse(LastScanCache.isEmpty());
            assertEquals("1.1.1.1", LastScanCache.getAll().get(0).ip());
        }

        @Test
        void updateFromHostResults_stripsMAC() {
            LastScanCache.updateFromHostResults(
                    List.of(new HostResult("1.1.1.1", "server [AA:BB:CC:DD:EE:FF]", "Linux")));
            assertEquals("server", LastScanCache.getAll().get(0).hostname());
        }

        @Test
        void updateFromScanResults_storesData() {
            LastScanCache.updateFromScanResults(
                    List.of(new ScanResult("2.2.2.2", "srv", new HashMap<>(), "Win")));
            assertFalse(LastScanCache.isEmpty());
            assertEquals("2.2.2.2", LastScanCache.getAll().get(0).ip());
        }

        @Test
        void getAll_notNull() { assertNotNull(LastScanCache.getAll()); }
    }

    @Nested
    class ScanProgressTest {

        @Test
        void constructor_doesNotThrow() { assertDoesNotThrow(() -> new ScanProgress(10)); }

        @Test
        void step_doesNotThrow() {
            ScanProgress sp = new ScanProgress(5);
            for (int i = 0; i < 5; i++) sp.step();
        }
    }

    @Nested
    class NetworkInfoTest {

        @Test
        void scanWithFilter_emptyFilters_doesNotThrow() {
            assertDoesNotThrow(() -> NetworkInfo.scanWithFilter(null, null));
        }
    }

    @Nested
    class PortChangeMonitorTest {

        @AfterEach
        void stop() { PortChangeMonitor.getInstance().stop(); }

        @Test
        void isActive_initiallyFalse()  { assertFalse(PortChangeMonitor.getInstance().isActive()); }

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

    @Nested
    class ScanSchedulerTest {

        ScanScheduler sched = ScanScheduler.getInstance();

        @AfterEach
        void cleanup() { sched.stopAll(); }

        @Test
        void getRunning_initially_empty()        { assertTrue(sched.getRunning().isEmpty()); }
        @Test
        void isRunning_nonexistent_false()       { assertFalse(sched.isRunning("no_such_profile")); }
        @Test
        void stop_nonexistent_doesNotThrow()     { assertDoesNotThrow(() -> sched.stop("ghost")); }
        @Test
        void stopAll_doesNotThrow()              { assertDoesNotThrow(sched::stopAll); }
    }
}