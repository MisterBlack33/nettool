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
                    case '/'  -> sb.append('/');
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case 'r'  -> sb.append('\r');
                    case 'u'  -> { i = appendUnicodeEscape(json, i, sb); }
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

    /** Dekodiert \\uXXXX ab Index i (Position von 'u'). Gibt neuen Index zurück; ungültige Sequenzen bleiben unverändert. */
    private static int appendUnicodeEscape(String json, int i, StringBuilder sb) {
        if (i + 4 < json.length()) {
            String hex = json.substring(i + 1, i + 5);
            try { sb.append((char) Integer.parseInt(hex, 16)); return i + 4; }
            catch (NumberFormatException ignored) { /* fall through: keep raw */ }
        }
        sb.append("\\u");
        return i;
    }

    /** Extrahiert ein Integer-Feld, z.B. für Schema-Versionen. Null wenn Feld fehlt oder ungültig ist. */
    static Integer extractInt(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
        int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++;
        if (e == s) return null;
        try { return Integer.parseInt(json.substring(s, e)); }
        catch (NumberFormatException ex) { return null; }
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

    // ── String-sicheres Struktur-Scanning ───────────────────────────────────
    // Zählt Klammern nur außerhalb von JSON-Strings, damit strukturelle
    // Zeichen ({, }, [, ], ,) innerhalb von Werten das Parsing nicht brechen.

    /** Überspringt einen JSON-String ab der öffnenden Anführung; gibt Index der schließenden zurück. */
    private static int skipString(String json, int quoteIdx) {
        int i = quoteIdx + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') { i += 2; continue; }
            if (c == '"') return i;
            i++;
        }
        return json.length() - 1; // unterminiert – fail safe statt Exception
    }

    /** Findet die schließende Klammer zu openIdx ('{' oder '['), ignoriert Klammern innerhalb von Strings. */
    static int matchBracket(String json, int openIdx) {
        char open = json.charAt(openIdx);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        for (int i = openIdx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') { i = skipString(json, i); continue; }
            if (c == open) depth++;
            else if (c == close && --depth == 0) return i;
        }
        return -1;
    }

    static List<String> extractObjects(String json, int arrStart) {
        List<String> objects = new ArrayList<>();
        int depth = 0, objStart = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') { i = skipString(json, i); continue; } // String-Inhalt zählt nicht als Struktur
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