package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class OsDetectorDelegationTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Test
    void detectWithConfidence_returnsSameAsPipeline() {
        OsDetector.OsResult fromDetector = OsDetector.detectWithConfidence("192.0.2.1");
        OsDetector.OsResult fromPipeline = OsDetectionPipeline.run("192.0.2.1");
        // Beide sollten dasselbe OS und dieselbe Konfidenz liefern
        assertEquals(fromPipeline.os,         fromDetector.os);
        assertEquals(fromPipeline.confidence, fromDetector.confidence);
        assertEquals(fromPipeline.method,     fromDetector.method);
    }

    @Test
    void detect_returnsOsString() {
        String os = OsDetector.detect("192.0.2.1");
        assertNotNull(os);
        assertFalse(os.isBlank());
    }

    @Test
    void detectWithConfidence_unreachable_niedrig() {
        OsDetector.OsResult r = OsDetector.detectWithConfidence("192.0.2.1");
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test
    void detectFromHostname_null_returnsNull() {
        assertNull(OsDetector.detectFromHostname(null, "1.1.1.1"));
    }

    @Test
    void detectFromHostname_sameAsIp_returnsNull() {
        assertNull(OsDetector.detectFromHostname("192.168.1.1", "192.168.1.1"));
    }

    @Test
    void detectFromHostname_hostPrefix_returnsNull() {
        assertNull(OsDetector.detectFromHostname("host-192-168-1-1", "192.168.1.1"));
    }

    @Test
    void detectFromHostname_raspberry_detected() {
        assertEquals("Raspberry Pi (Linux)",
                OsDetector.detectFromHostname("raspberrypi.local", "10.0.0.1"));
    }

    @Test
    void isOpen_closedPort_false() {
        assertFalse(OsDetector.isOpen("127.0.0.1", 19994));
    }

    @Test
    void getMacFromArp_doesNotThrow() {
        assertDoesNotThrow(() -> OsDetector.getMacFromArp("127.0.0.1"));
    }

    @Test
    void osResult_display_containsOs() {
        OsDetector.OsResult r = new OsDetector.OsResult("Linux", OsDetector.Confidence.HOCH, "SSH");
        assertTrue(r.display().contains("Linux"));
    }

    @Test
    void confidence_values_exist() {
        assertNotNull(OsDetector.Confidence.HOCH);
        assertNotNull(OsDetector.Confidence.MITTEL);
        assertNotNull(OsDetector.Confidence.NIEDRIG);
    }

    @Test
    void localhost_detect_doesNotThrow() {
        assumeTrue(loopbackReachable());
        assertDoesNotThrow(() -> OsDetector.detect("127.0.0.1"));
    }
}