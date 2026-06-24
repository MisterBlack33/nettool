package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * OS-Erkennungs-Pipeline mit stufenweisem Logging.
 * Jede Methode wird einzeln als [OK] oder [FAIL] gemeldet.
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
        System.out.println("  [OS-Detect] " + ip);
        OsSignature best = null;

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("Banner",   OsBannerAnalyzer.analyze(ip)));
        if (isConfident(best, 85)) return toResult(best);

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("UDP-Probe", OsProbeUdp.probe(ip)));
        if (isConfident(best, 80)) return toResult(best);

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("UPnP",     probeUpnp(ip)));
        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("Hostname",  probeHostname(ip)));
        if (isConfident(best, 75)) return toResult(best);

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("MAC/OUI",   probeMac(ip)));
        if (isConfident(best, 65)) return toResult(best);

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("Port-Scan", OsDetectorPorts.detectWithSignature(ip)));
        if (isConfident(best, 80)) return toResult(best);

        best = OsSignature.best(best,
                OsDetectionLogger.tryStep("TTL",       probeTtl(ip)));

        return best != null ? toResult(best)
                : new OsResult("Unbekannt", Confidence.NIEDRIG, "—");
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

    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }

    // ── Probe steps ───────────────────────────────────────────────────────

    private static OsSignature probeUpnp(String ip) {
        List<UpnpDiscovery.Device> devices = UpnpDiscovery.discover();
        String os = devices.stream()
                .filter(d -> d.ip().equals(ip))
                .map(UpnpDiscovery.Device::guessOs)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        return os != null ? OsSignature.of(os, 70, "UPnP") : null;
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
        int ttl    = OsDetectorArp.getTtl(ip);
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
        Thread t = new Thread(() -> {
            try {
                String name = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!name.equals(ip)) result[0] = name;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(600); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result[0];
    }
}