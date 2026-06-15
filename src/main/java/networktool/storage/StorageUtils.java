package main.java.networktool.storage;

import java.nio.file.Path;

public final class StorageUtils {

    private StorageUtils() {}

    // Liefert das zentrale Datenverzeichnis (früher: "txt").
    // Bezeichner wurden von resolveTxtDir -> resolveDataDir umbenannt.
    public static Path resolveDataDir() { return NetworkStorePersistence.resolveDataDir(); }

    public static String extractJsonStr(String json, String field) {
        return JsonHelper.extractStr(json, field);
    }

    public static String escapeJson(String s) { return JsonHelper.esc(s); }
}