// test/main/java/networktool/logic/analysis/OsSignatureTest.java
package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class OsSignatureTest {

    @Test void best_nullA_returnsB() {
        OsSignature b = OsSignature.of("Linux", 60, "Port");
        assertSame(b, OsSignature.best(null, b));
    }

    @Test void best_nullB_returnsA() {
        OsSignature a = OsSignature.of("Windows", 80, "SMB");
        assertSame(a, OsSignature.best(a, null));
    }

    @Test void best_higherScoreWins() {
        OsSignature low  = OsSignature.of("Linux",   40, "Port");
        OsSignature high = OsSignature.of("Windows", 90, "SMB");
        assertEquals("Windows", OsSignature.best(low, high).os);
    }

    @Test void best_equalScore_firstWins() {
        OsSignature a = OsSignature.of("Linux",   70, "SSH");
        OsSignature b = OsSignature.of("Windows", 70, "SMB");
        assertEquals("Linux", OsSignature.best(a, b).os);
    }

    @Test void toConfidence_high()   { assertEquals(OsDetector.Confidence.HOCH,    OsSignature.of("x", 80, "m").toConfidence()); }
    @Test void toConfidence_medium() { assertEquals(OsDetector.Confidence.MITTEL,  OsSignature.of("x", 40, "m").toConfidence()); }
    @Test void toConfidence_low()    { assertEquals(OsDetector.Confidence.NIEDRIG, OsSignature.of("x", 39, "m").toConfidence()); }
    @Test void toConfidence_exact80(){ assertEquals(OsDetector.Confidence.HOCH,    OsSignature.of("x", 80, "m").toConfidence()); }
    @Test void toConfidence_exact40(){ assertEquals(OsDetector.Confidence.MITTEL,  OsSignature.of("x", 40, "m").toConfidence()); }
}