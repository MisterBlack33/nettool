package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.ScanProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Speichert und lädt Scan-Profile als JSON.
 *
 * Datei: txt/scanProfiles.json
 * Legacy-Migration: scanProfiles.txt → JSON wird automatisch einmalig durchgeführt.
 *
 * Singleton, thread-sicher.
 */
public final class ScanProfileStore {

    private static final class Holder {
        static final ScanProfileStore INSTANCE = new ScanProfileStore();
    }
    public static ScanProfileStore getInstance() { return Holder.INSTANCE; }

    private static final String JSON_FILE   = "scanProfiles.json";
    private static final String LEGACY_FILE = "scanProfiles.txt";

    private final List<ScanProfile> profiles = new ArrayList<>();
    private Path filePath;

    private ScanProfileStore() {
        filePath = StorageUtils.resolveTxtDir().resolve(JSON_FILE);
        load();
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public synchronized List<ScanProfile> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(profiles));
    }

    public synchronized Optional<ScanProfile> get(String name) {
        return profiles.stream().filter(p -> p.name.equals(name)).findFirst();
    }

    public synchronized void save(ScanProfile profile) {
        profiles.removeIf(p -> p.name.equals(profile.name));
        profiles.add(profile);
        persist();
    }

    public synchronized void delete(String name) {
        if (profiles.removeIf(p -> p.name.equals(name))) persist();
    }

    public synchronized void updateLastRun(String name, String timestamp) {
        profiles.stream().filter(p -> p.name.equals(name)).findFirst()
                .ifPresent(p -> { p.lastRun = timestamp; persist(); });
    }

    // ── Persistenz ────────────────────────────────────────────────────────

    private void load() {
        Path legacy = filePath.resolveSibling(LEGACY_FILE);
        if (!Files.exists(filePath) && Files.exists(legacy)) {
            loadLegacy(legacy);
            persist();
            try { Files.delete(legacy); } catch (IOException ignored) {}
            System.out.println("[ScanProfileStore] Migriert: .txt → .json");
            return;
        }
        if (!Files.exists(filePath)) return;
        try {
            loadFromJson(Files.readString(filePath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[ScanProfileStore] Laden: " + e.getMessage());
        }
    }

    private void loadFromJson(String json) {
        profiles.clear();
        int arrStart = json.indexOf("\"profiles\"");
        if (arrStart < 0) return;
        int start = json.indexOf('[', arrStart);
        if (start < 0) return;
        for (String obj : extractObjects(json, start)) {
            String name = StorageUtils.extractJsonStr(obj, "name");
            if (name == null || name.isBlank()) continue;
            ScanProfile p = new ScanProfile(name);
            p.osFilter = nvl(StorageUtils.extractJsonStr(obj, "osFilter"), "");
            p.hnFilter = nvl(StorageUtils.extractJsonStr(obj, "hnFilter"), "");
            p.category = nvl(StorageUtils.extractJsonStr(obj, "category"), "");
            p.lastRun  = nvl(StorageUtils.extractJsonStr(obj, "lastRun"),  "");
            p.autoSave = "true".equalsIgnoreCase(
                    StorageUtils.extractJsonStr(obj, "autoSave"));
            p.cidrs.addAll(extractStringArray(obj, "cidrs"));
            for (String s : extractStringArray(obj, "ports")) {
                try { p.ports.add(Integer.parseInt(s.trim())); }
                catch (NumberFormatException ignored) {}
            }
            profiles.add(p);
        }
    }

    private void persist() {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, buildJson(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[ScanProfileStore] Speichern: " + e.getMessage());
        }
    }

    private String buildJson() {
        StringBuilder sb = new StringBuilder("{\n  \"profiles\": [\n");
        for (int i = 0; i < profiles.size(); i++) {
            ScanProfile p = profiles.get(i);
            sb.append("    {\n")
                    .append("      \"name\": \"").append(esc(p.name)).append("\",\n")
                    .append("      \"cidrs\": ").append(strArray(p.cidrs)).append(",\n")
                    .append("      \"osFilter\": \"").append(esc(p.osFilter)).append("\",\n")
                    .append("      \"hnFilter\": \"").append(esc(p.hnFilter)).append("\",\n")
                    .append("      \"ports\": ").append(intArray(p.ports)).append(",\n")
                    .append("      \"autoSave\": ").append(p.autoSave).append(",\n")
                    .append("      \"category\": \"").append(esc(p.category)).append("\",\n")
                    .append("      \"lastRun\": \"").append(esc(p.lastRun)).append("\"\n")
                    .append("    }").append(i < profiles.size() - 1 ? "," : "").append("\n");
        }
        return sb.append("  ]\n}").toString();
    }

    private void loadLegacy(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            ScanProfile[] holder = {null};
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) { holder[0] = null; continue; }
                ScanProfile cur = holder[0];
                if (line.startsWith("NAME:")) {
                    holder[0] = new ScanProfile(line.substring(5).trim());
                    profiles.add(holder[0]);
                } else if (cur == null) {
                    // no current profile; skip
                } else if (line.startsWith("CIDRS:")) {
                    Arrays.stream(line.substring(6).split(",")).map(String::trim)
                            .filter(s -> !s.isBlank()).forEach(cur.cidrs::add);
                } else if (line.startsWith("OS:"))       { cur.osFilter = line.substring(3).trim(); }
                else if (line.startsWith("HN:"))       { cur.hnFilter = line.substring(3).trim(); }
                else if (line.startsWith("PORTS:")) {
                    Arrays.stream(line.substring(6).split(",")).map(String::trim)
                            .filter(s -> !s.isBlank())
                            .forEach(s -> { try { cur.ports.add(Integer.parseInt(s)); }
                            catch (NumberFormatException ignored) {} });
                } else if (line.startsWith("AUTOSAVE:")) { cur.autoSave = "true".equalsIgnoreCase(line.substring(9).trim()); }
                else if (line.startsWith("CATEGORY:")) { cur.category = line.substring(9).trim(); }
                else if (line.startsWith("LASTRUN:"))  { cur.lastRun  = line.substring(8).trim(); }
            }
        } catch (IOException e) {
            System.err.println("[ScanProfileStore] Legacy: " + e.getMessage());
        }
    }

    // ── JSON-Hilfsmethoden ────────────────────────────────────────────────

    private static List<String> extractObjects(String json, int arrStart) {
        List<String> result = new ArrayList<>();
        int depth = 0, objStart = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if      (c == '{') { if (depth++ == 0) objStart = i; }
            else if (c == '}') {
                if (--depth == 0 && objStart >= 0) {
                    result.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return result;
    }

    private static List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) return result;
        int s = json.indexOf('[', ki);
        int e = json.indexOf(']', s < 0 ? 0 : s);
        if (s < 0 || e < 0) return result;
        String inner = json.substring(s + 1, e).trim();
        if (inner.isBlank()) return result;
        for (String part : inner.split(",")) {
            String v = part.trim().replaceAll("^\"|\"$", "")
                    .replace("\\\"", "\"").replace("\\\\", "\\");
            if (!v.isBlank()) result.add(v);
        }
        return result;
    }

    private static String strArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(esc(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private static String intArray(List<Integer> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}