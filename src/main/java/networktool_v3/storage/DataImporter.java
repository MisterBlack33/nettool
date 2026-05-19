package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public final class DataImporter {

    private DataImporter() {}

    public static int importCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        int count = 0;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("IP;")) continue;
            String[] p = line.split(";", 7);
            if (p.length < 1) continue;
            String ip    = p[0].trim();
            String hn    = p.length > 1 ? p[1].trim() : ip;
            String os    = p.length > 2 ? p[2].trim() : "";
            String date  = p.length > 3 ? p[3].trim() : "";
            String notes = p.length > 5 ? p[5].trim() : "";
            String cat   = p.length > 6 ? p[6].trim() : "Import";
            if (ip.isBlank()) continue;
            if (NetworkStore.getInstance().save(
                    new HostResult(ip, hn, os, date, null, notes),
                    ensureNetwork(cat))) count++;
        }
        return count;
    }

    public static int importJson(Path file) throws IOException {
        String content = Files.readString(file);
        int count = 0;
        for (String obj : content.split("\\},\\s*\\{")) {
            String ip    = extractJsonStr(obj, "ip");
            String hn    = extractJsonStr(obj, "hostname");
            String os    = extractJsonStr(obj, "os");
            String date  = extractJsonStr(obj, "savedAt");
            String notes = extractJsonStr(obj, "notes");
            String cat   = extractJsonStr(obj, "category");
            if (ip == null || ip.isBlank()) continue;
            if (cat == null || cat.isBlank()) cat = "Import";
            if (NetworkStore.getInstance().save(
                    new HostResult(ip, hn != null ? hn : ip, os != null ? os : "",
                            date, null, notes != null ? notes : ""),
                    ensureNetwork(cat))) count++;
        }
        return count;
    }

    public static int restoreBackup(Path zipFile) throws IOException {
        Path targetDir = NetworkStorePersistence.resolveTxtDir();
        int[] count = {0};
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new FileInputStream(zipFile.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = targetDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(targetDir)) continue;
                Files.createDirectories(dest.getParent());
                Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                count[0]++;
            }
        }
        return count[0];
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String ensureNetwork(String cat) {
        if (!NetworkStore.getInstance().getNetworkNames().contains(cat))
            NetworkStore.getInstance().createNetwork(cat, "");
        return cat;
    }

    private static String extractJsonStr(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"'))
            if (json.charAt(start++) == '"') break;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) { sb.append(json.charAt(++i)); }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }
}