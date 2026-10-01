package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("slow")
class OsDetectionPipelineDepthTest {

    private static final String UNREACHABLE = "192.0.2.1";

    @Test
    void allDepths_unreachable_returnStableResult() {
        for (ScanDepth depth : ScanDepth.values()) {
            OsDetector.OsResult result = OsDetectionPipeline.run(UNREACHABLE, depth);
            assertNotNull(result);
            assertEquals("Unbekannt", result.os, "Depth " + depth);
            assertEquals(OsDetector.Confidence.NIEDRIG, result.confidence, "Depth " + depth);
            assertEquals(result.os, result.display().split(" ")[0], "Depth " + depth);
        }
    }

    @Test
    void standardDepth_matchesLegacyRunMethod() {
        OsDetector.OsResult legacy = OsDetectionPipeline.run(UNREACHABLE);
        OsDetector.OsResult standard = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.STANDARD);
        assertEquals(legacy.os, standard.os);
        assertEquals(legacy.confidence, standard.confidence);
        assertEquals(legacy.method, standard.method);
    }

    @Test
    void gruendlich_neverWorseThanStandard_onSameInput() {
        OsDetector.OsResult standard = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.STANDARD);
        OsDetector.OsResult gruendlich = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.GRUENDLICH);
        assertTrue(confidenceScore(gruendlich.confidence) >= confidenceScore(standard.confidence));
    }

    @Test
    void allDepths_doNotThrow() {
        for (ScanDepth depth : ScanDepth.values()) {
            assertDoesNotThrow(() -> OsDetectionPipeline.run(UNREACHABLE, depth));
        }
    }

    private static int confidenceScore(OsDetector.Confidence c) {
        return switch (c) {
            case NIEDRIG -> 0;
            case MITTEL -> 1;
            case HOCH -> 2;
        };
    }
}