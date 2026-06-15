package main.java.networktool.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** JSON-Persistenz für UserAuth. Package-private. */
final class UserAuthPersistence {

    private UserAuthPersistence() {}

    static final String FILE_NAME = "users.json";

    static List<Map<String, String>> load(Path dataDir) {
        if (dataDir == null) return new ArrayList<>();
        Path file = dataDir.resolve(FILE_NAME);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            List<Map<String, String>> parsed = parse(json);
            if (!parsed.isEmpty()) return parsed;
            // Versuche Legacy-Format: {"users": [{"username":"u","password":"pw"}, ...]}
            List<Map<String, String>> legacy = legacyParse(json);
            if (!legacy.isEmpty()) {
                // Migriere: überschreibe file mit neuem Format (mit salt+hash)
                save(dataDir, legacy);
                return legacy;
            }
            return parsed;
        } catch (IOException e) {
            System.err.println("[UserAuth] load: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    static void save(Path dataDir, List<Map<String, String>> users) {
        if (dataDir == null) return;
        Path file = dataDir.resolve(FILE_NAME);
        StringBuilder sb = new StringBuilder("{\n  \"users\": [\n");
        for (int i = 0; i < users.size(); i++) {
            Map<String, String> u = users.get(i);
            sb.append("    {\n")
                    .append("      \"username\": \"").append(esc(u.get("username"))).append("\",\n")
                    .append("      \"salt\": \"")    .append(esc(u.get("salt")))    .append("\",\n")
                    .append("      \"hash\": \"")    .append(esc(u.get("hash")))    .append("\",\n")
                    .append("      \"role\": \"")    .append(esc(u.getOrDefault("role", "user"))).append("\"\n")
                    .append("    }").append(i < users.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[UserAuth] save: " + e.getMessage());
        }
    }

    private static List<Map<String, String>> parse(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        int arr = json.indexOf('[');
        if (arr < 0) return result;
        int depth = 0, objStart = -1;
        for (int i = arr; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth++ == 0) objStart = i; }
            else if (c == '}' && --depth == 0 && objStart >= 0) {
                String obj = json.substring(objStart, i + 1);
                Map<String, String> m = new LinkedHashMap<>();
                m.put("username", extractStr(obj, "username"));
                m.put("salt",     extractStr(obj, "salt"));
                m.put("hash",     extractStr(obj, "hash"));
                m.put("role",     nvl(extractStr(obj, "role"), "user"));
                if (m.get("username") != null && m.get("hash") != null) result.add(m);
                objStart = -1;
            }
        }
        return result;
    }

    /** Einfache Legacy-Erkennung: sucht nach username+password Paaren und erzeugt salt/hash */
    private static List<Map<String, String>> legacyParse(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        int idx = 0;
        while (idx < json.length()) {
            int u = json.indexOf("\"username\"", idx);
            if (u < 0) break;
            String username = extractStr(json.substring(u - 20, Math.min(json.length(), u + 200)), "username");
            String password = extractStr(json.substring(u - 20, Math.min(json.length(), u + 200)), "password");
            if (username != null && password != null) {
                try {
                    byte[] salt = main.java.networktool.security.UserAuth.generateSalt();
                    Map<String,String> m = new LinkedHashMap<>();
                    m.put("username", username.trim().toLowerCase());
                    m.put("salt", Base64.getEncoder().encodeToString(salt));
                    m.put("hash", main.java.networktool.security.UserAuth.hash(password, salt));
                    m.put("role", "user");
                    result.add(m);
                } catch (Exception e) { /* ignore malformed entries */ }
            }
            idx = u + 10;
        }
        return result;
    }

    static String extractStr(String json, String field) {
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int s = colon + 1;
        while (s < json.length() && json.charAt(s) == ' ') s++;
        if (s >= json.length() || json.charAt(s) != '"') return null;
        s++;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) { sb.append(json.charAt(++i)); }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}