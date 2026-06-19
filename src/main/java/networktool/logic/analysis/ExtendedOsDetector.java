package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Erweiterte OS-Erkennungs-Pipeline.
 * Integriert ICMP-Timing, DHCP Option 60, UPnP und mDNS
 * als zusätzliche Erkennungsstufen.
 *
 * Reihenfolge:
 *  1. Banner (SSH/HTTP/HTTPS/FTP/SMB)
 *  2. UDP-Probe (NetBIOS/mDNS/SNMP)
 *  3. DHCP Option 60
 *  4. UPnP (SSDP)
 *  5. mDNS Service Discovery
 *  6. Hostname-Analyse
 *  7. MAC/OUI
 *  8. Port-Kombination
 *  9. ICMP-Timing-Fingerprint
 * 10. TTL-Fingerprint
 */
public final class ExtendedOsDetector {

    private ExtendedOsDetector() {}

    /**
     * Vollständige Erkennung mit allen verfügbaren Methoden.
     * Langsamer als OsDetector.detect(), dafür präziser.
     */
    public static OsDetector.OsResult detect(String ip) {
        OsSignature best = null;

        // 1. Banner
        best = OsSignature.best(best, OsBannerAnalyzer.analyze(ip));
        if (confident(best, 85)) return toResult(best);

        // 2. UDP-Probe
        best = OsSignature.best(best, OsProbeUdp.probe(ip));
        if (confident(best, 80)) return toResult(best);

        // 3. DHCP Option 60
        best = OsSignature.best(best, fromDhcp(ip));
        if (confident(best, 80)) return toResult(best);

        // 4. UPnP
        best = OsSignature.best(best, fromUpnp(ip));
        if (confident(best, 75)) return toResult(best);

        // 5. mDNS Service Discovery
        best = OsSignature.best(best, fromMdns(ip));
        if (confident(best, 75)) return toResult(best);

        // 6. Hostname
        String hostname = resolveHostname(ip);
        if (hostname != null) {
            String fromHn = OsDetectorHostname.classify(hostname.toLowerCase());
            if (fromHn != null)
                best = OsSignature.best(best, OsSignature.of(fromHn, 75, "Hostname"));
        }
        if (confident(best, 75)) return toResult(best);

        // 7. MAC/OUI
        String mac = OsDetectorArp.getMacFromArp(ip);
        if (mac != null) {
            String vendor = OuiDatabase.lookup(mac);
            if (vendor != null)
                best = OsSignature.best(best, OsSignature.of(vendor, 65, "OUI/MAC"));
        }
        if (confident(best, 65)) return toResult(best);

        // 8. Port-Kombination
        best = OsSignature.best(best, OsDetectorPorts.detectWithSignature(ip));
        if (confident(best, 60)) return toResult(best);

        // 9. ICMP-Timing
        best = OsSignature.best(best, IcmpAnalyzer.fingerprintFromTiming(ip));

        // 10. TTL-Fingerprint
        int ttl = OsDetectorArp.getTtl(ip);
        String fingerprint = OsFingerprint.resolve(ip, ttl, mac);
        if (fingerprint != null)
            best = OsSignature.best(best, OsSignature.of(fingerprint, 40, "TTL=" + ttl));

        return best != null
                ? toResult(best)
                : new OsDetector.OsResult("Unbekannt", OsDetector.Confidence.NIEDRIG, "—");
    }

    // ── Adapter-Methoden ──────────────────────────────────────────────────

    private static OsSignature fromDhcp(String ip) {
        DhcpOptionAnalyzer.Result r = DhcpOptionAnalyzer.analyze(ip);
        if (r == null || r.detectedOs() == null) return null;
        return OsSignature.of(r.detectedOs(), 78, "DHCP-Option60");
    }

    private static OsSignature fromUpnp(String ip) {
        List<UpnpDiscovery.Device> devices = UpnpDiscovery.discover();
        return devices.stream()
                .filter(d -> ip.equals(d.ip()))
                .map(UpnpDiscovery.Device::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .map(os -> OsSignature.of(os, 72, "UPnP"))
                .orElse(null);
    }

    private static OsSignature fromMdns(String ip) {
        List<MdnsDiscovery.ServiceRecord> records = MdnsDiscovery.queryHost(ip);
        return records.stream()
                .map(MdnsDiscovery.ServiceRecord::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .map(os -> OsSignature.of(os, 70, "mDNS"))
                .orElse(null);
    }

    private static boolean confident(OsSignature sig, int threshold) {
        return sig != null && sig.score >= threshold;
    }

    private static OsDetector.OsResult toResult(OsSignature sig) {
        return new OsDetector.OsResult(sig.os, sig.toConfidence(), sig.method);
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