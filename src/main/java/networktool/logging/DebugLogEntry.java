package main.java.networktool.logging;

/** Unveränderlicher Eintrag im Debug-Log. Kein Sicherheitskontext (im Gegensatz zu AuditLogEntry). */
public record DebugLogEntry(String timestamp, String level, String message) {

    public DebugLogEntry {
        timestamp = timestamp != null ? timestamp : "";
        level     = level     != null ? level     : "INFO";
        message   = message   != null ? message   : "";
    }

    String toLine() {
        return timestamp + "\t" + sanitize(level) + "\t" + sanitize(message);
    }

    private static String sanitize(String s) {
        return s.replace("\t", " ").replace("\n", " ").replace("\r", "");
    }
}
