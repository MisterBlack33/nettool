package main.java.networktool.logic.analysis.security;

import main.java.networktool.security.AuditLogger;
import main.java.networktool.storage.network.NetworkStore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Prüft TLS-Zertifikate gespeicherter HTTPS-Hosts periodisch (nur WARN/CRITICAL werden gemeldet). */
public final class TlsCertScheduler {

    private static final int[] TLS_PORTS = {443, 8443};
    private static final String SELF_SIGNED_PREFIX = "Self-signed";
    private static final String EXPIRED_DETAIL     = "Zertifikat abgelaufen";
    private static final String EXPIRING_DETAIL    = "Zertifikat läuft in Kürze ab";

    private static final class Holder {
        static final TlsCertScheduler INSTANCE = new TlsCertScheduler(
                TlsCertScheduler::savedTlsHosts, TlsCertInspector::inspect,
                SecurityFindingsCollector.getInstance());
    }
    public static TlsCertScheduler getInstance() { return Holder.INSTANCE; }

    private final Supplier<List<String>> targets;
    private final Function<String, Optional<SecurityFinding>> inspector;
    private final FindingReporter reporter;
    private final PeriodicJob job = new PeriodicJob("TlsCertScheduler", this::runOnce);

    TlsCertScheduler(Supplier<List<String>> targets,
                     Function<String, Optional<SecurityFinding>> inspector,
                     SecurityFindingsCollector sink) {
        this.targets   = targets;
        this.inspector = inspector;
        this.reporter  = new FindingReporter(sink);
    }

    public boolean start(int intervalSec) {
        if (!job.start(intervalSec)) return false;
        AuditLogger.getInstance().log("TLS_SCHEDULER_START", "interval=" + intervalSec + "s");
        return true;
    }

    public boolean stop() {
        if (!job.stop()) return false;
        AuditLogger.getInstance().log("TLS_SCHEDULER_STOP", "");
        return true;
    }

    public boolean isActive() { return job.isActive(); }

    void runOnce() {
        for (String ip : targets.get())
            inspector.apply(ip)
                    .filter(f -> f.severity() != SecurityFinding.Severity.INFO)
                    .map(TlsCertScheduler::stabilize)
                    .ifPresent(reporter::report);
    }

    /** Tageszähler im Detailtext würden die Deduplizierung aushebeln und täglich neue Findings erzeugen. */
    static SecurityFinding stabilize(SecurityFinding f) {
        if (f.detail().startsWith(SELF_SIGNED_PREFIX)) return f;
        String detail = f.severity() == SecurityFinding.Severity.CRITICAL ? EXPIRED_DETAIL : EXPIRING_DETAIL;
        return new SecurityFinding(f.ip(), f.category(), f.severity(), detail);
    }

    static List<String> savedTlsHosts() {
        return NetworkStore.getInstance().getAllHosts().stream()
                .filter(h -> Arrays.stream(TLS_PORTS).anyMatch(h.ports::containsKey))
                .map(h -> h.ip)
                .toList();
    }
}
