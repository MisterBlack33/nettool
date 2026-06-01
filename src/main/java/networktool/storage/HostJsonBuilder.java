package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

import java.util.*;

/** Builds and parses the JSON representation of a single host entry. Package-private. */
final class HostJsonBuilder {

    private HostJsonBuilder() {}

    static String buildNetworkJson(String name, String prefix, List<HostResult> hosts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append("  \"network\": \"").append(JsonHelper.esc(name)).append("\",\n")
                .append("  \"prefix\": \"").append(JsonHelper.esc(prefix)).append("\",\n")
                .append("  \"hosts\": [\n");
        for (int i = 0; i < hosts.size(); i++) {
            appendHost(sb, hosts.get(i), i < hosts.size() - 1);
        }
        return sb.append("  ]\n}").toString();
    }

    private static void appendHost(StringBuilder sb, HostResult h, boolean comma) {
        sb.append("    {\n")
                .append("      \"ip\": \"")       .append(JsonHelper.esc(h.ip))       .append("\",\n")
                .append("      \"hostname\": \"") .append(JsonHelper.esc(h.hostname)) .append("\",\n")
                .append("      \"os\": \"")       .append(JsonHelper.esc(h.os))       .append("\",\n")
                .append("      \"savedAt\": \"")  .append(JsonHelper.esc(h.savedAt))  .append("\",\n")
                .append("      \"ports\": ")      .append(serPortsJson(h.ports))      .append(",\n")
                .append("      \"notes\": \"")    .append(JsonHelper.esc(h.notes))    .append("\"\n")
                .append("    }").append(comma ? "," : "").append("\n");
    }

    static HostResult parseHost(String obj) {
        String ip = JsonHelper.extractStr(obj, "ip");
        if (ip == null || ip.isBlank()) return null;
        return new HostResult(
                ip,
                JsonHelper.nvl(JsonHelper.extractStr(obj, "hostname"), ip),
                JsonHelper.nvl(JsonHelper.extractStr(obj, "os"),       ""),
                JsonHelper.nvl(JsonHelper.extractStr(obj, "savedAt"),  ""),
                parsePortsObj(extractRawPorts(obj)),
                JsonHelper.nvl(JsonHelper.extractStr(obj, "notes"),    "")
        );
    }

    static String serPortsJson(Map<Integer, String> ports) {
        if (ports == null || ports.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : ports.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"")
                    .append(JsonHelper.esc(e.getValue())).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }

    private static Map<Integer, String> parsePortsObj(String portsJson) {
        Map<Integer, String> map = new TreeMap<>();
        if (portsJson == null || portsJson.isBlank() || portsJson.equals("{}")) return map;
        String inner = portsJson.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}"))   inner = inner.substring(0, inner.length() - 1);
        for (String pair : splitPairs(inner)) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) continue;
            try { map.put(Integer.parseInt(unquote(kv[0].trim())), unquote(kv[1].trim())); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private static String extractRawPorts(String obj) {
        String key   = "\"ports\"";
        int ki       = obj.indexOf(key);
        if (ki < 0) return null;
        int colon = obj.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < obj.length() && obj.charAt(s) == ' ') s++;
        if (s >= obj.length() || obj.charAt(s) != '{') return null;
        int depth = 0, end = s;
        for (int i = s; i < obj.length(); i++) {
            if      (obj.charAt(i) == '{') depth++;
            else if (obj.charAt(i) == '}') { if (--depth == 0) { end = i; break; } }
        }
        return obj.substring(s, end + 1);
    }

    private static List<String> splitPairs(String inner) {
        List<String> pairs = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                pairs.add(inner.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (start < inner.length()) pairs.add(inner.substring(start).trim());
        return pairs;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\""))   s = s.substring(0, s.length() - 1);
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}