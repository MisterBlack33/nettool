package main.java.networktool.logic.analysis;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * OS-Erkennungs-Pipeline. Reihenfolge: kein I/O zuerst, TCP zuletzt.
 *
 *  1. Hostname  – kein Netzwerk-IO
 *  2. MAC/OUI   – nur ARP-Cache
 *  3. Banner    – TCP: SSH/HTTP/SMB/FTP/HTTPS
 *  4. UDP       – NetBIOS/mDNS/SNMP
 *  5. mDNS-Unicast direkt an Host
 *  6. Ports     – teuerster Schritt
 *  7. TTL       – Fallback ohne TCP
 *
 * Die Score-Schwellen für den Früh-Abbruch hängen von {@link ScanDepth} ab:
 * SCHNELL bricht früher ab, GRUENDLICH untersucht mehr Schritte und kann
 * dadurch nie eine niedrigere Konfidenz liefern als STANDARD/SCHNELL.
 */
final class OsDetectionPipeline {

    private OsDetectionPipeline() {}

    private enum Step { HOSTNAME, MAC_OUI, BANNER, UDP, MDNS, PORTS }

    // STANDARD entspricht den bisherigen hartkodierten Werten (Abwärtskompatibilität).
    private static final Map<ScanDepth, Map<Step, Integer>> THRESHOLDS = buildThresholds();

    private static Map<ScanDepth, Map<Step, Integer>> buildThresholds() {
        Map<ScanDepth, Map<Step, Integer>> all = new EnumMap<>(ScanDepth.class);
        all.put(ScanDepth.SCHNELL,    thresholds(60, 55, 70, 65, 60, 50));
        all.put(ScanDepth.STANDARD,   thresholds(75, 65, 85, 80, 75, 60));
        all.put(ScanDepth.GRUENDLICH, thresholds(95, 95, 95, 95, 95, 95));
        return all;
    }

    private static Map<Step, Integer> thresholds(int hostname, int macOui, int banner,
                                                 int udp, int mdns, int ports) {
        Map<Step, Integer> m = new EnumMap<>(Step.class);
        m.put(Step.HOSTNAME, hostname);
        m.put(Step.MAC_OUI,  macOui);
        m.put(Step.BANNER,   banner);
        m.put(Step.UDP,      udp);
        m.put(Step.MDNS,     mdns);
        m.put(Step.PORTS,    ports);
        return m;
    }

    /** Abwärtskompatibler Einstiegspunkt – entspricht {@code run(ip, ScanDepth.STANDARD)}. */
    static OsDetector.OsResult run(String ip) {
        return run(ip, ScanDepth.STANDARD);
    }

    static OsDetector.OsResult run(String ip, ScanDepth depth) {
        try {
            return runSafely(ip, THRESHOLDS.get(depth));
        } catch (Exception e) {
            return new OsDetector.OsResult("Unbekannt", OsDetector.Confidence.NIEDRIG, "Fallback");
        }
    }

    private static OsDetector.OsResult runSafely(String ip, Map<Step, Integer> t) {
        System.out.println("  [OS-Detect] " + ip);
        OsSignature best = null;

        best = merge(best, safe("Hostname", () -> hostname(ip)));
        if (hit(best, t.get(Step.HOSTNAME))) return result(best);

        best = merge(best, safe("MAC/OUI", () -> macOui(ip)));
        if (hit(best, t.get(Step.MAC_OUI))) return result(best);

        best = merge(best, safe("Banner", () -> OsBannerAnalyzer.analyze(ip)));
        if (hit(best, t.get(Step.BANNER))) return result(best);

        best = merge(best, safe("UDP", () -> OsProbeUdp.probe(ip)));
        if (hit(best, t.get(Step.UDP))) return result(best);

        best = merge(best, safe("mDNS", () -> mdnsUnicast(ip)));
        if (hit(best, t.get(Step.MDNS))) return result(best);

        best = merge(best, safe("Ports", () -> OsDetectorPorts.detectWithSignature(ip)));
        if (hit(best, t.get(Step.PORTS))) return result(best);

        best = merge(best, safe("TTL", () -> ttlNoTcp(ip)));
        if (best != null) return result(best);

        // Backup: mind. eine Klassifizierung liefern statt "Unbekannt", falls Host doch reagiert
        best = merge(best, safe("Backup-Reachability", () -> reachabilityBackup(ip)));

        return best != null ? result(best)
                : new OsDetector.OsResult("Unbekannt", OsDetector.Confidence.NIEDRIG, "—");
    }

    private static OsSignature safe(String name, Supplier<OsSignature> s) {
        return OsDetectionStepRunner.safeCall(name, s);
    }

    private static OsSignature reachabilityBackup(String ip) {
        boolean open = OsDetectorPorts.isOpen(ip, 80)  || OsDetectorPorts.isOpen(ip, 443)
                || OsDetectorPorts.isOpen(ip, 22)  || OsDetectorPorts.isOpen(ip, 445);
        return open ? OsSignature.of("Netzwerkgerät (unbestimmt)", 20, "Backup-Reachability") : null;
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