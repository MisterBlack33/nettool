package main.java.networktool.storage;

import java.nio.file.Path;

public final class StorageLocationsResolver {

    private StorageLocationsResolver() {}

    /** Standard-Datenverzeichnis für Netzwerk-nahe Stores (saves/networkdata). */
    public static Path resolveDataDir() { return StorageLocations.networkData(); }

    public static String extractJsonStr(String json, String field) {
        return JsonCodec.extractStr(json, field);
    }

    public static String escapeJson(String s) { return JsonCodec.esc(s); }
}
