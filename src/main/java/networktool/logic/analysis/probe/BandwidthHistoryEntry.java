package main.java.networktool.logic.analysis.probe;

/** Ein Zeitreihen-Eintrag der Bandbreiten-Historie eines Hosts. */
public record BandwidthHistoryEntry(long timestampMs, String ip, double downMbps, double upMbps) {

    public BandwidthHistoryEntry {
        ip = ip != null ? ip : "";
    }

    String toNdjson() {
        return "{\"ts\":" + timestampMs + ",\"ip\":\"" + esc(ip)
                + "\",\"down\":" + downMbps + ",\"up\":" + upMbps + "}";
    }

    static BandwidthHistoryEntry parse(String line) {
        if (line == null || line.isBlank()) return null;
        String t = line.trim();
        if (!t.startsWith("{") || !t.endsWith("}")) return null;
        Long ts = extractLong(t, "ts");
        Double down = extractDouble(t, "down");
        Double up = extractDouble(t, "up");
        String ip = extractStr(t, "ip");
        if (ts == null || down == null || up == null) return null;
        return new BandwidthHistoryEntry(ts, ip, down, up);
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Long extractLong(String json, String field) {
        Double d = extractDouble(json, field);
        return d != null ? d.longValue() : null;
    }

    private static Double extractDouble(String json, String field) {
        String key = "\"" + field + "\":";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int s = ki + key.length();
        int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e))
                || json.charAt(e) == '-' || json.charAt(e) == '.')) e++;
        if (e == s) return null;
        try { return Double.parseDouble(json.substring(s, e)); }
        catch (NumberFormatException ex) { return null; }
    }

    private static String extractStr(String json, String field) {
        String key = "\"" + field + "\":\"";
        int ki = json.indexOf(key);
        if (ki < 0) return "";
        int s = ki + key.length();
        int e = json.indexOf('"', s);
        return e < 0 ? "" : json.substring(s, e).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
