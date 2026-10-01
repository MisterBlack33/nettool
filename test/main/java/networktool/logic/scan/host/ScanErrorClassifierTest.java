package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
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

    @Test void classify_connectException_isConnectionRefused() {
        assertEquals(ScanErrorClassifier.Kind.CONNECTION_REFUSED,
                ScanErrorClassifier.classify(new ConnectException()));
    }

    @Test void classify_noRouteToHost_isHostOffline() {
        assertEquals(ScanErrorClassifier.Kind.HOST_OFFLINE,
                ScanErrorClassifier.classify(new NoRouteToHostException()));
    }

    @Test void classify_portUnreachable_isHostOffline() {
        assertEquals(ScanErrorClassifier.Kind.HOST_OFFLINE,
                ScanErrorClassifier.classify(new PortUnreachableException()));
    }

    @Test void classify_resetSocketException_isConnectionReset() {
        assertEquals(ScanErrorClassifier.Kind.CONNECTION_RESET,
                ScanErrorClassifier.classify(new SocketException("Connection reset")));
    }

    @Test void classify_socketExceptionWithoutReset_isUnknown() {
        assertEquals(ScanErrorClassifier.Kind.UNKNOWN,
                ScanErrorClassifier.classify(new SocketException("broken pipe")));
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

    @Test void classify_findsKindInCauseChain() {
        assertEquals(ScanErrorClassifier.Kind.TIMEOUT,
                ScanErrorClassifier.classify(new IllegalStateException(
                        "wrapper", new SocketTimeoutException())));
    }

    @Test void classify_doesNotInspectBeyondFiveExceptions() {
        Throwable error = new SocketTimeoutException();
        for (int depth = 0; depth < 5; depth++) {
            error = new IllegalStateException("wrapper", error);
        }
        assertEquals(ScanErrorClassifier.Kind.UNKNOWN, ScanErrorClassifier.classify(error));
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

    @Test void describe_contextIncludesOperationAndPortWithoutMessage() {
        String description = ScanErrorClassifier.describe(
                new main.java.networktool.logic.error.ScanContext(
                        "scan-123", "tcp-probe", "10.0.0.1", 443),
                new RuntimeException("private detail"));
        assertEquals("[UNKNOWN] 10.0.0.1:443 scan=scan-123 op=tcp-probe type=RuntimeException",
                description);
    }

    @Test void describe_nullContextUsesSafeDefaults() {
        assertEquals("[UNKNOWN] unknown:-1 scan=unknown op=unknown type=null",
                ScanErrorClassifier.describe(
                        (main.java.networktool.logic.error.ScanContext) null, null));
    }
}
