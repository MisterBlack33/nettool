package main.java.networktool.storage;

import java.nio.file.Path;

public final class StorageUtils {

    private StorageUtils() {}

    /** Standard-Datenverzeichnis für Netzwerk-nahe Stores (saves/networkdata). */
    public static Path resolveDataDir() { return StorageLocations.networkData(); }

    public static String extractJsonStr(String json, String field) {
        return JsonHelper.extractStr(json, field);
    }

    public static String escapeJson(String s) { return JsonHelper.esc(s); }
}