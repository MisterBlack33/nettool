package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Datei-I/O für {@link NetworkStore}.
 *
 * Alle Daten werden als JSON gespeichert (kein plain-text mehr).
 *
 * Verzeichnisstruktur:
 *   txt/
 *     savedHostsTags/
 *       Heim.json        ← JSON-Datei je Kategorie
 *       Schule.json
 *       all.json         ← Alle Hosts, dedupliziert, auto-generiert
 *     ntfyTopics.json    ← JSON-Array der ntfy-Topics
 *     scanProfiles.json  ← Scan-Profile als JSON-Array
 *
 * Host-JSON-Format (pro Kategorie-Datei):
 * {
 *   "network": "Heim",
 *   "prefix": "192.168.1.",
 *   "hosts": [
 *     {
 *       "ip": "192.168.1.1",
 *       "hostname": "fritz.box",
 *       "os": "Linux/Unix",
 *       "savedAt": "2026-04-30 10:00:00",
 *       "ports": {"80": "nginx", "22": "OpenSSH"},
 *       "notes": "Router"
 *     }
 *   ]
 * }
 */
final class NetworkStorePersistence {

    private NetworkStorePersistence() {}

    static final String SAVED_SUBDIR  = "savedHostsTags";
    static final String FILE_EXT      = ".json";
    static final String ALL_FILE      = "all.json";
    static final String NTFY_FILE     = "ntfyTopics.json";
    static final String LEGACY_FILE   = "saved_hosts.txt";
    static final String LEGACY_EXT    = ".txt";

    // ── Pfad-Auflösung ────────────────────────────────────────────────────

    static Path resolveTxtDir() {
        try {
            URL  url  = NetworkStorePersistence.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path base = Paths.get(url.toURI()).toAbsolutePath().normalize();

            if (base.toString().endsWith(".jar"))
                return base.getParent().resolve("txt");

            String pkg     = NetworkStorePersistence.class.getPackageName().replace('.', '/');
            String rootPkg = pkg.contains("/") ? pkg.substring(0, pkg.indexOf('/')) : pkg;
            Path txtInPkg  = base.resolve(rootPkg).resolve("txt");
            if (Files.isDirectory(txtInPkg)) return txtInPkg;
            try { Files.createDirectories(txtInPkg); return txtInPkg; }
            catch (IOException ignored2) {}
            Path candidate = base.resolve("txt");
            if (Files.isDirectory(candidate)) return candidate;
            return candidate;

        } catch (URISyntaxException | SecurityException ignored) {}
        return Paths.get(System.getProperty("user.dir"), "networktool_v3", "txt");
    }

    static Path savedDir(Path txtDir) {
        return txtDir.resolve(SAVED_SUBDIR);
    }

    // ── Laden ─────────────────────────────────────────────────────────────

    static void loadAll(Path txtDir,
                        Map<String, List<HostResult>> networks,
                        Map<String, String> prefixes) {
        Path saved = savedDir(txtDir);
        if (!Files.isDirectory(saved)) return;
        try {
            Files.list(saved)
                    .filter(p -> p.getFileName().toString().endsWith(FILE_EXT)
                            && !p.getFileName().toString().equals(ALL_FILE))
                    .sorted()
                    .forEach(f -> {
                        String name = stripExt(f.getFileName().toString());
                        loadJsonFile(f, name, networks, prefixes);
                    });
        } catch (IOException e) {
            System.err.println("[NetworkStore] loadAll: " + e.getMessage());
        }
    }

    static void loadAll(Path txtDir, Map<String, List<HostResult>> networks) {
        loadAll(txtDir, networks, null);
    }

    private static void loadJsonFile(Path file, String name,
                                     Map<String, List<HostResult>> networks,
                                     Map<String, String> prefixes) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            String prefix = extractJsonStr(json, "prefix");
            if (prefixes != null && prefix != null)
                prefixes.put(name, prefix);

            List<HostResult> list = networks.computeIfAbsent(name, k -> new ArrayList<>());
            // Parse hosts array
            int arrStart = findArrayStart(json, "hosts");
            if (arrStart < 0) return;
            List<String> objects = extractJsonObjects(json, arrStart);
            for (String obj : objects) {
                String ip      = extractJsonStr(obj, "ip");
                if (ip == null || ip.isBlank()) continue;
                String hostname = nvl(extractJsonStr(obj, "hostname"), ip);
                String os       = nvl(extractJsonStr(obj, "os"), "");
                String savedAt  = nvl(extractJsonStr(obj, "savedAt"), "");
                String notes    = nvl(extractJsonStr(obj, "notes"), "");
                Map<Integer, String> ports = parsePortsObj(extractJsonRaw(obj, "ports"));
                list.add(new HostResult(ip, hostname, os, savedAt, ports, notes));
            }
        } catch (IOException e) {
            System.err.println("[NetworkStore] Laden " + name + ": " + e.getMessage());
        }
    }

    // ── Speichern ─────────────────────────────────────────────────────────

    static void saveNetwork(Path txtDir, String name,
                            List<HostResult> hosts, String prefix) {
        try {
            Path dir = savedDir(txtDir);
            Files.createDirectories(dir);
            String json = buildNetworkJson(name, prefix != null ? prefix : "", hosts);
            Files.writeString(dir.resolve(name + FILE_EXT), json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[NetworkStore] Speichern " + name + ": " + e.getMessage());
        }
    }

    static void saveNetwork(Path txtDir, String name, List<HostResult> hosts) {
        saveNetwork(txtDir, name, hosts, null);
    }

    private static String buildNetworkJson(String name, String prefix,
                                           List<HostResult> hosts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"network\": \"").append(esc(name)).append("\",\n");
        sb.append("  \"prefix\": \"").append(esc(prefix)).append("\",\n");
        sb.append("  \"hosts\": [\n");
        for (int i = 0; i < hosts.size(); i++) {
            HostResult h = hosts.get(i);
            sb.append("    {\n");
            sb.append("      \"ip\": \"").append(esc(h.ip)).append("\",\n");
            sb.append("      \"hostname\": \"").append(esc(h.hostname)).append("\",\n");
            sb.append("      \"os\": \"").append(esc(h.os)).append("\",\n");
            sb.append("      \"savedAt\": \"").append(esc(h.savedAt)).append("\",\n");
            sb.append("      \"ports\": ").append(serPortsJson(h.ports)).append(",\n");
            sb.append("      \"notes\": \"").append(esc(h.notes)).append("\"\n");
            sb.append("    }").append(i < hosts.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    /** Schreibt all.json: alle IPs aller Kategorien, dedupliziert. */
    static void saveAllFile(Path txtDir, Map<String, List<HostResult>> networks) {
        try {
            Path dir = savedDir(txtDir);
            Files.createDirectories(dir);
            Set<String> seen = new LinkedHashSet<>();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"generated\": true,\n  \"entries\": [\n");
            boolean first = true;
            for (Map.Entry<String, List<HostResult>> e : networks.entrySet()) {
                String cat = e.getKey();
                for (HostResult h : e.getValue()) {
                    if (!seen.add(h.ip)) continue;
                    if (!first) sb.append(",\n");
                    sb.append("    {\"ip\":\"").append(esc(h.ip))
                            .append("\",\"hostname\":\"").append(esc(h.hostname))
                            .append("\",\"os\":\"").append(esc(h.os))
                            .append("\",\"savedAt\":\"").append(esc(h.savedAt))
                            .append("\",\"ports\":").append(serPortsJson(h.ports))
                            .append(",\"notes\":\"").append(esc(h.notes))
                            .append("\",\"category\":\"").append(esc(cat)).append("\"}");
                    first = false;
                }
            }
            sb.append("\n  ]\n}");
            Files.writeString(dir.resolve(ALL_FILE), sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    // ── ntfy-Topics (JSON) ────────────────────────────────────────────────

    static List<String> loadNtfyTopics(Path txtDir) {
        Path file = txtDir.resolve(NTFY_FILE);
        if (!Files.exists(file)) {
            // Legacy: alte .txt-Datei lesen
            Path legacy = txtDir.resolve("ntfyTopics.txt");
            if (Files.exists(legacy)) return loadNtfyTopicsLegacy(legacy);
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return extractStringArray(json, "topics");
        } catch (IOException e) { return new ArrayList<>(); }
    }

    static void saveNtfyTopic(Path txtDir, String topic) {
        if (topic == null || topic.isBlank()) return;
        try {
            Files.createDirectories(txtDir);
            List<String> existing = loadNtfyTopics(txtDir);
            if (existing.contains(topic)) return;
            existing.add(topic);
            Collections.sort(existing);
            Files.writeString(txtDir.resolve(NTFY_FILE),
                    buildStringArrayJson("topics", existing),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private static List<String> loadNtfyTopicsLegacy(Path file) {
        try {
            return new ArrayList<>(Files.readAllLines(file).stream()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .distinct().toList());
        } catch (IOException e) { return new ArrayList<>(); }
    }

    // ── Legacy-Import (.txt → .json) ─────────────────────────────────────

    static boolean needsLegacyImport(Path txtDir) {
        Path newSaved = savedDir(txtDir);
        // Migration: altes saved/-Verzeichnis
        Path oldSaved = txtDir.resolve("saved");
        if (Files.isDirectory(oldSaved) && !Files.isDirectory(newSaved)) {
            try { Files.move(oldSaved, newSaved); }
            catch (IOException e) { System.err.println("[NetworkStore] Migration: " + e.getMessage()); }
        }
        // Konvertierung: .txt → .json
        if (Files.isDirectory(newSaved)) {
            convertLegacyTxtFiles(newSaved);
        }
        try {
            if (!Files.isDirectory(newSaved)) return true;
            return Files.list(newSaved)
                    .noneMatch(p -> p.getFileName().toString().endsWith(FILE_EXT)
                            && !p.getFileName().toString().equals(ALL_FILE));
        } catch (IOException e) { return true; }
    }

    /** Wandelt alle vorhandenen .txt-Dateien in .json um und löscht die .txt. */
    private static void convertLegacyTxtFiles(Path dir) {
        try {
            Files.list(dir)
                    .filter(p -> p.getFileName().toString().endsWith(LEGACY_EXT)
                            && !p.getFileName().toString().equals("all.txt"))
                    .forEach(txtFile -> {
                        String name = stripExt(txtFile.getFileName().toString());
                        Map<String, List<HostResult>> tmp  = new LinkedHashMap<>();
                        Map<String, String>           prfx = new LinkedHashMap<>();
                        loadLegacyFile(txtFile, name, tmp, prfx);
                        List<HostResult> hosts = tmp.getOrDefault(name, new ArrayList<>());
                        String prefix = prfx.getOrDefault(name, "");
                        saveNetwork(dir.getParent(), name, hosts, prefix);
                        try { Files.delete(txtFile); }
                        catch (IOException e) { System.err.println("[NetworkStore] Löschen " + txtFile + ": " + e.getMessage()); }
                        System.out.println("[NetworkStore] Konvertiert: " + name + ".txt → " + name + ".json");
                    });
        } catch (IOException e) {
            System.err.println("[NetworkStore] Konvertierung: " + e.getMessage());
        }
    }

    /**
     * Rückwärtskompatibles Alias für {@link #loadLegacyFile}.
     * Wird von {@link NetworkStore} beim Legacy-Import aufgerufen (3-Parameter-Signatur).
     */
    static void loadFile(Path file, String name,
                         Map<String, List<HostResult>> networks) {
        loadLegacyFile(file, name, networks, null);
    }

    static void loadLegacyFile(Path file, String name,
                               Map<String, List<HostResult>> networks,
                               Map<String, String> prefixes) {
        List<HostResult> list = networks.computeIfAbsent(name, k -> new ArrayList<>());
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                if (line.startsWith("IP-PRÄFIX:")) {
                    if (prefixes != null)
                        prefixes.put(name, line.substring("IP-PRÄFIX:".length()).trim());
                    continue;
                }
                String[] p = line.split(";", 6);
                if (p.length < 1 || p[0].isBlank()) continue;
                String ip       = p[0].trim();
                String hostname = p.length >= 2 ? p[1].trim() : ip;
                String os       = p.length >= 3 ? p[2].trim() : "";
                String ports    = p.length >= 4 ? p[3].trim() : "";
                String savedAt  = p.length >= 5 ? p[4].trim() : "";
                String notes    = p.length >= 6 ? p[5].trim() : "";
                list.add(new HostResult(ip, hostname, os, savedAt,
                        parseLegacyPorts(ports), notes));
            }
        } catch (IOException e) {
            System.err.println("[NetworkStore] Legacy-Laden " + name + ": " + e.getMessage());
        }
    }

    // ── JSON-Hilfsmethoden ────────────────────────────────────────────────

    static Map<Integer, String> parsePorts(String s) {
        return parseLegacyPorts(s);
    }

    private static Map<Integer, String> parseLegacyPorts(String s) {
        Map<Integer, String> map = new TreeMap<>();
        if (s == null || s.isBlank()) return map;
        for (String e : s.split(",")) {
            String[] kv = e.split(":", 2);
            try { map.put(Integer.parseInt(kv[0].trim()),
                    kv.length > 1 ? kv[1].trim() : "offen"); }
            catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private static Map<Integer, String> parsePortsObj(String portsJson) {
        Map<Integer, String> map = new TreeMap<>();
        if (portsJson == null || portsJson.isBlank() || portsJson.equals("{}")) return map;
        // {"80":"nginx","22":"OpenSSH"}
        String inner = portsJson.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}"))   inner = inner.substring(0, inner.length() - 1);
        for (String pair : splitJsonPairs(inner)) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) continue;
            try {
                int port = Integer.parseInt(unquote(kv[0].trim()));
                String banner = unquote(kv[1].trim());
                map.put(port, banner);
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private static String serPortsJson(Map<Integer, String> ports) {
        if (ports == null || ports.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : ports.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"")
                    .append(esc(e.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    static String serPorts(Map<Integer, String> p) {
        // Legacy-Format für Rückwärtskompatibilität
        if (p == null || p.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        p.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(",");
            sb.append(k).append(":").append(v.replace(",", "|").replace(";", "|"));
        });
        return sb.toString();
    }

    private static String buildStringArrayJson(String key, List<String> items) {
        StringBuilder sb = new StringBuilder("{\n  \"" + key + "\": [\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append("    \"").append(esc(items.get(i))).append("\"");
            if (i < items.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    private static List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) return result;
        int arrStart = json.indexOf('[', start);
        if (arrStart < 0) return result;
        int arrEnd   = json.indexOf(']', arrStart);
        if (arrEnd   < 0) return result;
        String inner = json.substring(arrStart + 1, arrEnd);
        for (String part : inner.split(",")) {
            String v = part.trim();
            if (v.startsWith("\"")) v = v.substring(1);
            if (v.endsWith("\""))   v = v.substring(0, v.length() - 1);
            if (!v.isBlank()) result.add(v);
        }
        return result;
    }

    static String extractJsonStr(String json, String field) {
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

    /** Gibt den rohen JSON-Wert (ohne Außenanführungszeichen) für ein Feld zurück. */
    private static String extractJsonRaw(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length()) return null;
        char first = json.charAt(s);
        if (first == '{') {
            // Objekt
            int depth = 0, end = s;
            for (int i = s; i < json.length(); i++) {
                if (json.charAt(i) == '{') depth++;
                else if (json.charAt(i) == '}') { if (--depth == 0) { end = i; break; } }
            }
            return json.substring(s, end + 1);
        }
        return null;
    }

    private static int findArrayStart(String json, String key) {
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) return -1;
        return json.indexOf('[', ki);
    }

    private static List<String> extractJsonObjects(String json, int arrStart) {
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

    private static List<String> splitJsonPairs(String inner) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        int start = 0;
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

    static String san(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private static String unquote(String s) {
        if (s == null) return "";
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\""))   s = s.substring(0, s.length() - 1);
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}