package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Zentrale OS-Erkennungs-Pipeline.
 * Reihenfolge: Banner → UDP-Probe → UPnP → mDNS → Hostname → MAC/OUI → Port-Kombination → TTL.
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
        OsSignature best = runBannerProbe(ip);
        if (isConfident(best, 85)) return toResult(best);

        best = OsSignature.best(best, OsProbeUdp.probe(ip));
        if (isConfident(best, 80)) return toResult(best);

        best = OsSignature.best(best, probeUpnp(ip));
        best = OsSignature.best(best, probeHostname(ip));
        if (isConfident(best, 75)) return toResult(best);

        best = OsSignature.best(best, probeMac(ip));
        if (isConfident(best, 65)) return toResult(best);

        best = OsSignature.best(best, OsDetectorPorts.detectWithSignature(ip));
        if (isConfident(best, 80)) return toResult(best);

        best = OsSignature.best(best, probeTtl(ip));
        return best != null ? toResult(best) : new OsResult("Unbekannt", Confidence.NIEDRIG, "—");
    }

    public static String detect(String ip) {
        return detectWithConfidence(ip).os;
    }

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

    // Package-private — used by tests
    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }

    // ── Private probe steps ───────────────────────────────────────────────

    private static OsSignature runBannerProbe(String ip) {
        return OsBannerAnalyzer.analyze(ip);
    }

    /** UPnP: find the first device matching our IP and extract its OS hint. */
    private static OsSignature probeUpnp(String ip) {
        List<UpnpDiscovery.Device> devices = UpnpDiscovery.discover();
        String osHint = devices.stream()
                .filter(d -> d.ip().equals(ip))
                .map(UpnpDiscovery.Device::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return osHint != null ? OsSignature.of(osHint, 70, "UPnP") : null;
    }

    private static OsSignature probeHostname(String ip) {
        String hostname = resolveHostname(ip);
        if (hostname == null) return null;
        String classified = OsDetectorHostname.classify(hostname.toLowerCase());
        return classified != null ? OsSignature.of(classified, 75, "Hostname") : null;
    }

    private static OsSignature probeMac(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac == null) return null;
        String vendor = OuiDatabase.lookup(mac);
        return vendor != null ? OsSignature.of(vendor, 65, "OUI/MAC") : null;
    }

    private static OsSignature probeTtl(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        int ttl = OsDetectorArp.getTtl(ip);
        String fingerprint = OsFingerprint.resolve(ip, ttl, mac);
        return fingerprint != null
                ? OsSignature.of(fingerprint, 40, ttl > 0 ? "TTL=" + ttl : "MAC")
                : null;
    }

    private static boolean isConfident(OsSignature sig, int threshold) {
        return sig != null && sig.score >= threshold;
    }

    private static OsResult toResult(OsSignature sig) {
        return new OsResult(sig.os, sig.toConfidence(), sig.method);
    }

    private static String resolveHostname(String ip) {
        String[] result = {null};
        Thread thread = new Thread(() -> {
            try {
                String hostname = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!hostname.equals(ip)) result[0] = hostname;
            } catch (Exception ignored) { /* DNS not available */ }
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