package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OsDetector.classifyHostname (package-private).
 * Must be in package main.java.networktool_v3.logic.analysis.
 */
class OsDetectorPackageTest {

    @Test
    void iphone_detected() {
        assertEquals("iPhone (iOS)", OsDetector.classifyHostname("my-iphone.local"));
    }

    @Test
    void ipad_detected() {
        assertEquals("iPad (iPadOS)", OsDetector.classifyHostname("my-ipad-pro"));
    }

    @Test
    void macbook_detected() {
        // classifyHostname expects lowercase input
        assertEquals("MacBook (macOS)", OsDetector.classifyHostname("johns-macbook-pro"));
    }

    @Test
    void imac_detected() {
        assertEquals("iMac (macOS)", OsDetector.classifyHostname("studio-imac"));
    }

    @Test
    void windows_desktop_detected() {
        assertEquals("Windows", OsDetector.classifyHostname("desktop-ab1234"));
    }

    @Test
    void windows_laptop_detected() {
        assertEquals("Windows", OsDetector.classifyHostname("laptop-xy9876"));
    }

    @Test
    void raspberry_detected() {
        assertEquals("Raspberry Pi (Linux)", OsDetector.classifyHostname("raspberrypi"));
    }

    @Test
    void fritzbox_detected() {
        assertEquals("Router (FRITZ!Box)", OsDetector.classifyHostname("fritz.box"));
    }

    @Test
    void samsung_detected() {
        assertTrue(OsDetector.classifyHostname("galaxy-s23").contains("Samsung"));
    }

    @Test
    void pixel_detected() {
        assertTrue(OsDetector.classifyHostname("pixel-7a").contains("Pixel"));
    }

    @Test
    void xiaomi_detected() {
        assertTrue(OsDetector.classifyHostname("redmi-note12").contains("Xiaomi"));
    }

    @Test
    void synology_detected() {
        // classifyHostname checks for "synology", not "diskstation"
        assertEquals("NAS (Synology)", OsDetector.classifyHostname("synology-ds920"));
    }

    @Test
    void android_generic() {
        assertEquals("Android", OsDetector.classifyHostname("my-android-device"));
    }

    @Test
    void xbox_detected() {
        assertEquals("Xbox", OsDetector.classifyHostname("my-xbox-console"));
    }

    @Test
    void chromecast_detected() {
        assertEquals("Chromecast", OsDetector.classifyHostname("chromecast-ultra"));
    }

    @Test
    void playstation_detected() {
        assertNotNull(OsDetector.classifyHostname("ps5-console"));
    }

    @Test
    void printer_detected() {
        assertNotNull(OsDetector.classifyHostname("hp-laserjet-printer"));
    }

    @Test
    void unknown_returnsNull() {
        assertNull(OsDetector.classifyHostname("xyzzy12345"));
    }

    @Test
    void null_returnsNull() {
        assertNull(OsDetector.classifyHostname(null));
    }
}
