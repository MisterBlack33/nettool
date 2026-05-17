package main.java.networktool_v3.logic.scan;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RemoteNetScanner package-private methods.
 * Must be in package main.java.networktool_v3.logic.scan.
 */
class RemoteNetScannerPackageTest {

    @Test
    void normalizeCidr_withSlash() {
        assertEquals("10.0.0.0/16", RemoteNetScanner.normalizeCidr("10.0.0.0/16"));
    }

    @Test
    void normalizeCidr_threeOctets() {
        assertEquals("192.168.1.0/24", RemoteNetScanner.normalizeCidr("192.168.1"));
    }

    @Test
    void normalizeCidr_twoOctets() {
        assertTrue(RemoteNetScanner.normalizeCidr("10.0").contains("/16"));
    }

    @Test
    void normalizeCidr_fourOctets_noSlash() {
        assertTrue(RemoteNetScanner.normalizeCidr("10.0.0.1").contains("/24"));
    }

    @Test
    void normalizeCidr_null() {
        assertNull(RemoteNetScanner.normalizeCidr(null));
    }

    @Test
    void cidrToSubnetPrefixes_slash24_onePrefix() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.1.0/24");
        assertEquals(1, prefixes.size());
        assertEquals("192.168.1", prefixes.get(0));
    }

    @Test
    void cidrToSubnetPrefixes_slash23_twoPrefixes() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.0.0/23");
        assertEquals(2, prefixes.size());
    }

    @Test
    void cidrToSubnetPrefixes_slash16_256prefixes() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("10.0.0.0/16");
        assertEquals(256, prefixes.size());
    }

    @Test
    void cidrToSubnetPrefixes_slash25_onePrefix() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.1.0/25");
        assertEquals(1, prefixes.size());
    }

    @Test
    void cidrToSubnetPrefixes_invalid_empty() {
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("invalid");
        assertTrue(prefixes.isEmpty());
    }
}
