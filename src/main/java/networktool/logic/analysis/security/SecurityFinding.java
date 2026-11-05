package main.java.networktool.logic.analysis.security;

import java.time.Instant;
import java.util.Objects;

/** Unveränderlicher Sicherheitsbefund mit Herkunft, Einordnung und Beobachtungszeit. */
public record SecurityFinding(String ip, Category category, Severity severity, String detail,
                              Source source, Confidence confidence, Instant observedAt, Status status) {

    public enum Category { TLS_CERT, DEFAULT_CREDENTIALS, KNOWN_VULNERABLE, ROGUE_DHCP }
    public enum Severity { INFO, WARN, CRITICAL }
    public enum Source {
        TLS_CERT_INSPECTOR,
        DEFAULT_CREDENTIAL_PROBE,
        CVE_BANNER_MATCH,
        ROGUE_DHCP_DETECTOR,
        UNSPECIFIED
    }

    /** Confidence describes the strength of the collected evidence, not the impact. */
    public enum Confidence { CONFIRMED, HIGH, MEDIUM, LOW, UNKNOWN }
    public enum Status { OPEN, RESOLVED }

    public SecurityFinding(String ip, Category category, Severity severity, String detail) {
        this(ip, category, severity, detail, sourceFor(category), Confidence.UNKNOWN, Instant.now(), Status.OPEN);
    }

    public SecurityFinding(String ip, Category category, Severity severity, String detail,
                          Source source, Confidence confidence, Instant observedAt) {
        this(ip, category, severity, detail, source, confidence, observedAt, Status.OPEN);
    }

    public SecurityFinding {
        ip = ip != null ? ip : "";
        detail = detail != null ? detail : "";
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(observedAt, "observedAt");
        Objects.requireNonNull(status, "status");
    }

    public SecurityFinding withStatus(Status newStatus) {
        return new SecurityFinding(ip, category, severity, detail, source, confidence, observedAt, newStatus);
    }

    private static Source sourceFor(Category category) {
        if (category == null) return Source.UNSPECIFIED;
        return switch (category) {
            case TLS_CERT -> Source.TLS_CERT_INSPECTOR;
            case DEFAULT_CREDENTIALS -> Source.DEFAULT_CREDENTIAL_PROBE;
            case KNOWN_VULNERABLE -> Source.CVE_BANNER_MATCH;
            case ROGUE_DHCP -> Source.ROGUE_DHCP_DETECTOR;
        };
    }
}
