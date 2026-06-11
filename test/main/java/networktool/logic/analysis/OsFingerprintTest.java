package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OsFingerprintTest {

    @Test void ttl0_noMac_returnsNull() {
        assertNull(OsFingerprint.resolve("192.0.2.1", 0, null));
    }

    @Test void ttl32_withoutMac_returnsRouter() {
        String r = OsFingerprint.resolve("192.0.2.1", 32, null);
        assertNotNull(r);
        assertTrue(r.contains("Router") || r.contains("Netzwerk"));
    }

    @Test void ttl128_withoutMac_returnsWindowsOrAndroid() {
        String r = OsFingerprint.resolve("192.0.2.1", 128, null);
        assertNotNull(r);
        assertTrue(r.contains("Android") || r.contains("Windows"));
    }

    @Test void ttl255_withoutMac_returnsApple() {
        String r = OsFingerprint.resolve("192.0.2.1", 255, null);
        assertNotNull(r);
        assertTrue(r.contains("macOS") || r.contains("iOS") || r.contains("Apple"));
    }

    @Test void raspberryMac_recognized() {
        // B8:27:EB = Raspberry Pi OUI
        String r = OsFingerprint.resolve("192.0.2.1", 64, "B8:27:EB");
        assertNotNull(r);
        assertTrue(r.contains("Raspberry"));
    }

    @Test void appleMac_recognized() {
        // 00:03:93 = Apple OUI
        String r = OsFingerprint.resolve("192.0.2.1", 128, "00:03:93");
        assertNotNull(r);
        assertTrue(r.toLowerCase().contains("macos") || r.toLowerCase().contains("ios")
                || r.toLowerCase().contains("apple"));
    }
}