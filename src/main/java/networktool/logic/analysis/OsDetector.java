package main.java.networktool.logic.analysis;

/**
 * Öffentliche API für OS-Erkennung.
 * Alle Erkennungslogik liegt in {@link OsDetectionPipeline}.
 */
public final class OsDetector {

    private OsDetector() {}

    public enum Confidence { HOCH, MITTEL, NIEDRIG }

    public static final class OsResult {
        public final String     os;
        public final Confidence confidence;
        public final String     method;

        public OsResult(String os, Confidence c, String method) {
            this.os = os; this.confidence = c; this.method = method;
        }

        public String display() { return os + " [" + confidence.name().charAt(0) + "]"; }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Erkennt OS mit Konfidenz-Angabe. Delegiert an {@link OsDetectionPipeline}. */
    public static OsResult detectWithConfidence(String ip) {
        return OsDetectionPipeline.run(ip);
    }

    /** Gibt nur den OS-String zurück. */
    public static String detect(String ip) {
        return detectWithConfidence(ip).os;
    }

    /** Erkennt OS aus Hostname ohne Netzwerk-IO. */
    public static String detectFromHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip)) return null;
        if (hostname.startsWith("host-"))             return null;
        return OsDetectorHostname.classify(hostname.toLowerCase());
    }

    public static boolean isOpen(String ip, int port) {
        return OsDetectorPorts.isOpen(ip, port);
    }

    public static String getMacFromArp(String ip) {
        return OsDetectorArp.getMacFromArp(ip);
    }

    /** Package-private: für Tests und interne Nutzung. */
    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }
}