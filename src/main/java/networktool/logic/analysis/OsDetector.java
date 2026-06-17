package main.java.networktool.logic.analysis;

/**
 * Zentrale OS-Erkennungs-Pipeline.
 * Reihenfolge: Banner → UDP-Probe → Hostname → MAC/OUI → Port-Kombination → TTL-Fingerprint.
 * UDP-Probe (mDNS/NetBIOS/SNMP) greift auch wenn TCP-Ports durch Firewalls blockiert sind.
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
        OsSignature best = null;

        // 1. Banner (SSH/HTTP/HTTPS/FTP/SMB) — sehr zuverlässig
        best = OsSignature.best(best, OsBannerAnalyzer.analyze(ip));
        if (best != null && best.score >= 85) return toResult(best);

        // 2. UDP-Probing (NetBIOS/mDNS/SNMP) — funktioniert auch hinter Firewalls
        best = OsSignature.best(best, OsProbeUdp.probe(ip));
        if (best != null && best.score >= 80) return toResult(best);

        // 3. Hostname-Analyse — oft zuverlässig und schnell
        String hostname = resolveHostname(ip);
        if (hostname != null) {
            String fromHn = OsDetectorHostname.classify(hostname.toLowerCase());
            if (fromHn != null) {
                best = OsSignature.best(best, OsSignature.of(fromHn, 75, "Hostname"));
                if (best.score >= 75) return toResult(best);
            }
        }

        // 4. MAC/OUI — zuverlässig wenn ARP funktioniert
        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac != null) {
            String vendor = OuiDatabase.lookup(mac);
            if (vendor != null) {
                best = OsSignature.best(best, OsSignature.of(vendor, 65, "OUI/MAC"));
                if (best.score >= 65) return toResult(best);
            }
        }

        // 5. Port-Kombination — kann durch Firewalls blockiert sein
        best = OsSignature.best(best, OsDetectorPorts.detectWithSignature(ip));
        if (best != null && best.score >= 80) return toResult(best);

        // 6. TTL-Fingerprint — Fallback
        int ttl = OsDetectorArp.getTtl(ip);
        String fingerprint = OsFingerprint.resolve(ip, ttl, mac);
        if (fingerprint != null) {
            best = OsSignature.best(best, OsSignature.of(fingerprint, 40, ttl > 0 ? "TTL=" + ttl : "MAC"));
        }

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

    static String classifyHostname(String h) {
        return OsDetectorHostname.classify(h);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static OsResult toResult(OsSignature sig) {
        return new OsResult(sig.os, sig.toConfidence(), sig.method);
    }

    private static String resolveHostname(String ip) {
        String[] result = {null};
        Thread t = new Thread(() -> {
            try {
                String h = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!h.equals(ip)) result[0] = h;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result[0];
    }
}