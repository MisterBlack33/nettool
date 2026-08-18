package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.IcmpAnalyzer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schätzt adaptive Timeouts pro /24-Subnetz aus vergangenen ICMP-Timing-Ergebnissen.
 *
 * Formel: avg + 3×stddev, hart begrenzt auf [defaultMs, 2×defaultMs] damit
 * instabile Netze keine unbegrenzt wachsenden Timeouts erzeugen.
 *
 * Opt-in (deaktiviert per Default): bestehende statische Timeouts bleiben
 * unverändert, solange {@link #setEnabled(boolean)} nicht aufgerufen wird.
 *
 * NICHT für sicherheitskritische Pfade verwenden (ARP-/IP-Spoofing-Erkennung
 * in SecurityMonitor/ArpMonitor) — dort haben feste Timeouts Vorrang vor
 * Performance, da Angreifer variable Timeouts sonst gezielt ausnutzen könnten.
 */
public final class AdaptiveTimeoutEstimator {

    private static final class Holder {
        static final AdaptiveTimeoutEstimator INSTANCE = new AdaptiveTimeoutEstimator();
    }
    public static AdaptiveTimeoutEstimator getInstance() { return Holder.INSTANCE; }

    private static final double STDDEV_MULTIPLIER = 3.0;

    private final Map<String, SubnetStats> statsBySubnet = new ConcurrentHashMap<>();
    private volatile boolean enabled = false;

    private AdaptiveTimeoutEstimator() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled()              { return enabled; }

    /** Erfasst ein ICMP-Timing-Ergebnis für das Subnetz von {@code ip}. */
    public void record(String ip, IcmpAnalyzer.Result result) {
        if (result == null) return;
        record(ip, result.avgMs());
    }

    public void record(String ip, double avgMs) {
        String subnet = subnet24(ip);
        if (subnet == null) return;
        statsBySubnet.computeIfAbsent(subnet, k -> new SubnetStats()).add(avgMs);
    }

    /**
     * Liefert den geschätzten Timeout in ms für die IP, oder {@code defaultMs}
     * wenn deaktiviert, keine oder zu wenig Historie vorhanden ist.
     */
    public int estimateTimeoutMs(String ip, int defaultMs) {
        if (!enabled) return defaultMs;
        SubnetStats stats = lookup(ip);
        if (stats == null || stats.count() < 2) return defaultMs;

        double raw = stats.mean() + STDDEV_MULTIPLIER * stats.stddev();
        int min = defaultMs;
        int max = defaultMs * 2;
        return (int) Math.max(min, Math.min(max, Math.round(raw)));
    }

    /** Setzt die gesamte Historie zurück (Test-Isolation). */
    public void reset() { statsBySubnet.clear(); }

    // ── Intern ────────────────────────────────────────────────────────────

    private SubnetStats lookup(String ip) {
        String subnet = subnet24(ip);
        return subnet != null ? statsBySubnet.get(subnet) : null;
    }

    static String subnet24(String ip) {
        if (ip == null) return null;
        int last = ip.lastIndexOf('.');
        return last > 0 ? ip.substring(0, last) : null;
    }
}