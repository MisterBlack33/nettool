package main.java.networktool.logic.error;

import main.java.networktool.logic.scan.host.ScanErrorClassifier;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScanFailureTest {

    @Test
    void contextCreatesShortIdAndNormalizesMissingValues() {
        ScanContext context = ScanContext.create("scan");

        assertEquals(8, context.scanId().length());
        assertEquals("scan", context.operation());
        assertEquals("unknown", new ScanContext(null, null).host());
        assertEquals("unknown", ScanContext.forHost(null).host());
        assertEquals("unknown", ScanContext.forHost("https://user:secret@example.test").host());
        assertEquals("unknown", new ScanContext("safe-id", "token=private")
                .operation());
        assertEquals("unknown", ScanContext.unknown().scanId());
    }

    @Test
    void failureCapturesKindAndFormatsSafeDescription() {
        ScanContext context = new ScanContext("id", "probe", "host", 80);

        ScanFailure failure = ScanFailure.from(context, new SocketTimeoutException("secret"));

        assertEquals(ScanErrorClassifier.Kind.TIMEOUT, failure.kind());
        assertEquals("[TIMEOUT] host:80 scan=id op=probe", failure.describe());
    }

    @Test
    void failureRejectsMissingKind() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScanFailure(null, "host", 80, "id", "probe"));
    }

    @Test
    void failureNormalizesMissingContextAndSupportsNullThrowable() {
        ScanFailure failure = ScanFailure.from(null, null);

        assertEquals(ScanErrorClassifier.Kind.UNKNOWN, failure.kind());
        assertEquals("unknown", failure.host());
        assertEquals("unknown", failure.scanId());
        assertEquals("unknown", failure.operation());
        assertEquals(-1, failure.port());
    }

    @Test
    void constructorNormalizesOptionalText() {
        ScanFailure failure = new ScanFailure(
                ScanErrorClassifier.Kind.UNKNOWN, null, 0, null, null);

        assertEquals("[UNKNOWN] unknown:0 scan=unknown op=unknown", failure.describe());
    }
}
