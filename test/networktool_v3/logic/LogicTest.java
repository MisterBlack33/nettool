package networktool_v3.logic;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.logic.analysis.OuiDatabase;
import main.java.networktool_v3.logic.analysis.TracerouteRunner;
import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.util.CIDRUtils;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ── CIDRUtils ────────────────────────────────────────────────────────────────

class CIDRUtilsTest {

    @Test
    void slash24HasCorrectCount() {
        List<String> ips = CIDRUtils.getAllIPs("192.168.1.0/24");
        assertEquals(254, ips.size());
    }

    @Test
    void slash24FirstAndLastIp() {
        List<String> ips = CIDRUtils.getAllIPs("192.168.1.0/24");
        assertEquals("192.168.1.1",   ips.get(0));
        assertEquals("192.168.1.254", ips.get(ips.size() - 1));
    }

    @Test
    void slash30HasTwoHosts() {
        List<String> ips = CIDRUtils.getAllIPs("10.0.0.0/30");
        assertEquals(2, ips.size());
        assertEquals("10.0.0.1", ips.get(0));
        assertEquals("10.0.0.2", ips.get(1));
    }

    @Test
    void slash32HasNoHosts() {
        List<String> ips = CIDRUtils.getAllIPs("10.0.0.1/32");
        assertTrue(ips.isEmpty());
    }

    @Test
    void slash16HasCorrectCount() {
        List<String> ips = CIDRUtils.getAllIPs("10.0.0.0/16");
        assertEquals(65534, ips.size());
    }

    @Test
    void noBroadcastInList() {
        List<String> ips = CIDRUtils.getAllIPs("192.168.1.0/24");
        assertFalse(ips.contains("192.168.1.0"));
        assertFalse(ips.contains("192.168.1.255"));
    }
}

// ── OuiDatabase ──────────────────────────────────────────────────────────────

class OuiDatabaseTest {

    @Test
    void lookupAppleOui() {
        String vendor = OuiDatabase.lookup("00:03:93");
        assertNotNull(vendor);
        assertTrue(vendor.toLowerCase().contains("apple") || vendor.contains("iOS"));
    }

    @Test
    void lookupSamsungOui() {
        String vendor = OuiDatabase.lookup("00:12:47");
        assertNotNull(vendor);
        assertTrue(vendor.toLowerCase().contains("samsung"));
    }

    @Test
    void lookupUnknownReturnsNull() {
        assertNull(OuiDatabase.lookup("FF:FF:FF"));
    }

    @Test
    void lookupNullReturnsNull() {
        assertNull(OuiDatabase.lookup(null));
    }

    @Test
    void lookupCaseInsensitive() {
        String lower = OuiDatabase.lookup("00:03:93");
        String upper = OuiDatabase.lookup("00:03:93".toUpperCase());
        assertEquals(lower, upper);
    }

    @Test
    void lookupRaspberryPi() {
        String vendor = OuiDatabase.lookup("B8:27:EB");
        assertNotNull(vendor);
        assertTrue(vendor.toLowerCase().contains("raspberry"));
    }
}

// ── OsDetector hostname classifier ──────────────────────────────────────────

class OsDetectorHostnameTest {

    @Test
    void detectIphone() {
        String os = OsDetector.classifyHostname("iphone-von-max.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("iphone") || os.toLowerCase().contains("ios"));
    }

    @Test
    void detectWindowsDesktop() {
        String os = OsDetector.classifyHostname("desktop-ab1234.example.com");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("windows"));
    }

    @Test
    void detectFritzbox() {
        String os = OsDetector.classifyHostname("fritz.box");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("fritz") || os.toLowerCase().contains("router"));
    }

    @Test
    void detectRaspberryPi() {
        String os = OsDetector.classifyHostname("raspberrypi.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("raspberry"));
    }

    @Test
    void detectSamsungAndroid() {
        String os = OsDetector.classifyHostname("samsung-galaxy-s24.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("samsung") || os.toLowerCase().contains("android"));
    }

    @Test
    void detectMacBook() {
        String os = OsDetector.classifyHostname("macbook-pro-max.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("mac"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(OsDetector.classifyHostname(null));
    }
}

// ── RemoteNetScanner normalize ───────────────────────────────────────────────

class RemoteNetScannerTest {

    @Test
    void normalizeThreeOctets() {
        assertEquals("10.16.5.0/24", RemoteNetScanner.normalizeCidr("10.16.5"));
    }

    @Test
    void normalizeFourOctets() {
        assertEquals("10.16.5.1/24", RemoteNetScanner.normalizeCidr("10.16.5.1"));
    }

    @Test
    void normalizeTwoOctets() {
        assertEquals("10.16.0.0/16", RemoteNetScanner.normalizeCidr("10.16"));
    }

    @Test
    void normalizeAlreadyCidr() {
        assertEquals("10.0.0.0/8", RemoteNetScanner.normalizeCidr("10.0.0.0/8"));
    }

    @Test
    void normalizeNull() {
        assertNull(RemoteNetScanner.normalizeCidr(null));
    }

    @Test
    void cidrToSubnetPrefixes_slash24() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.1.0/24");
        assertEquals(1, prefixes.size());
        assertEquals("192.168.1", prefixes.get(0));
    }

    @Test
    void cidrToSubnetPrefixes_slash16() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.0.0/16");
        assertEquals(256, prefixes.size());
        assertEquals("192.168.0", prefixes.get(0));
        assertEquals("192.168.255", prefixes.get(255));
    }
}

// ── TracerouteRunner parser ───────────────────────────────────────────────────

class TracerouteRunnerTest {

    @Test
    void parseWindowsHopLine() {
        String line = "  1    <1 ms    <1 ms    <1 ms  192.168.1.1";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertEquals(1, hop.number);
        assertEquals("192.168.1.1", hop.ip);
        assertFalse(hop.timeout);
        assertFalse(hop.msValues.isEmpty());
    }

    @Test
    void parseWindowsTimeoutLine() {
        String line = "  3     *        *        *     Request timed out.";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertTrue(hop.timeout);
    }

    @Test
    void parseLinuxHopLine() {
        String line = " 2  _gateway (192.168.0.1)  0.512 ms  0.488 ms  0.501 ms";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, false);
        assertNotNull(hop);
        assertEquals(2, hop.number);
        assertEquals("192.168.0.1", hop.ip);
        assertFalse(hop.timeout);
        assertFalse(hop.msValues.isEmpty());
    }

    @Test
    void parseEmptyLineReturnsNull() {
        assertNull(TracerouteRunner.parseLine("", true));
        assertNull(TracerouteRunner.parseLine("  ", false));
    }

    @Test
    void latencyFormattedSingleValue() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        hop.msValues.add(42L);
        assertEquals("42 ms", hop.latencyFormatted());
    }

    @Test
    void latencyFormattedMultipleValues() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        hop.msValues.addAll(List.of(10L, 20L, 30L));
        String fmt = hop.latencyFormatted();
        assertTrue(fmt.contains("10"));
        assertTrue(fmt.contains("30"));
    }

    @Test
    void latencyFormattedTimeout() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        hop.timeout = true;
        assertEquals("–", hop.latencyFormatted());
    }
}
