package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("slow")
/** Tests für OsDetector.detectWithConfidence(String, ScanDepth) — Modul D, GUI-Anbindung. */
class OsDetectorDepthOverloadTest {

    private static final String UNREACHABLE = "192.0.2.1";

    @Test
    void allDepths_unreachable_returnStableResult() {
        for (ScanDepth depth : ScanDepth.values()) {
            OsDetector.OsResult result = OsDetector.detectWithConfidence(UNREACHABLE, depth);
            assertNotNull(result);
            assertEquals(OsDetector.Confidence.NIEDRIG, result.confidence, "Depth " + depth);
            assertTrue(result.display().contains(result.os), "Depth " + depth);
        }
    }

    @Test
    void standardDepth_matchesLegacyNoArgOverload() {
        OsDetector.OsResult legacy = OsDetector.detectWithConfidence(UNREACHABLE);
        OsDetector.OsResult viaDepth = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.STANDARD);
        assertEquals(legacy.os, viaDepth.os);
        assertEquals(legacy.confidence, viaDepth.confidence);
        assertEquals(legacy.method, viaDepth.method);
    }

    @Test
    void gruendlich_routesThrough_extendedOsDetector() {
        OsDetector.OsResult viaDepth = OsDetector.detectWithConfidence(UNREACHABLE, ScanDepth.GRUENDLICH);
        OsDetector.OsResult direct = ExtendedOsDetector.detect(UNREACHABLE);
        assertEquals(direct.os, viaDepth.os);
        assertEquals(direct.confidence, viaDepth.confidence);
    }

    @Test
    void allDepths_doNotThrow() {
        for (ScanDepth depth : ScanDepth.values()) {
            assertDoesNotThrow(() -> OsDetector.detectWithConfidence(UNREACHABLE, depth));
        }
    }
}