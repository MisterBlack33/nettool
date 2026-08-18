package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsDetectionPipelineDepthTest {

    private static final String UNREACHABLE = "192.0.2.1";

    @Test void allDepths_unreachable_returnUnbekanntNiedrig() {
        for (ScanDepth d : ScanDepth.values()) {
            OsDetector.OsResult r = OsDetectionPipeline.run(UNREACHABLE, d);
            assertEquals("Unbekannt", r.os, "Depth " + d);
            assertEquals(OsDetector.Confidence.NIEDRIG, r.confidence, "Depth " + d);
        }
    }

    @Test void standardDepth_matchesLegacyRunMethod() {
        OsDetector.OsResult legacy   = OsDetectionPipeline.run(UNREACHABLE);
        OsDetector.OsResult standard = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.STANDARD);
        assertEquals(legacy.os, standard.os);
        assertEquals(legacy.confidence, standard.confidence);
        assertEquals(legacy.method, standard.method);
    }

    @Test void gruendlich_neverWorseThanStandard_onSameInput() {
        OsDetector.OsResult standard   = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.STANDARD);
        OsDetector.OsResult gruendlich = OsDetectionPipeline.run(UNREACHABLE, ScanDepth.GRUENDLICH);
        assertTrue(confidenceScore(gruendlich.confidence) >= confidenceScore(standard.confidence));
    }

    @Test void allDepths_doNotThrow() {
        for (ScanDepth d : ScanDepth.values())
            assertDoesNotThrow(() -> OsDetectionPipeline.run(UNREACHABLE, d));
    }

    private static int confidenceScore(OsDetector.Confidence c) {
        return switch (c) {
            case NIEDRIG -> 0;
            case MITTEL  -> 1;
            case HOCH    -> 2;
        };
    }
}