package networktool_v3.logic;

import main.java.networktool_v3.logic.analysis.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisTest {

    // ══════════════════════════════════════════════════════════════
    //  ArpMonitor
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ArpMonitorTest {

        @AfterEach
        void stop() {
            ArpMonitor.getInstance().stop();
        }

        @Test
        void isActive_initiallyFalse() {
            assertFalse(ArpMonitor.getInstance().isActive());
        }

        @Test
        void start_setsActive() {
            ArpMonitor.getInstance().start("");
            assertTrue(ArpMonitor.getInstance().isActive());
        }

        @Test
        void stop_clearsActive() {
            ArpMonitor.getInstance().start("");
            ArpMonitor.getInstance().stop();
            assertFalse(ArpMonitor.getInstance().isActive());
        }

        @Test
        void startTwice_doesNotThrow() {
            ArpMonitor.getInstance().start("");
            assertDoesNotThrow(() -> ArpMonitor.getInstance().start(""));
        }

        @Test
        void addBaseline_doesNotThrow() {
            assertDoesNotThrow(() ->
                    ArpMonitor.getInstance().addBaseline("192.168.1.1", "AA:BB:CC:DD:EE:FF"));
        }

        @Test
        void addBaseline_nullInputs_doesNotThrow() {
            assertDoesNotThrow(() -> ArpMonitor.getInstance().addBaseline(null, null));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  IpInspector (smoke)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class IpInspectorTest {

        @Test
        void quickScan_localhost_doesNotThrow() {
            assertDoesNotThrow(() -> IpInspector.quickScan("127.0.0.1", 1000));
        }

        @Test
        void quickScan_unreachable_doesNotThrow() {
            assertDoesNotThrow(() -> IpInspector.quickScan("192.0.2.1", 500));
        }

        @Test
        void inspect_localhost_doesNotThrow() {
            assertDoesNotThrow(() -> IpInspector.inspect("127.0.0.1"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OsDetector (public API)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class OsDetectorPublicTest {

        @Test
        void detect_localhost_returnsString() {
            String r = OsDetector.detect("127.0.0.1");
            assertNotNull(r);
            assertFalse(r.isBlank());
        }

        @Test
        void detectWithConfidence_localhost_notNull() {
            OsDetector.OsResult r = OsDetector.detectWithConfidence("127.0.0.1");
            assertNotNull(r);
            assertNotNull(r.os);
            assertNotNull(r.confidence);
        }

        @Test
        void detectFromHostname_null_null() {
            assertNull(OsDetector.detectFromHostname(null, "1.1.1.1"));
        }

        @Test
        void detectFromHostname_sameAsIp_null() {
            assertNull(OsDetector.detectFromHostname("192.168.1.1", "192.168.1.1"));
        }

        @Test
        void detectFromHostname_hostPrefix_null() {
            assertNull(OsDetector.detectFromHostname("host-1-2-3-4", "1.2.3.4"));
        }

        @Test
        void getMacFromArp_doesNotThrow() {
            assertDoesNotThrow(() -> OsDetector.getMacFromArp("127.0.0.1"));
        }

        @Test
        void isOpen_closedPort_false() {
            assertFalse(OsDetector.isOpen("127.0.0.1", 19994));
        }

        @Test
        void osResult_display_containsOs() {
            OsDetector.OsResult r = new OsDetector.OsResult("Linux", OsDetector.Confidence.HOCH, "Port");
            assertTrue(r.display().contains("Linux"));
        }

        @Test
        void confidence_values_exist() {
            assertNotNull(OsDetector.Confidence.HOCH);
            assertNotNull(OsDetector.Confidence.MITTEL);
            assertNotNull(OsDetector.Confidence.NIEDRIG);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OuiUpdater
    // ══════════════════════════════════════════════════════════════

    @Nested
    class OuiUpdaterTest {

        @TempDir Path tmp;

        @Test
        void isLoaded_afterInit_true() {
            OuiUpdater.init(tmp);
            // Either loaded from cache or from built-in – no throw
            assertNotNull(OuiUpdater.isLoaded());
        }

        @Test
        void lookup_afterInit_noExceptionOnKnownOui() {
            OuiUpdater.init(tmp);
            // May return null if not in extended DB, but should not throw
            assertDoesNotThrow(() -> OuiUpdater.lookup("B8:27:EB"));
        }

        @Test
        void extendedCount_nonNegative() {
            assertTrue(OuiUpdater.extendedCount() >= 0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PingUtil (smoke)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class PingUtilTest {

        @Test
        void pingWithDetails_localhost_doesNotThrow() {
            assertDoesNotThrow(() -> PingUtil.pingWithDetails("127.0.0.1", 500));
        }

        @Test
        void pingWithDetails_unreachable_doesNotThrow() {
            assertDoesNotThrow(() -> PingUtil.pingWithDetails("192.0.2.1", 300));
        }
    }
}