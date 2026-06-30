package main.java.networktool.logic.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Erweiterte OS-Erkennung für IpInspector (Vollanalyse).
 * Basis: {@link OsDetectionPipeline}, ergänzt durch DHCP/UPnP/ICMP-Timing.
 *
 * Nur für inspect() verwenden – zu langsam für Massen-Scans.
 */
public final class ExtendedOsDetector {

    private ExtendedOsDetector() {}

    public static OsDetector.OsResult detect(String ip) {
        // Basis-Pipeline (Hostname→MAC→Banner→UDP→mDNS→Ports→TTL)
        OsDetector.OsResult base = OsDetectionPipeline.run(ip);
        if (base.confidence == OsDetector.Confidence.HOCH) return base;

        // Basis als OsSignature weiterverwenden
        OsSignature best = OsSignature.of(base.os, confidenceToScore(base.confidence), base.method);

        // DHCP Option 60
        best = OsSignature.best(best, fromDhcp(ip));
        if (best.score >= 78) return toResult(best);

        // UPnP/SSDP
        best = OsSignature.best(best, fromUpnp(ip));
        if (best.score >= 72) return toResult(best);

        // ICMP-Timing als letzter Hinweis
        best = OsSignature.best(best, IcmpAnalyzer.fingerprintFromTiming(ip));

        return toResult(best);
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    private static OsSignature fromDhcp(String ip) {
        DhcpOptionAnalyzer.Result r = DhcpOptionAnalyzer.analyze(ip);
        if (r == null || r.detectedOs() == null) return null;
        return OsSignature.of(r.detectedOs(), 78, "DHCP-Option60");
    }

    private static OsSignature fromUpnp(String ip) {
        return UpnpDiscovery.discover().stream()
                .filter(d -> ip.equals(d.ip()))
                .map(UpnpDiscovery.Device::guessOs)
                .filter(Objects::nonNull)
                .findFirst()
                .map(os -> OsSignature.of(os, 72, "UPnP"))
                .orElse(null);
    }

    private static OsDetector.OsResult toResult(OsSignature s) {
        return new OsDetector.OsResult(s.os, s.toConfidence(), s.method);
    }

    private static int confidenceToScore(OsDetector.Confidence c) {
        return switch (c) {
            case HOCH    -> 80;
            case MITTEL  -> 50;
            case NIEDRIG -> 20;
        };
    }
}