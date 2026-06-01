package networktool_v3.logic;

import main.java.networktool.logic.analysis.WakeOnLan;
import main.java.networktool.logic.scan.ScanDelta;
import main.java.networktool.logic.scan.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.CIDRUtils;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CIDRUtils, RemoteNetScanner helpers, OsDetector (hostname),
 * TracerouteRunner (parser), ScanDelta, ScanHistory, WakeOnLan, ScanProfile.
 */
class LogicTest {

    // ══════════════════════════════════════════════════════════════
    //  CIDRUtils
    // ══════════════════════════════════════════════════════════════

    @Nested
    class CIDRUtilsTest {

        @Test
        void getAllIPs_slash24_returns254() {
            List<String> ips = CIDRUtils.getAllIPs("192.168.1.0/24");
            assertEquals(254, ips.size());
        }

        @Test
        void getAllIPs_firstAndLast() {
            List<String> ips = CIDRUtils.getAllIPs("10.0.0.0/24");
            assertEquals("10.0.0.1", ips.get(0));
            assertEquals("10.0.0.254", ips.get(ips.size() - 1));
        }

        @Test
        void getAllIPs_slash30_returns2() {
            List<String> ips = CIDRUtils.getAllIPs("192.168.0.0/30");
            assertEquals(2, ips.size());
        }

        @Test
        void ipToInt_roundtrip() {
            String ip = "172.16.5.200";
            assertEquals(ip, CIDRUtils.intToIp(CIDRUtils.ipToInt(ip)));
        }

        @Test
        void ipToInt_zero() {
            assertEquals(0, CIDRUtils.ipToInt("0.0.0.0"));
        }

        @Test
        void ipToInt_max() {
            // 255.255.255.255 = -1 as signed int
            assertEquals(-1, CIDRUtils.ipToInt("255.255.255.255"));
        }

        @Test
        void intToIp_loopback() {
            assertEquals("127.0.0.1", CIDRUtils.intToIp(CIDRUtils.ipToInt("127.0.0.1")));
        }

        @Test
        void getAllIPs_doesNotIncludeNetwork() {
            List<String> ips = CIDRUtils.getAllIPs("192.168.1.0/24");
            assertFalse(ips.contains("192.168.1.0"));
            assertFalse(ips.contains("192.168.1.255"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanDelta
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanDeltaTest {

        ScanResult r(String ip, String os, Integer... ports) {
            Map<Integer, String> m = new LinkedHashMap<>();
            for (int p : ports) m.put(p, "open");
            return new ScanResult(ip, "host-" + ip, m, os);
        }

        @Test
        void noChanges_emptyDelta() {
            List<ScanResult> list = List.of(r("1.1.1.1", "Linux", 22));
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(list, list, "A", "B");
            assertTrue(d.isEmpty());
        }

        @Test
        void newHost_detected() {
            List<ScanResult> before = List.of(r("1.1.1.1", "Linux", 22));
            List<ScanResult> after  = List.of(r("1.1.1.1", "Linux", 22), r("2.2.2.2", "Win", 445));
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(before, after, "A", "B");
            assertTrue(d.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.NEU));
        }

        @Test
        void goneHost_detected() {
            List<ScanResult> before = List.of(r("1.1.1.1", "Linux", 22), r("3.3.3.3", "Win"));
            List<ScanResult> after  = List.of(r("1.1.1.1", "Linux", 22));
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(before, after, "A", "B");
            assertTrue(d.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.WEG));
        }

        @Test
        void osChange_detected() {
            ScanResult b = r("1.1.1.1", "Linux");
            ScanResult a = r("1.1.1.1", "Windows");
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(List.of(b), List.of(a), "A", "B");
            assertTrue(d.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.OS_WECHSEL));
        }

        @Test
        void portChange_newPort_detected() {
            ScanResult b = r("1.1.1.1", "Linux", 22);
            ScanResult a = r("1.1.1.1", "Linux", 22, 80);
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(List.of(b), List.of(a), "A", "B");
            assertTrue(d.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.PORT_AENDERUNG));
        }

        @Test
        void portChange_closedPort_detected() {
            ScanResult b = r("1.1.1.1", "Linux", 22, 8080);
            ScanResult a = r("1.1.1.1", "Linux", 22);
            List<ScanDelta.DeltaEntry> d = ScanDelta.compare(List.of(b), List.of(a), "A", "B");
            assertTrue(d.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.PORT_AENDERUNG));
        }

        @Test
        void compareHosts_newHost() {
            List<HostResult> before = List.of(new HostResult("1.1.1.1", "h", "Linux"));
            List<HostResult> after  = List.of(
                    new HostResult("1.1.1.1", "h", "Linux"),
                    new HostResult("2.2.2.2", "h2", "Win"));
            List<ScanDelta.DeltaEntry> d = ScanDelta.compareHosts(before, after, "A", "B");
            assertEquals(1, d.stream().filter(e -> e.type == ScanDelta.ChangeType.NEU).count());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanHistory
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanHistoryTest {

        ScanHistory hist = ScanHistory.getInstance();

        @BeforeEach
        void setup() { hist.clear(); }

        @Test
        void add_and_size() {
            hist.add("scan1", List.of());
            assertEquals(1, hist.size());
        }

        @Test
        void getLast_returnsNewest() {
            hist.add("first", List.of());
            hist.add("second", List.of());
            assertEquals("second", hist.getLast().get().label);
        }

        @Test
        void maxHistory_respected() {
            for (int i = 0; i < ScanHistory.MAX_HISTORY + 5; i++)
                hist.add("scan" + i, List.of());
            assertEquals(ScanHistory.MAX_HISTORY, hist.size());
        }

        @Test
        void get_validIndex() {
            hist.add("only", List.of());
            assertTrue(hist.get(0).isPresent());
        }

        @Test
        void get_invalidIndex_empty() {
            assertFalse(hist.get(99).isPresent());
        }

        @Test
        void clear_removesAll() {
            hist.add("x", List.of());
            hist.clear();
            assertEquals(0, hist.size());
        }

        @Test
        void results_immutable() {
            List<ScanResult> original = new ArrayList<>();
            original.add(new ScanResult("1.1.1.1", "h", new HashMap<>(), "Linux"));
            hist.add("test", original);
            original.add(new ScanResult("2.2.2.2", "h", new HashMap<>(), "Win"));
            // history should still have only 1 result
            assertEquals(1, hist.getLast().get().results.size());
        }

        @Test
        void entry_display_containsLabel() {
            hist.add("my-label", List.of());
            assertTrue(hist.getLast().get().display().contains("my-label"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  WakeOnLan
    // ══════════════════════════════════════════════════════════════

    @Nested
    class WakeOnLanTest {

        @Test
        void deriveBroadcast_slash24() {
            assertEquals("192.168.1.255", WakeOnLan.deriveBroadcast("192.168.1.50", 24));
        }

        @Test
        void deriveBroadcast_slash16() {
            assertEquals("10.0.255.255", WakeOnLan.deriveBroadcast("10.0.5.1", 16));
        }

        @Test
        void deriveBroadcast_slash32() {
            assertEquals("192.168.1.5", WakeOnLan.deriveBroadcast("192.168.1.5", 32));
        }

        @Test
        void deriveBroadcast_invalidIp_fallback() {
            assertEquals("255.255.255.255", WakeOnLan.deriveBroadcast("invalid", 24));
        }

        @Test
        void extractMacFromHostname_valid() {
            String mac = WakeOnLan.extractMacFromHostname("server [AA:BB:CC:DD:EE:FF]");
            assertEquals("AA:BB:CC:DD:EE:FF", mac);
        }

        @Test
        void extractMacFromHostname_dashFormat() {
            String mac = WakeOnLan.extractMacFromHostname("pc [AA-BB-CC-DD-EE-FF]");
            assertEquals("AA-BB-CC-DD-EE-FF", mac);
        }

        @Test
        void extractMacFromHostname_noMac_returnsNull() {
            assertNull(WakeOnLan.extractMacFromHostname("plain-hostname"));
        }

        @Test
        void extractMacFromHostname_null_returnsNull() {
            assertNull(WakeOnLan.extractMacFromHostname(null));
        }
    }
}
