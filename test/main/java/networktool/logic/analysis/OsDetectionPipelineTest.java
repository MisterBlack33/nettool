package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class OsDetectionPipelineTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Test
    void run_unreachable_returnsUnbekannt() {
        OsDetector.OsResult r = OsDetectionPipeline.run("192.0.2.1");
        assertNotNull(r);
        assertEquals("Unbekannt", r.os);
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test
    void run_doesNotThrow() {
        assertDoesNotThrow(() -> OsDetectionPipeline.run("192.0.2.1"));
    }

    @Test
    void run_localhost_returnsNonBlank() {
        assumeTrue(loopbackReachable());
        OsDetector.OsResult r = OsDetectionPipeline.run("127.0.0.1");
        assertNotNull(r.os);
        assertFalse(r.os.isBlank());
    }

    @Test
    void run_methodNotNull() {
        OsDetector.OsResult r = OsDetectionPipeline.run("192.0.2.1");
        assertNotNull(r.method);
    }

    @Test
    void run_confidenceNotNull() {
        OsDetector.OsResult r = OsDetectionPipeline.run("192.0.2.1");
        assertNotNull(r.confidence);
    }

    @Test
    void run_result_display_containsOs() {
        OsDetector.OsResult r = OsDetectionPipeline.run("192.0.2.1");
        assertTrue(r.display().contains(r.os));
    }

    @Test
    void run_consistentWithOsDetector() {
        // OsDetector.detectWithConfidence muss identisch zu Pipeline sein
        OsDetector.OsResult direct  = OsDetectionPipeline.run("192.0.2.1");
        OsDetector.OsResult via     = OsDetector.detectWithConfidence("192.0.2.1");
        assertEquals(direct.os,         via.os);
        assertEquals(direct.confidence, via.confidence);
        assertEquals(direct.method,     via.method);
    }
}