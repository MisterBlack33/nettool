package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Schnelle OS-Erkennungs-Pipeline.
 * Reihenfolge: kein I/O zuerst, TCP zuletzt.
 *
 *  1. Hostname  – kein Netzwerk-IO
 *  2. MAC/OUI   – nur ARP-Cache
 *  3. Banner    – TCP: SSH/HTTP/SMB/FTP/HTTPS
 *  4. UDP       – NetBIOS/mDNS/SNMP
 *  5. mDNS-Unicast direkt an Host
 *  6. Ports     – teuerster Schritt
 *  7. TTL       – Fallback ohne TCP
 */
final class OsDetectionPipeline {

    private OsDetectionPipeline() {}

    static OsDetector.OsResult run(String ip) {
        System.out.println("  [OS-Detect] " + ip);
        OsSignature best = null;

        best = merge(best, step("Hostname", hostname(ip)));
        if (hit(best, 75)) return result(best);

        best = merge(best, step("MAC/OUI", macOui(ip)));
        if (hit(best, 65)) return result(best);

        best = merge(best, step("Banner", OsBannerAnalyzer.analyze(ip)));
        if (hit(best, 85)) return result(best);

        best = merge(best, step("UDP", OsProbeUdp.probe(ip)));
        if (hit(best, 80)) return result(best);

        best = merge(best, step("mDNS", mdnsUnicast(ip)));
        if (hit(best, 75)) return result(best);

        best = merge(best, step("Ports", OsDetectorPorts.detectWithSignature(ip)));
        if (hit(best, 60)) return result(best);

        best = merge(best, step("TTL", ttlNoTcp(ip)));

        return best != null ? result(best)
                : new OsDetector.OsResult("Unbekannt", OsDetector.Confidence.NIEDRIG, "—");
    }

    // ── Probe-Methoden ────────────────────────────────────────────────────

    private static OsSignature hostname(String ip) {
        String h = resolveHostname(ip);
        if (h == null) return null;
        String os = OsDetectorHostname.classify(h.toLowerCase());
        return os != null ? OsSignature.of(os, 75, "Hostname") : null;
    }

    private static OsSignature macOui(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac == null) return null;
        String vendor = OuiDatabase.lookup(mac);
        return vendor != null ? OsSignature.of(vendor, 65, "OUI") : null;
    }

    private static OsSignature mdnsUnicast(String ip) {
        List<MdnsDiscovery.ServiceRecord> records = MdnsDiscovery.queryHost(ip);
        return records.stream()
                .map(MdnsDiscovery.ServiceRecord::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .map(os -> OsSignature.of(os, 72, "mDNS-Unicast"))
                .orElse(null);
    }

    private static OsSignature ttlNoTcp(String ip) {
        String mac = OsDetectorArp.getMacFromArp(ip);
        int    ttl = OsDetectorArp.getTtl(ip);
        if (ttl <= 0 && mac == null) return null;
        String fp  = OsFingerprint.resolveNoTcp(ip, ttl, mac);
        return fp != null ? OsSignature.of(fp, 40, "TTL=" + ttl) : null;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static OsSignature step(String name, OsSignature sig) {
        return OsDetectionLogger.tryStep(name, sig);
    }

    private static OsSignature merge(OsSignature a, OsSignature b) {
        return OsSignature.best(a, b);
    }

    private static boolean hit(OsSignature s, int threshold) {
        return s != null && s.score >= threshold;
    }

    private static OsDetector.OsResult result(OsSignature s) {
        return new OsDetector.OsResult(s.os, s.toConfidence(), s.method);
    }

    private static String resolveHostname(String ip) {
        String[] out = {null};
        Thread t = new Thread(() -> {
            try {
                String h = java.net.InetAddress.getByName(ip).getCanonicalHostName();
                if (!h.equals(ip)) out[0] = h;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return out[0];
    }
}