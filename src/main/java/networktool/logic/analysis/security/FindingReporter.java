package main.java.networktool.logic.analysis.security;

import main.java.networktool.security.AuditLogger;

/** Meldet neue Findings an den Collector und schreibt sie ins Audit-Log. */
final class FindingReporter {

    private final SecurityFindingsCollector sink;

    FindingReporter(SecurityFindingsCollector sink) { this.sink = sink; }

    /** @return true, wenn das Finding neu war. */
    boolean report(SecurityFinding finding) {
        if (!sink.addIfNew(finding)) return false;
        AuditLogger.getInstance().log("SECURITY_ALERT_" + finding.category().name(),
                finding.ip() + " " + finding.detail());
        return true;
    }
}
