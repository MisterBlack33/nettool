package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@Tag("slow")
class ExtendedOsDetectorTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Test
    void detect_unreachable_returnsStableResult() {
        OsDetector.OsResult result = ExtendedOsDetector.detect("192.0.2.1");
        assertNotNull(result);
        assertNotNull(result.os);
        assertNotNull(result.confidence);
        assertNotNull(result.method);
        assertEquals(OsDetector.Confidence.NIEDRIG, result.confidence);
        assertTrue(result.display().contains(result.os));
    }

    @Test
    void detect_localhost_returnsResult_whenReachable() {
        assumeTrue(loopbackReachable());
        OsDetector.OsResult result = ExtendedOsDetector.detect("127.0.0.1");
        assertNotNull(result);
        assertFalse(result.os.isBlank());
        assertNotNull(result.method);
    }

    @Test
    void detect_doesNotThrow() {
        assertDoesNotThrow(() -> ExtendedOsDetector.detect("192.0.2.1"));
    }
}