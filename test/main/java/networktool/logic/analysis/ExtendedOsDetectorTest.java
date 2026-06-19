package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.net.InetAddress;

class ExtendedOsDetectorTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Test
    void detect_unreachable_returnsUnbekannt() {
        OsDetector.OsResult r = ExtendedOsDetector.detect("192.0.2.1");
        assertNotNull(r);
        assertNotNull(r.os);
        assertNotNull(r.confidence);
        assertNotNull(r.method);
    }

    @Test
    void detect_doesNotThrow() {
        assertDoesNotThrow(() -> ExtendedOsDetector.detect("192.0.2.1"));
    }

    @Test
    void detect_localhost_returnsResult() {
        assumeTrue(loopbackReachable());
        OsDetector.OsResult r = ExtendedOsDetector.detect("127.0.0.1");
        assertNotNull(r);
        assertFalse(r.os.isBlank());
    }

    @Test
    void detect_result_confidenceNotNull() {
        OsDetector.OsResult r = ExtendedOsDetector.detect("192.0.2.1");
        assertNotNull(r.confidence);
    }

    @Test
    void detect_unreachable_confidenceNiedrig() {
        OsDetector.OsResult r = ExtendedOsDetector.detect("192.0.2.1");
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test
    void detect_result_methodNotNull() {
        OsDetector.OsResult r = ExtendedOsDetector.detect("192.0.2.1");
        assertNotNull(r.method);
    }

    @Test
    void detect_result_display_containsOs() {
        OsDetector.OsResult r = ExtendedOsDetector.detect("192.0.2.1");
        assertTrue(r.display().contains(r.os));
    }
}