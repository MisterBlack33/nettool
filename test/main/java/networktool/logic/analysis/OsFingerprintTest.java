package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsFingerprintTest {

    // ── resolveNoTcp ──────────────────────────────────────────────────────

    @Test
    void noTcp_ttl0_noMac_returnsNull() {
        assertNull(OsFingerprint.resolveNoTcp("192.0.2.1", 0, null));
    }

    @Test
    void noTcp_ttl32_returnsRouter() {
        assertEquals("Router / Netzwerkgerät",
                OsFingerprint.resolveNoTcp("192.0.2.1", 32, null));
    }

    @Test
    void noTcp_ttl64_returnsLinuxOrAndroid() {
        assertEquals("Linux/Unix oder Android",
                OsFingerprint.resolveNoTcp("192.0.2.1", 64, null));
    }

    @Test
    void noTcp_ttl128_returnsWindowsOrAndroid() {
        assertEquals("Windows oder Android",
                OsFingerprint.resolveNoTcp("192.0.2.1", 128, null));
    }

    @Test
    void noTcp_ttl255_returnsIosMacos() {
        assertEquals("iOS / macOS",
                OsFingerprint.resolveNoTcp("192.0.2.1", 255, null));
    }

    @Test
    void noTcp_raspberryMac_overridesTtl() {
        String r = OsFingerprint.resolveNoTcp("192.0.2.1", 64, "B8:27:EB");
        assertNotNull(r);
        assertTrue(r.contains("Raspberry"));
    }

    @Test
    void noTcp_appleMac_overridesHighTtl() {
        String r = OsFingerprint.resolveNoTcp("192.0.2.1", 128, "00:03:93");
        assertNotNull(r);
        String lower = r.toLowerCase();
        assertTrue(lower.contains("ios") || lower.contains("macos") || lower.contains("apple"));
    }

    @Test
    void noTcp_isFast() {
        long start = System.currentTimeMillis();
        OsFingerprint.resolveNoTcp("192.0.2.1", 64, null);
        assertTrue(System.currentTimeMillis() - start < 50,
                "resolveNoTcp darf keine TCP-Verbindungen öffnen");
    }

    // ── resolve (mit TCP) ─────────────────────────────────────────────────

    @Test
    void resolve_ttl0_noMac_returnsNull() {
        assertNull(OsFingerprint.resolve("192.0.2.1", 0, null));
    }

    @Test
    void resolve_ttl32_returnsRouter() {
        String r = OsFingerprint.resolve("192.0.2.1", 32, null);
        assertNotNull(r);
        assertTrue(r.contains("Router") || r.contains("Netzwerk"));
    }

    @Test
    void resolve_ttl128_returnsWindowsOrAndroid() {
        String r = OsFingerprint.resolve("192.0.2.1", 128, null);
        assertNotNull(r);
        assertTrue(r.contains("Android") || r.contains("Windows"));
    }

    @Test
    void resolve_ttl255_returnsApple() {
        String r = OsFingerprint.resolve("192.0.2.1", 255, null);
        assertNotNull(r);
        String lower = r.toLowerCase();
        assertTrue(lower.contains("macos") || lower.contains("ios") || lower.contains("apple"));
    }

    @Test
    void resolve_raspberryMac_recognized() {
        String r = OsFingerprint.resolve("192.0.2.1", 64, "B8:27:EB");
        assertNotNull(r);
        assertTrue(r.contains("Raspberry"));
    }
}