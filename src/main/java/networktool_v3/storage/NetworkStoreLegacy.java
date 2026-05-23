package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Legacy .txt file migration. Package-private. */
final class NetworkStoreLegacy {

    private NetworkStoreLegacy() {}

    static boolean needsImport(Path txtDir) {
        Path newSaved = NetworkStorePersistence.savedDir(txtDir);
        Path oldSaved = txtDir.resolve("saved");
        if (Files.isDirectory(oldSaved) && !Files.isDirectory(newSaved)) {
            try { Files.move(oldSaved, newSaved); } catch (IOException ignored) {}
        }
        if (Files.isDirectory(newSaved)) convertTxtFiles(newSaved);
        try {
            if (!Files.isDirectory(newSaved)) return true;
            return Files.list(newSaved)
                    .noneMatch(p -> p.getFileName().toString().endsWith(NetworkStorePersistence.FILE_EXT)
                            && !p.getFileName().toString().equals(NetworkStorePersistence.ALL_FILE));
        } catch (IOException e) {
            return true;
        }
    }

    private static void convertTxtFiles(Path dir) {
        try {
            Files.list(dir)
                    .filter(p -> p.getFileName().toString().endsWith(NetworkStorePersistence.LEGACY_EXT)
                            && !p.getFileName().toString().equals("all.txt"))
                    .forEach(f -> {
                        String name = stripExt(f.getFileName().toString());
                        Map<String, List<HostResult>> tmp  = new LinkedHashMap<>();
                        Map<String, String>           prfx = new LinkedHashMap<>();
                        loadFile(f, name, tmp, prfx);
                        NetworkStorePersistence.saveNetwork(dir.getParent(), name,
                                tmp.getOrDefault(name, new ArrayList<>()),
                                prfx.getOrDefault(name, ""));
                        try { Files.delete(f); } catch (IOException ignored) {}
                        System.out.println("[NetworkStore] Converted: " + name + ".txt → .json");
                    });
        } catch (IOException ignored) {}
    }

    static void loadFile(Path file, String name,
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
                HostResult h = parseLegacyLine(line);
                if (h != null) list.add(h);
            }
        } catch (IOException e) {
            System.err.println("[NetworkStore] legacy load " + name + ": " + e.getMessage());
        }
    }

    private static HostResult parseLegacyLine(String line) {
        String[] p = line.split(";", 6);
        if (p.length < 1 || p[0].isBlank()) return null;
        return new HostResult(
                p[0].trim(),
                p.length >= 2 ? p[1].trim() : p[0].trim(),
                p.length >= 3 ? p[2].trim() : "",
                p.length >= 5 ? p[4].trim() : "",
                parsePorts(p.length >= 4 ? p[3].trim() : ""),
                p.length >= 6 ? p[5].trim() : ""
        );
    }

    static Map<Integer, String> parsePorts(String s) {
        Map<Integer, String> map = new TreeMap<>();
        if (s == null || s.isBlank()) return map;
        for (String e : s.split(",")) {
            String[] kv = e.split(":", 2);
            try {
                map.put(Integer.parseInt(kv[0].trim()),
                        kv.length > 1 ? kv[1].trim() : "offen");
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private static String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}