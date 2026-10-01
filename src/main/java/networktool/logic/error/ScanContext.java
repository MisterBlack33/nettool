package main.java.networktool.logic.error;

import java.util.UUID;

/** Non-secret context attached to one scan operation. */
public record ScanContext(String scanId, String operation, String host, int port) {

    public ScanContext(String scanId, String operation) {
        this(scanId, operation, "unknown", -1);
    }

    public static ScanContext create(String operation) {
        return new ScanContext(UUID.randomUUID().toString().substring(0, 8), operation);
    }

    public static ScanContext forHost(String host) {
        return new ScanContext("unknown", "unknown", host == null ? "unknown" : host, -1);
    }

    public static ScanContext unknown() {
        return new ScanContext("unknown", "unknown");
    }

    public ScanContext {
        scanId = safeValue(scanId, "[A-Za-z0-9-]{1,32}");
        operation = safeValue(operation, "[A-Za-z0-9_.-]{1,64}");
        host = safeValue(host, "[A-Za-z0-9_.:-]{1,255}");
    }

    private static String safeValue(String value, String pattern) {
        return value != null && value.matches(pattern) ? value : "unknown";
    }
}
