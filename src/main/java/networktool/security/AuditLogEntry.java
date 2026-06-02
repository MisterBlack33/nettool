package main.java.networktool.security;

/**
 * Unveränderlicher Eintrag im Audit-Log.
 * Format-Version 1: {"v":1,"ts":"...","user":"...","action":"...","detail":"..."}
 */
public record AuditLogEntry(
        String timestamp,
        String user,
        String action,
        String detail
) {
    public AuditLogEntry {
        timestamp = timestamp != null ? timestamp : "";
        user      = user      != null ? user      : "system";
        action    = action    != null ? action    : "";
        detail    = detail    != null ? detail    : "";
    }

    /** Serialisiert als NDJSON-Zeile (Version 1). */
    public String toNdjson() {
        return "{\"v\":1,\"ts\":\""     + esc(timestamp)
                + "\",\"user\":\""         + esc(user)
                + "\",\"action\":\""       + esc(action)
                + "\",\"detail\":\""       + esc(detail) + "\"}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}