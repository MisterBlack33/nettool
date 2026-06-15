package main.java.networktool.storage;

import main.java.networktool.model.HostResult;

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
    // Legacy: ältere saved_hosts.txt
    static final String LEGACY_FILE  = "saved_hosts.txt";
    static final String LEGACY_EXT   = ".txt";

    // ── Path resolution ───────────────────────────────────────────────────

    // Neu: liefert das Verzeichnis 'data' (früher 'txt')
    static Path resolveDataDir() {
        try {
            URL  url  = NetworkStorePersistence.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path base = Paths.get(url.toURI()).toAbsolutePath().normalize();
            if (base.toString().endsWith(".jar"))
                return base.getParent().resolve("data");
            String pkg     = NetworkStorePersistence.class.getPackageName().replace('.', '/');
            String rootPkg = pkg.contains("/") ? pkg.substring(0, pkg.indexOf('/')) : pkg;
            Path candidate = base.resolve(rootPkg).resolve("data");
            try { Files.createDirectories(candidate); return candidate; }
            catch (IOException ignored) {}
        } catch (URISyntaxException | SecurityException ignored) {}
        return Paths.get(System.getProperty("user.dir"), "networktool", "data");
    }

    static Path savedDir(Path dataDir) {
        return dataDir.resolve(SAVED_SUBDIR);
    }

    // ── Load ──────────────────────────────────────────────────────────────

    static void loadAll(Path dataDir, Map<String, List<HostResult>> networks,
                        Map<String, String> prefixes) {
        Path saved = savedDir(dataDir);
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

    static void saveNetwork(Path dataDir, String name,
                            List<HostResult> hosts, String prefix) {
        try {
            Path dir = savedDir(dataDir);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + FILE_EXT),
                    HostJsonBuilder.buildNetworkJson(name, prefix != null ? prefix : "", hosts),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[NetworkStore] save " + name + ": " + e.getMessage());
        }
    }

    static void saveAllFile(Path dataDir, Map<String, List<HostResult>> networks) {
        try {
            Path dir = savedDir(dataDir);
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

    // ── ntfy topics — delegated ───────────────────────────────────────────

    static List<String> loadNtfyTopics(Path dataDir) {
        return NetworkStoreNtfy.loadTopics(dataDir);
    }

    static void saveNtfyTopic(Path dataDir, String topic) {
        NetworkStoreNtfy.saveTopic(dataDir, topic);
    }

    // ── Legacy migration — delegated ──────────────────────────────────────

    static boolean needsLegacyImport(Path dataDir) {
        return NetworkStoreLegacy.needsImport(dataDir);
    }

    static void loadFile(Path file, String name, Map<String, List<HostResult>> networks) {
        NetworkStoreLegacy.loadFile(file, name, networks, null);
    }

    static void loadLegacyFile(Path file, String name,
                               Map<String, List<HostResult>> networks,
                               Map<String, String> prefixes) {
        NetworkStoreLegacy.loadFile(file, name, networks, prefixes);
    }

    static Map<Integer, String> parsePorts(String s) {
        return NetworkStoreLegacy.parsePorts(s);
    }

    /**
     * Führt einmalige Legacy-Migrationen von alten ".txt"-Speichern in das
     * neue Datenverzeichnis durch. Ziel ist es, vorhandene Legacy-Dateien
     * automatisch in die neuen Formate zu überführen (binär/json), ohne dass
     * Nutzer- oder Testcode manuell eingreifen muss.
     */
    static void migrateLegacyTxtFiles(Path dataDir) {
        if (dataDir == null) return;
        try {
            // 1) Trigger SavedHostsStore migration: setFilePath() lädt und migriert
            try {
                main.java.networktool.storage.SavedHostsStore.getInstance()
                        .setFilePath(dataDir.resolve("saved_hosts.bin"));
            } catch (Throwable t) {
                System.err.println("[NetworkStorePersistence] SavedHostsStore migration failed: " + t.getMessage());
            }

            // 2) Trigger ScanProfileStore constructor/migration
            try {
                main.java.networktool.storage.ScanProfileStore.getInstance();
            } catch (Throwable t) {
                System.err.println("[NetworkStorePersistence] ScanProfileStore migration failed: " + t.getMessage());
            }

            // 3) Delegate network .txt -> .json conversion via NetworkStoreLegacy
            try {
                NetworkStoreLegacy.needsImport(dataDir);
            } catch (Throwable t) {
                System.err.println("[NetworkStorePersistence] NetworkStoreLegacy migration failed: " + t.getMessage());
            }
        } catch (Exception ignored) {}
    }

    // Backward compat
    static String extractStr(String json, String field) { return JsonHelper.extractStr(json, field); }
    static String esc(String s)                          { return JsonHelper.esc(s); }

    private static String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}