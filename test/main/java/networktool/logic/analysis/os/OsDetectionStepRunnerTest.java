package main.java.networktool.logic.analysis.os;

import main.java.networktool.logic.analysis.os.OsDetectionStepRunner;
import main.java.networktool.logic.analysis.os.OsSignature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OsDetectionStepRunnerTest {

    @Test void safeCall_normalResult_returned() {
        OsSignature sig = OsSignature.of("Linux", 70, "Test");
        assertSame(sig, OsDetectionStepRunner.safeCall("X", () -> sig));
    }

    @Test void safeCall_null_returnsNull() {
        assertNull(OsDetectionStepRunner.safeCall("X", () -> null));
    }

    @Test void safeCall_throwingSupplier_returnsNull() {
        assertNull(OsDetectionStepRunner.safeCall("X", () -> { throw new RuntimeException("boom"); }));
    }

    @Test void safeCall_npe_returnsNull() {
        assertNull(OsDetectionStepRunner.safeCall("X", () -> { throw new NullPointerException(); }));
    }
}