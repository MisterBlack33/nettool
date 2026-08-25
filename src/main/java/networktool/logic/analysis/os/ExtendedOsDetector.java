package main.java.networktool.logic.analysis.os;

import java.util.Objects;

/**
 * Erweiterte OS-Erkennung für IpInspector (Vollanalyse / {@link ScanDepth#GRUENDLICH}).
 * Basis: {@link OsDetectionPipeline} mit GRUENDLICH-Schwellen, ergänzt durch DHCP/UPnP/ICMP-Timing.
 *
 * Nur für inspect() verwenden – zu langsam für Massen-Scans.
 */
public final class ExtendedOsDetector {

    private ExtendedOsDetector() {}

    public static OsDetector.OsResult detect(String ip) {
        try {
            return detectSafely(ip);
        } catch (Exception e) {
            return OsDetectionPipeline.run(ip, ScanDepth.GRUENDLICH); // Basis-Pipeline als Fallback
        }
    }

    private static OsDetector.OsResult detectSafely(String ip) {
        OsDetector.OsResult base = OsDetectionPipeline.run(ip, ScanDepth.GRUENDLICH);
        if (base.confidence == OsDetector.Confidence.HOCH) return base;

        OsSignature best = OsSignature.of(base.os, confidenceToScore(base.confidence), base.method);
        best = OsSignature.best(best, OsDetectionStepRunner.safeCall("DHCP", () -> fromDhcp(ip)));
        if (best.score >= 78) return toResult(best);
        best = OsSignature.best(best, OsDetectionStepRunner.safeCall("UPnP", () -> fromUpnp(ip)));
        if (best.score >= 72) return toResult(best);
        best = OsSignature.best(best, OsDetectionStepRunner.safeCall("ICMP-Timing",
                () -> IcmpAnalyzer.fingerprintFromTiming(ip)));
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