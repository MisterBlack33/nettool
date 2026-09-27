package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class ScanErrorClassifierTest {

    @Test void classify_socketTimeout_isTimeout() {
        assertEquals(ScanErrorClassifier.Kind.TIMEOUT,
                ScanErrorClassifier.classify(new SocketTimeoutException()));
    }

    @Test void classify_interruptedIo_isTimeout() {
        assertEquals(ScanErrorClassifier.Kind.TIMEOUT,
                ScanErrorClassifier.classify(new InterruptedIOException()));
    }

    @Test void classify_unknownHost_isDns() {
        assertEquals(ScanErrorClassifier.Kind.DNS,
                ScanErrorClassifier.classify(new UnknownHostException("x")));
    }

    @Test void classify_connectException_isUnreachable() {
        assertEquals(ScanErrorClassifier.Kind.UNREACHABLE,
                ScanErrorClassifier.classify(new ConnectException()));
    }

    @Test void classify_accessDenied_isPermission() {
        assertEquals(ScanErrorClassifier.Kind.PERMISSION,
                ScanErrorClassifier.classify(new AccessDeniedException("x")));
    }

    @Test void classify_securityException_isPermission() {
        assertEquals(ScanErrorClassifier.Kind.PERMISSION,
                ScanErrorClassifier.classify(new SecurityException()));
    }

    @Test void classify_otherException_isUnknown() {
        assertEquals(ScanErrorClassifier.Kind.UNKNOWN,
                ScanErrorClassifier.classify(new IllegalStateException()));
    }

    @Test void classify_null_isUnknown() {
        assertEquals(ScanErrorClassifier.Kind.UNKNOWN, ScanErrorClassifier.classify(null));
    }

    @Test void describe_containsKindHostAndExceptionType() {
        String s = ScanErrorClassifier.describe("10.0.0.1", new SocketTimeoutException());
        assertTrue(s.contains("TIMEOUT"));
        assertTrue(s.contains("10.0.0.1"));
        assertTrue(s.contains("SocketTimeoutException"));
    }

    @Test void describe_nullException_doesNotThrow() {
        assertDoesNotThrow(() -> ScanErrorClassifier.describe("1.1.1.1", null));
    }

    @Test void describe_neverContainsMessageText() {
        String s = ScanErrorClassifier.describe("1.1.1.1", new RuntimeException("secret-detail"));
        assertFalse(s.contains("secret-detail"));
    }
}
