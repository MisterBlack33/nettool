package main.java.networktool.logic.analysis;

import java.net.InetAddress;
import java.util.*;

/**
 * Analysiert ICMP-Response-Timing: Jitter, Loss-Rate, Min/Max/Avg.
 * Ergebnis dient als zusätzliches OS-Fingerprinting-Signal.
 */
public final class IcmpAnalyzer {

    private IcmpAnalyzer() {}

    private static final int TIMEOUT_MS   = 1500;
    private static final int PROBE_COUNT  = 5;

    public record Result(
            double avgMs,
            double minMs,
            double maxMs,
            double jitterMs,
            double lossRate,
            int    sent,
            int    received
    ) {
        /** Heuristik: hoher Jitter deutet auf VoIP-Gerät oder schlechte Verbindung hin. */
        public boolean isHighJitter() { return jitterMs > 20.0; }

        /** Verlustrate über 20 % → Verbindung instabil. */
        public boolean isUnstable()   { return lossRate > 0.20; }

        @Override public String toString() {
            return String.format(
                    "ICMP: avg=%.1fms min=%.1f max=%.1f jitter=%.1f loss=%.0f%%",
                    avgMs, minMs, maxMs, jitterMs, lossRate * 100);
        }
    }

    /**
     * Führt PROBE_COUNT Pings durch und gibt Timing-Statistik zurück.
     * Gibt null zurück wenn Host nicht erreichbar.
     */
    public static Result analyze(String ip) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }

        List<Long> times = new ArrayList<>(PROBE_COUNT);
        int lost = 0;

        for (int i = 0; i < PROBE_COUNT; i++) {
            long start = System.currentTimeMillis();
            try {
                boolean alive = addr.isReachable(TIMEOUT_MS);
                long ms = System.currentTimeMillis() - start;
                if (alive) {
                    times.add(ms);
                } else {
                    lost++;
                }
            } catch (Exception e) {
                lost++;
            }
        }

        if (times.isEmpty()) return null;

        double avg    = times.stream().mapToLong(v -> v).average().orElse(0);
        double min    = times.stream().mapToLong(v -> v).min().orElse(0);
        double max    = times.stream().mapToLong(v -> v).max().orElse(0);
        double jitter = computeJitter(times);

        return new Result(avg, min, max, jitter,
                (double) lost / PROBE_COUNT, PROBE_COUNT, times.size());
    }

    /** Mittlere absolute Abweichung aufeinanderfolgender Messungen. */
    private static double computeJitter(List<Long> times) {
        if (times.size() < 2) return 0.0;
        double sum = 0;
        for (int i = 1; i < times.size(); i++) {
            sum += Math.abs(times.get(i) - times.get(i - 1));
        }
        return sum / (times.size() - 1);
    }

    /**
     * Leitet OS-Hinweis aus Timing-Profil ab.
     * Ergänzt TTL-Fingerprinting mit statistischen Merkmalen.
     */
    public static OsSignature fingerprintFromTiming(String ip) {
        Result r = analyze(ip);
        if (r == null) return null;

        // Sehr niedrige Latenz + stabiler Jitter → lokales Gerät
        if (r.avgMs() < 2 && r.jitterMs() < 0.5) {
            return OsSignature.of("Lokales Gerät", 30, "ICMP-Timing");
        }

        // Hoher Jitter → oft IoT oder WLAN-Gerät
        if (r.isHighJitter()) {
            return OsSignature.of("IoT-Gerät (WLAN)", 25, "ICMP-Jitter");
        }

        return null;
    }
}