package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class OsDetectionLoggerTest {

    private String capture(Runnable r) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(buf));
        try { r.run(); } finally { System.setOut(orig); }
        return buf.toString();
    }

    @Test
    void tryStep_success_logsOk() {
        OsSignature sig = OsSignature.of("Windows", 80, "SMB");
        String out = capture(() -> OsDetectionLogger.tryStep("Port-Scan", sig));
        assertTrue(out.contains("[OK]"));
        assertTrue(out.contains("Port-Scan"));
        assertTrue(out.contains("Windows"));
    }

    @Test
    void tryStep_null_logsFail() {
        String out = capture(() -> OsDetectionLogger.tryStep("Banner", null));
        assertTrue(out.contains("[FAIL]"));
        assertTrue(out.contains("Banner"));
    }

    @Test
    void tryStep_returnsSignatureUnchanged() {
        OsSignature sig = OsSignature.of("Linux", 60, "SSH");
        assertSame(sig, OsDetectionLogger.tryStep("Test", sig));
    }

    @Test
    void tryStep_null_returnsNull() {
        assertNull(OsDetectionLogger.tryStep("Empty", null));
    }

    @Test
    void tryStep_logsScore() {
        OsSignature sig = OsSignature.of("macOS", 90, "TLS");
        String out = capture(() -> OsDetectionLogger.tryStep("HTTPS", sig));
        assertTrue(out.contains("90"));
    }

    @Test
    void tryStep_logsMethodName() {
        String out = capture(() -> OsDetectionLogger.tryStep("UDP-Probe", null));
        assertTrue(out.contains("UDP-Probe"));
    }
}