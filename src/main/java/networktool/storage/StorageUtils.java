package main.java.networktool.storage;

import java.nio.file.Path;

public final class StorageUtils {

    private StorageUtils() {}

    public static Path resolveTxtDir() { return NetworkStorePersistence.resolveTxtDir(); }

    public static String extractJsonStr(String json, String field) {
        return JsonHelper.extractStr(json, field);
    }

    public static String escapeJson(String s) { return JsonHelper.esc(s); }
}