package main.java.networktool.logging;

/**
 * Gemeinsamer Log-Eintrag für Audit- und Debug-Logs.
 * category unterscheidet die Quelle ("AUDIT"/"DEBUG"); je nach Quelle bleibt
 * user (Audit) bzw. level (Debug) das führende Feld, das jeweils andere leer.
 */
public record LogEntry(
        String timestamp,
        String category,
        String user,
        String level,
        String action,
        String detail
) {
    public LogEntry {
        timestamp = timestamp != null ? timestamp : "";
        category  = category  != null ? category  : "";
        user      = user      != null ? user      : "";
        level     = level     != null ? level     : "";
        action    = action    != null ? action    : "";
        detail    = detail    != null ? detail    : "";
    }

    /** Serialisiert als NDJSON-Zeile (Version 2 – additiv zu Audit-v1/Debug-Tab-Format). */
    public String toNdjson() {
        return "{\"v\":2,\"ts\":\""      + esc(timestamp)
                + "\",\"category\":\""   + esc(category)
                + "\",\"user\":\""       + esc(user)
                + "\",\"level\":\""      + esc(level)
                + "\",\"action\":\""     + esc(action)
                + "\",\"detail\":\""     + esc(detail) + "\"}";
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