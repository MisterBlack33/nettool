package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für OsDetector.detectWithConfidence(String, ScanDepth) — Modul D, GUI-Anbindung. */
class OsDetectorDepthOverloadTest {

    private static final String UNREACHABLE = "192.0.2.1";

    @Test void schnell_unreachable_niedrig() {
        OsDetector.OsResult r = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.SCHNELL);
        assertNotNull(r);
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test void standard_unreachable_niedrig() {
        OsDetector.OsResult r = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.STANDARD);
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test void gruendlich_unreachable_niedrig() {
        OsDetector.OsResult r = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.GRUENDLICH);
        assertNotNull(r);
        assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence);
    }

    @Test void standardOverload_matchesLegacyNoArgOverload() {
        OsDetector.OsResult legacy   = OsDetector.detectWithConfidence(UNREACHABLE);
        OsDetector.OsResult viaDepth = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.STANDARD);
        assertEquals(legacy.os, viaDepth.os);
        assertEquals(legacy.confidence, viaDepth.confidence);
        assertEquals(legacy.method, viaDepth.method);
    }

    @Test void gruendlich_routesThrough_extendedOsDetector() {
        OsDetector.OsResult viaDepth = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.GRUENDLICH);
        OsDetector.OsResult direct   = ExtendedOsDetector.detect(UNREACHABLE);
        assertEquals(direct.os, viaDepth.os);
        assertEquals(direct.confidence, viaDepth.confidence);
    }

    @Test void allDepths_doNotThrow() {
        for (ScanDepth d : ScanDepth.values())
            assertDoesNotThrow(() -> OsDetector.detectWithConfidence(UNREACHABLE, d));
    }

    @Test void result_display_containsOs() {
        OsDetector.OsResult r = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.SCHNELL);
        assertTrue(r.display().contains(r.os));
    }
}