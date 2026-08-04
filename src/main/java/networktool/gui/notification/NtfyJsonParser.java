package networktool.gui.notification;

/**
 * Minimaler, abhängigkeitsfreier Parser für einzelne ntfy.sh-NDJSON-Zeilen.
 * Kein vollständiger JSON-Parser – extrahiert gezielt die wenigen benötigten Felder.
 */
final class NtfyJsonParser {

    private NtfyJsonParser() {}

    static final class NtfyEvent {
        String id, event, topic, title, message;
    }

    static NtfyEvent parse(String json) {
        if (json == null || !json.startsWith("{")) return null;
        NtfyEvent ev = new NtfyEvent();
        ev.id      = extractStr(json, "id");
        ev.event   = extractStr(json, "event");
        ev.topic   = extractStr(json, "topic");
        ev.title   = extractStr(json, "title");
        ev.message = extractStr(json, "message");
        return ev;
    }

    private static String extractStr(String json, String field) {
        String keyPattern1 = ",\"" + field + "\"";
        String keyPattern2 = "{\"" + field + "\"";
        int ki;
        int i1 = json.indexOf(keyPattern1), i2 = json.indexOf(keyPattern2);
        if (i1 < 0 && i2 < 0) return null;
        if (i1 < 0) ki = i2;
        else if (i2 < 0) ki = i1;
        else ki = Math.min(i1, i2);
        int keyStart = json.indexOf('"', ki);
        if (keyStart < 0) return null;
        int keyEnd = json.indexOf('"', keyStart + 1);
        if (keyEnd < 0) return null;
        int colon = json.indexOf(':', keyEnd + 1);
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length() || json.charAt(s) != '"') return null;
        s++;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append(ch); sb.append(nx); }
                }
            } else if (ch == '"') {
                break;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
