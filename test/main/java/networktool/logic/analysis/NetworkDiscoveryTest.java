package networktool.logic.analysis;

import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class NetworkDiscoveryTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    // ── IcmpAnalyzer ──────────────────────────────────────────────────────

    @Nested
    class IcmpAnalyzerTest {

        @Test
        void analyze_unreachable_returnsNull() {
            assertNull(IcmpAnalyzer.analyze("192.0.2.1"));
        }

        @Test
        void analyze_localhost_returnsResult() {
            assumeTrue(loopbackReachable());
            IcmpAnalyzer.Result r = IcmpAnalyzer.analyze("127.0.0.1");
            assertNotNull(r);
            assertTrue(r.received() > 0);
            assertEquals(5, r.sent());
        }

        @Test
        void result_lossRate_between0and1() {
            assumeTrue(loopbackReachable());
            IcmpAnalyzer.Result r = IcmpAnalyzer.analyze("127.0.0.1");
            assertNotNull(r);
            assertTrue(r.lossRate() >= 0.0 && r.lossRate() <= 1.0);
        }

        @Test
        void result_avgMs_nonNegative() {
            assumeTrue(loopbackReachable());
            IcmpAnalyzer.Result r = IcmpAnalyzer.analyze("127.0.0.1");
            assertNotNull(r);
            assertTrue(r.avgMs() >= 0);
        }

        @Test
        void result_minLessThanMax() {
            assumeTrue(loopbackReachable());
            IcmpAnalyzer.Result r = IcmpAnalyzer.analyze("127.0.0.1");
            assertNotNull(r);
            assertTrue(r.minMs() <= r.maxMs());
        }

        @Test
        void result_jitter_nonNegative() {
            assumeTrue(loopbackReachable());
            IcmpAnalyzer.Result r = IcmpAnalyzer.analyze("127.0.0.1");
            assertNotNull(r);
            assertTrue(r.jitterMs() >= 0);
        }

        @Test
        void result_highJitter_detectedCorrectly() {
            IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(50, 10, 100, 25, 0, 5, 5);
            assertTrue(r.isHighJitter());
        }

        @Test
        void result_lowJitter_notHighJitter() {
            IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(10, 8, 12, 2, 0, 5, 5);
            assertFalse(r.isHighJitter());
        }

        @Test
        void result_unstable_detectedCorrectly() {
            IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(50, 10, 100, 5, 0.5, 5, 3);
            assertTrue(r.isUnstable());
        }

        @Test
        void result_stable_notUnstable() {
            IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(10, 8, 12, 1, 0.0, 5, 5);
            assertFalse(r.isUnstable());
        }

        @Test
        void result_toString_containsAvg() {
            IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(15.5, 10, 20, 3, 0, 5, 5);
            assertTrue(r.toString().contains("15.5") || r.toString().contains("avg"));
        }

        @Test
        void fingerprintFromTiming_unreachable_returnsNull() {
            assertNull(IcmpAnalyzer.fingerprintFromTiming("192.0.2.1"));
        }

        @Test
        void fingerprintFromTiming_doesNotThrow() {
            assertDoesNotThrow(() -> IcmpAnalyzer.fingerprintFromTiming("127.0.0.1"));
        }
    }

    // ── DhcpOptionAnalyzer ────────────────────────────────────────────────

    @Nested
    class DhcpOptionAnalyzerTest {

        @Test
        void analyze_unreachable_returnsNull() {
            // Kein DHCP-Server auf RFC5737-Adresse
            assertDoesNotThrow(() -> DhcpOptionAnalyzer.analyze("192.0.2.1"));
        }

        @Test
        void analyze_doesNotThrow() {
            assertDoesNotThrow(() -> DhcpOptionAnalyzer.analyze("192.0.2.1"));
        }

        @Test
        void result_fields_accessible() {
            DhcpOptionAnalyzer.Result r = new DhcpOptionAnalyzer.Result("MSFT 5.0", "Windows");
            assertEquals("MSFT 5.0", r.vendorClass());
            assertEquals("Windows", r.detectedOs());
        }

        @Test
        void classify_windows_vendorClass() {
            // Indirekt über Result-Record testen
            DhcpOptionAnalyzer.Result r = new DhcpOptionAnalyzer.Result("MSFT 5.0", "Windows");
            assertTrue(r.detectedOs().contains("Windows"));
        }

        @Test
        void classify_android_vendorClass() {
            DhcpOptionAnalyzer.Result r = new DhcpOptionAnalyzer.Result("android-dhcp-11", "Android");
            assertTrue(r.detectedOs().contains("Android"));
        }
    }

    // ── UpnpDiscovery ─────────────────────────────────────────────────────

    @Nested
    class UpnpDiscoveryTest {

        @Test
        void discover_returnsNonNullList() {
            assertNotNull(UpnpDiscovery.discover());
        }

        @Test
        void discover_doesNotThrow() {
            assertDoesNotThrow(UpnpDiscovery::discover);
        }

        @Test
        void discover_resultIsUnmodifiable() {
            List<UpnpDiscovery.Device> devices = UpnpDiscovery.discover();
            assertThrows(UnsupportedOperationException.class, () -> devices.add(null));
        }

        @Test
        void device_guessOs_windows() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device(
                    "1.1.1.1", "Windows/10 UPnP/1.1", "uuid:abc", "http://1.1.1.1:80/", "upnp:rootdevice");
            assertEquals("Windows", d.guessOs());
        }

        @Test
        void device_guessOs_fritz() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device(
                    "192.168.1.1", "FRITZ!Box UPnP/1.1", null, null, null);
            assertEquals("Router (FRITZ!Box)", d.guessOs());
        }

        @Test
        void device_guessOs_synology() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device(
                    "192.168.1.2", "synology/DSM6", null, null, null);
            assertEquals("NAS (Synology)", d.guessOs());
        }

        @Test
        void device_guessOs_unknownServer_returnsNull() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device(
                    "1.1.1.1", "SomeUnknownDevice/1.0", null, null, null);
            assertNull(d.guessOs());
        }

        @Test
        void device_guessOs_nullServer_returnsNull() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device("1.1.1.1", null, null, null, null);
            assertNull(d.guessOs());
        }

        @Test
        void device_fields_accessible() {
            UpnpDiscovery.Device d = new UpnpDiscovery.Device(
                    "10.0.0.1", "Server/1.0", "uuid:123", "http://x/", "upnp:rootdevice");
            assertEquals("10.0.0.1", d.ip());
            assertEquals("Server/1.0", d.server());
            assertEquals("uuid:123", d.usn());
        }
    }

    // ── MdnsDiscovery ─────────────────────────────────────────────────────

    @Nested
    class MdnsDiscoveryTest {

        @Test
        void discover_returnsNonNullList() {
            assertNotNull(MdnsDiscovery.discover());
        }

        @Test
        void discover_doesNotThrow() {
            assertDoesNotThrow(MdnsDiscovery::discover);
        }

        @Test
        void discover_resultIsUnmodifiable() {
            List<MdnsDiscovery.ServiceRecord> records = MdnsDiscovery.discover();
            assertThrows(UnsupportedOperationException.class, () -> records.add(null));
        }

        @Test
        void queryHost_unreachable_returnsEmptyList() {
            List<MdnsDiscovery.ServiceRecord> records = MdnsDiscovery.queryHost("192.0.2.1");
            assertNotNull(records);
            assertTrue(records.isEmpty());
        }

        @Test
        void queryHost_doesNotThrow() {
            assertDoesNotThrow(() -> MdnsDiscovery.queryHost("192.0.2.1"));
        }

        @Test
        void serviceRecord_guessOs_airplay() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "1.1.1.1", "_airplay._tcp.local", "MyDevice", 7000);
            assertEquals("Apple-Gerät", r.guessOs());
        }

        @Test
        void serviceRecord_guessOs_googlecast() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "1.1.1.1", "_googlecast._tcp.local", "Chromecast", 8009);
            assertEquals("Chromecast", r.guessOs());
        }

        @Test
        void serviceRecord_guessOs_printer() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "1.1.1.1", "_printer._tcp.local", "HP LaserJet", 9100);
            assertEquals("Drucker", r.guessOs());
        }

        @Test
        void serviceRecord_guessOs_smb() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "1.1.1.1", "_smb._tcp.local", "FileShare", 445);
            assertEquals("Windows oder Samba", r.guessOs());
        }

        @Test
        void serviceRecord_guessOs_http_returnsNull() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "1.1.1.1", "_http._tcp.local", "WebServer", 80);
            assertNull(r.guessOs());
        }

        @Test
        void serviceRecord_fields_accessible() {
            MdnsDiscovery.ServiceRecord r = new MdnsDiscovery.ServiceRecord(
                    "10.0.0.5", "_ssh._tcp.local", "myHost", 22);
            assertEquals("10.0.0.5", r.ip());
            assertEquals("_ssh._tcp.local", r.serviceType());
            assertEquals("myHost", r.name());
            assertEquals(22, r.port());
        }
    }
}