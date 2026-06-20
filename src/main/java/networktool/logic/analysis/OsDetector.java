package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Zentrale OS-Erkennungs-Pipeline.
 * Reihenfolge: Banner → UDP → UPnP → Hostname → MAC → Ports → TTL.
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

    public static OsResult detectWithConfidence(String ip) {
        OsSignature result = null;

        result = OsSignature.best(result, OsBannerAnalyzer.analyze(ip));
        if (hasScore(result, 85)) return toResult(result);

        result = OsSignature.best(result, OsProbeUdp.probe(ip));
        if (hasScore(result, 80)) return toResult(result);

        result = OsSignature.best(result, fromUpnp(ip));
        result = OsSignature.best(result, fromHostname(ip));
        if (hasScore(result, 75)) return toResult(result);

        result = OsSignature.best(result, fromMac(ip));
        if (hasScore(result, 65)) return toResult(result);

        result = OsSignature.best(result, OsDetectorPorts.detectWithSignature(ip));
        if (hasScore(result, 80)) return toResult(result);

        result = OsSignature.best(result, fromTtl(ip));
        return result != null ? toResult(result) : unknown();
    }

    public static String detect(String ip) {
        return detectWithConfidence(ip).os;
    }

    public static String detectFromHostname(String hostname, String ip) {
        if (hostname == null || hostname.equals(ip) || hostname.startsWith("host-")) return null;
        return OsDetectorHostname.classify(hostname.toLowerCase());
    }

    public static boolean isOpen(String ip, int port) {
        return OsDetectorPorts.isOpen(ip, port);
    }

    public static String getMacFromArp(String ip) {
        return OsDetectorArp.getMacFromArp(ip);
    }

    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }

    // ── Probe steps ───────────────────────────────────────────────────────

    private static OsSignature fromUpnp(String ip) {
        String osHint = UpnpDiscovery.discover().stream()
                .filter(d -> d.ip().equals(ip))
                .map(UpnpDiscovery.Device::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return osHint != null ? OsSignature.of(osHint, 70, "UPnP") : null;
    }

    private static OsSignature fromHostname(String ip) {
        String hostname = resolveHostname(ip);
        if (hostname == null) return null;
        String os = OsDetectorHostname.classify(hostname.toLowerCase());
        return os != null ? OsSignature.of(os, 75, "Hostname") : null;
    }

    private static OsSignature fromMac(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac == null) return null;
        String vendor = OuiDatabase.lookup(mac);
        return vendor != null ? OsSignature.of(vendor, 65, "OUI/MAC") : null;
    }

    private static OsSignature fromTtl(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        int ttl = OsDetectorArp.getTtl(ip);
        String os = OsFingerprint.resolve(ip, ttl, mac);
        if (os == null) return null;
        String method = ttl > 0 ? "TTL=" + ttl : "MAC";
        return OsSignature.of(os, 40, method);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static boolean hasScore(OsSignature sig, int min) {
        return sig != null && sig.score >= min;
    }

    private static OsResult toResult(OsSignature sig) {
        return new OsResult(sig.os, sig.toConfidence(), sig.method);
    }

    private static OsResult unknown() {
        return new OsResult("Unbekannt", Confidence.NIEDRIG, "—");
    }

    private static String resolveHostname(String ip) {
        String[] result = {null};
        Thread thread = new Thread(() -> {
            try {
                String name = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!name.equals(ip)) result[0] = name;
            } catch (Exception ignored) { /* DNS unavailable */ }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }
}