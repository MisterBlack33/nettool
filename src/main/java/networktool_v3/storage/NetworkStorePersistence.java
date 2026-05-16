package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class NetworkStorePersistence {

    private NetworkStorePersistence() {}

    static final String SAVED_SUBDIR = "savedHostsTags";
    static final String FILE_EXT     = ".json";
    static final String ALL_FILE     = "all.json";
    static final String NTFY_FILE    = "ntfyTopics.json";
    static final String LEGACY_FILE  = "saved_hosts.txt";
    static final String LEGACY_EXT   = ".txt";

    // ── Path resolution ───────────────────────────────────────────────────

    static Path resolveTxtDir() {
        try {
            URL  url  = NetworkStorePersistence.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path base = Paths.get(url.toURI()).toAbsolutePath().normalize();
            if (base.toString().endsWith(".jar"))
                return base.getParent().resolve("txt");
            String pkg     = NetworkStorePersistence.class.getPackageName().replace('.', '/');
            String rootPkg = pkg.contains("/") ? pkg.substring(0, pkg.indexOf('/')) : pkg;
            Path candidate = base.resolve(rootPkg).resolve("txt");
            try { Files.createDirectories(candidate); return candidate; }
            catch (IOException ignored) {}
        } catch (URISyntaxException | SecurityException ignored) {}
        return Paths.get(System.getProperty("user.dir"), "networktool_v3", "txt");
    }

    static Path savedDir(Path txtDir) { return txtDir.resolve(SAVED_SUBDIR); }

    // ── Load ──────────────────────────────────────────────────────────────

    static void loadAll(Path txtDir, Map<String, List<HostResult>> networks,
                        Map<String, String> prefixes) {
        Path saved = savedDir(txtDir);
        if (!Files.isDirectory(saved)) return;
        try {
            Files.list(saved)
                    .filter(p -> p.getFileName().toString().endsWith(FILE_EXT)
                            && !p.getFileName().toString().equals(ALL_FILE))
                    .sorted()
                    .forEach(f -> loadJsonFile(f, stripExt(f.getFileName().toString()),
                            networks, prefixes));
        } catch (IOException e) {
            System.err.println("[NetworkStore] loadAll: " + e.getMessage());
        }
    }

    private static void loadJsonFile(Path file, String name,
                                     Map<String, List<HostResult>> networks,
                                     Map<String, String> prefixes) {
        try {
            String json   = Files.readString(file, StandardCharsets.UTF_8);
            String prefix = JsonHelper.extractStr(json, "prefix");
            if (prefixes != null && prefix != null) prefixes.put(name, prefix);

            List<HostResult> list = networks.computeIfAbsent(name, k -> new ArrayList<>());
            int arrStart = JsonHelper.findArrayStart(json, "hosts");
            if (arrStart < 0) return;
            for (String obj : JsonHelper.extractObjects(json, arrStart)) {
                HostResult h = HostJsonBuilder.parseHost(obj);
                if (h != null) list.add(h);
            }
        } catch (IOException e) {
            System.err.println("[NetworkStore] load " + name + ": " + e.getMessage());
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────

    static void saveNetwork(Path txtDir, String name,
                            List<HostResult> hosts, String prefix) {
        try {
            Path dir = savedDir(txtDir);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + FILE_EXT),
                    HostJsonBuilder.buildNetworkJson(name, prefix != null ? prefix : "", hosts),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[NetworkStore] save " + name + ": " + e.getMessage());
        }
    }

    static void saveAllFile(Path txtDir, Map<String, List<HostResult>> networks) {
        try {
            Path dir = savedDir(txtDir);
            Files.createDirectories(dir);
            Set<String> seen = new LinkedHashSet<>();
            StringBuilder sb = new StringBuilder("{\n  \"generated\": true,\n  \"entries\": [\n");
            boolean first = true;
            for (Map.Entry<String, List<HostResult>> e : networks.entrySet()) {
                for (HostResult h : e.getValue()) {
                    if (!seen.add(h.ip)) continue;
                    if (!first) sb.append(",\n");
                    sb.append("    {\"ip\":\"").append(JsonHelper.esc(h.ip))
                            .append("\",\"category\":\"").append(JsonHelper.esc(e.getKey())).append("\"}");
                    first = false;
                }
            }
            sb.append("\n  ]\n}");
            Files.writeString(dir.resolve(ALL_FILE), sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    // ── ntfy topics ───────────────────────────────────────────────────────

    static List<String> loadNtfyTopics(Path txtDir) {
        Path file = txtDir.resolve(NTFY_FILE);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            return JsonHelper.extractStringArray(
                    Files.readString(file, StandardCharsets.UTF_8), "topics");
        } catch (IOException e) { return new ArrayList<>(); }
    }

    static void saveNtfyTopic(Path txtDir, String topic) {
        if (topic == null || topic.isBlank()) return;
        try {
            Files.createDirectories(txtDir);
            List<String> existing = new ArrayList<>(loadNtfyTopics(txtDir));
            if (existing.contains(topic)) return;
            existing.add(topic);
            Collections.sort(existing);
            Files.writeString(txtDir.resolve(NTFY_FILE),
                    JsonHelper.buildStringArrayJson("topics", existing),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    // ── Legacy migration ──────────────────────────────────────────────────

    static boolean needsLegacyImport(Path txtDir) {
        Path newSaved = savedDir(txtDir);
        Path oldSaved = txtDir.resolve("saved");
        if (Files.isDirectory(oldSaved) && !Files.isDirectory(newSaved)) {
            try { Files.move(oldSaved, newSaved); } catch (IOException ignored) {}
        }
        if (Files.isDirectory(newSaved)) convertLegacyTxtFiles(newSaved);
        try {
            if (!Files.isDirectory(newSaved)) return true;
            return Files.list(newSaved)
                    .noneMatch(p -> p.getFileName().toString().endsWith(FILE_EXT)
                            && !p.getFileName().toString().equals(ALL_FILE));
        } catch (IOException e) { return true; }
    }

    private static void convertLegacyTxtFiles(Path dir) {
        try {
            Files.list(dir)
                    .filter(p -> p.getFileName().toString().endsWith(LEGACY_EXT)
                            && !p.getFileName().toString().equals("all.txt"))
                    .forEach(f -> {
                        String name = stripExt(f.getFileName().toString());
                        Map<String, List<HostResult>> tmp  = new LinkedHashMap<>();
                        Map<String, String>           prfx = new LinkedHashMap<>();
                        loadLegacyFile(f, name, tmp, prfx);
                        saveNetwork(dir.getParent(), name,
                                tmp.getOrDefault(name, new ArrayList<>()),
                                prfx.getOrDefault(name, ""));
                        try { Files.delete(f); } catch (IOException ignored) {}
                        System.out.println("[NetworkStore] Converted: " + name + ".txt → .json");
                    });
        } catch (IOException ignored) {}
    }

    static void loadFile(Path file, String name, Map<String, List<HostResult>> networks) {
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
                list.add(new HostResult(
                        p[0].trim(),
                        p.length >= 2 ? p[1].trim() : p[0].trim(),
                        p.length >= 3 ? p[2].trim() : "",
                        p.length >= 5 ? p[4].trim() : "",
                        parsePorts(p.length >= 4 ? p[3].trim() : ""),
                        p.length >= 6 ? p[5].trim() : ""
                ));
            }
        } catch (IOException e) {
            System.err.println("[NetworkStore] legacy load " + name + ": " + e.getMessage());
        }
    }

    // ── Port helpers (legacy CSV format) ─────────────────────────────────

    static Map<Integer, String> parsePorts(String s) {
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

    // Delegated for backward compat (StorageUtils, tests)
    static String extractStr(String json, String field) { return JsonHelper.extractStr(json, field); }
    static String esc(String s) { return JsonHelper.esc(s); }

    private static String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}