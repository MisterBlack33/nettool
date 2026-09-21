package main.java.networktool.logic.analysis.security;

/** Einzelnes Sicherheits-Finding eines Hosts. Unveränderlich. */
public record SecurityFinding(String ip, Category category, Severity severity, String detail) {

    public enum Category { TLS_CERT, DEFAULT_CREDENTIALS, KNOWN_VULNERABLE, ROGUE_DHCP }
    public enum Severity { INFO, WARN, CRITICAL }

    public SecurityFinding {
        ip     = ip     != null ? ip     : "";
        detail = detail != null ? detail : "";
    }
}
