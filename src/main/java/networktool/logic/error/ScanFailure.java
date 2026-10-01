package main.java.networktool.logic.error;

import main.java.networktool.logic.scan.host.ScanErrorClassifier;

/** Immutable, log-safe description of a scan failure. */
public record ScanFailure(
        ScanErrorClassifier.Kind kind,
        String host,
        int port,
        String scanId,
        String operation
) {
    public ScanFailure {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        host = safeValue(host, "[A-Za-z0-9_.:-]{1,255}");
        scanId = safeValue(scanId, "[A-Za-z0-9-]{1,32}");
        operation = safeValue(operation, "[A-Za-z0-9_.-]{1,64}");
    }

    public static ScanFailure from(ScanContext context, Throwable error) {
        ScanContext safeContext = context == null ? ScanContext.unknown() : context;
        return new ScanFailure(
                ScanErrorClassifier.classify(error),
                safeContext.host(),
                safeContext.port(),
                safeContext.scanId(),
                safeContext.operation()
        );
    }

    public String describe() {
        return "[" + kind + "] " + host + ":" + port + " scan=" + scanId + " op=" + operation;
    }

    private static String safeValue(String value, String pattern) {
        return value != null && value.matches(pattern) ? value : "unknown";
    }
}
