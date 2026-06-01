package main.java.networktool.storage;

import java.util.ArrayList;
import java.util.List;

/** Minimal JSON parse/write helpers. No external dependencies. */
public final class JsonHelper {

    private JsonHelper() {}

    static String extractStr(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length() || json.charAt(s) != '"') return null;
        s++;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append(c); sb.append(nx); }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    static String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    static List<String> extractObjects(String json, int arrStart) {
        List<String> objects = new ArrayList<>();
        int depth = 0, objStart = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth++ == 0) objStart = i; }
            else if (c == '}') {
                if (--depth == 0 && objStart >= 0) {
                    objects.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return objects;
    }

    static int findArrayStart(String json, String key) {
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) return -1;
        return json.indexOf('[', ki);
    }

    static String buildStringArrayJson(String key, List<String> items) {
        StringBuilder sb = new StringBuilder("{\n  \"" + key + "\": [\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append("    \"").append(esc(items.get(i))).append("\"");
            if (i < items.size() - 1) sb.append(",");
            sb.append("\n");
        }
        return sb.append("  ]\n}").toString();
    }

    static List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) return result;
        int arrStart = json.indexOf('[', start);
        int arrEnd   = json.indexOf(']', arrStart < 0 ? 0 : arrStart);
        if (arrStart < 0 || arrEnd < 0) return result;
        String inner = json.substring(arrStart + 1, arrEnd);
        for (String part : inner.split(",")) {
            String v = part.trim().replaceAll("^\"|\"$", "");
            if (!v.isBlank()) result.add(v);
        }
        return result;
    }
}