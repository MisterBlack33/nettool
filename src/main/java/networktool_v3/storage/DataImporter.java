package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class DataImporter {

    private DataImporter() {}

    public static int importCsv(Path file) throws IOException {
        int count = 0;
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("IP;")) continue;
            String[] p = line.split(";", 7);
            if (p.length < 1 || p[0].isBlank()) continue;
            String ip    = p[0].trim();
            String hn    = p.length > 1 ? p[1].trim() : ip;
            String os    = p.length > 2 ? p[2].trim() : "";
            String date  = p.length > 3 ? p[3].trim() : "";
            String notes = p.length > 5 ? p[5].trim() : "";
            String cat   = p.length > 6 ? p[6].trim() : "Import";
            if (ip.isBlank()) continue;
            if (saveHost(ip, hn, os, date, notes, cat)) count++;
        }
        return count;
    }

    public static int importJson(Path file) throws IOException {
        String content = Files.readString(file);
        int count = 0;
        for (String obj : content.split("\\},\\s*\\{")) {
            // FIX: delegate to JsonHelper instead of own copy
            String ip    = JsonHelper.extractStr(obj, "ip");
            String hn    = JsonHelper.extractStr(obj, "hostname");
            String os    = JsonHelper.extractStr(obj, "os");
            String date  = JsonHelper.extractStr(obj, "savedAt");
            String notes = JsonHelper.extractStr(obj, "notes");
            String cat   = JsonHelper.extractStr(obj, "category");
            if (ip == null || ip.isBlank()) continue;
            if (saveHost(ip, nvl(hn, ip), nvl(os, ""), date, nvl(notes, ""), nvl(cat, "Import"))) count++;
        }
        return count;
    }

    public static int restoreBackup(Path zipFile) throws IOException {
        Path targetDir = NetworkStorePersistence.resolveTxtDir();
        int[] count = {0};
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = targetDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(targetDir)) continue; // Zip-Slip-Schutz
                Files.createDirectories(dest.getParent());
                Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                count[0]++;
            }
        }
        return count[0];
    }

    private static boolean saveHost(String ip, String hn, String os,
                                    String date, String notes, String cat) {
        NetworkStore store = NetworkStore.getInstance();
        if (!store.getNetworkNames().contains(cat))
            store.createNetwork(cat, "");
        return store.save(new HostResult(ip, hn, os, date, null, notes), cat);
    }

    private static String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}