package main.java.networktool.logic.analysis.security;

import main.java.networktool.security.AuditLogger;

import java.util.Set;
import java.util.function.Supplier;

/** Erkennt unbekannte DHCP-Server durch periodische Discover-Probes. */
public final class RogueDhcpDetector {

    private static final int PROBE_TIMEOUT_MS = 3_000;

    private static final class Holder {
        static final RogueDhcpDetector INSTANCE = new RogueDhcpDetector(
                () -> DhcpDiscoverProbe.collectServers(PROBE_TIMEOUT_MS),
                new DhcpOfferTracker(), SecurityFindingsCollector.getInstance());
    }
    public static RogueDhcpDetector getInstance() { return Holder.INSTANCE; }

    private final Supplier<Set<String>> probe;
    private final DhcpOfferTracker      tracker;
    private final FindingReporter       reporter;
    private final PeriodicJob           job = new PeriodicJob("RogueDhcpDetector", this::runOnce);

    RogueDhcpDetector(Supplier<Set<String>> probe, DhcpOfferTracker tracker,
                      SecurityFindingsCollector sink) {
        this.probe    = probe;
        this.tracker  = tracker;
        this.reporter = new FindingReporter(sink);
    }

    /** Startet nur mit mindestens einem vertrauten Server, sonst wäre jede Antwort ein Alarm. */
    public boolean start(int intervalSec, Set<String> trustedServers) {
        if (trustedServers == null || trustedServers.isEmpty()) return false;
        tracker.trust(trustedServers);
        if (!job.start(intervalSec)) return false;
        AuditLogger.getInstance().log("DHCP_WATCH_START",
                "interval=" + intervalSec + "s trusted=" + trustedServers.size());
        return true;
    }

    public boolean stop() {
        if (!job.stop()) return false;
        AuditLogger.getInstance().log("DHCP_WATCH_STOP", "");
        return true;
    }

    public boolean isActive() { return job.isActive(); }

    void runOnce() {
        tracker.evaluate(probe.get()).forEach(reporter::report);
    }
}
