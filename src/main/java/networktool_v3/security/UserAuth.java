package main.java.networktool_v3.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Benutzerverwaltung mit sicherer Passwort-Speicherung.
 *
 * Regeln:
 *  - Benutzernamen sind case-insensitiv (admin == Admin == ADMIN)
 *  - Intern werden alle Namen lowercase gespeichert
 *  - Erster angelegter Benutzer = Admin automatisch
 *  - Admins: Audit-Log, Fremdnetz-Scanner
 *
 * Algorithmus: PBKDF2WithHmacSHA256, 310.000 Iter., 256-Bit-Key, 32-Byte-Salt
 */
public final class UserAuth {

    private static final class Holder { static final UserAuth INSTANCE = new UserAuth(); }
    public static UserAuth getInstance() { return Holder.INSTANCE; }

    private static final int    ITERATIONS = 310_000;
    private static final int    KEY_LEN    = 256;
    private static final int    SALT_LEN   = 32;
    private static final String FILE_NAME  = "users.json";
    private static final String ALGO       = "PBKDF2WithHmacSHA256";

    private Path usersFile;
    private volatile String currentUser = null;

    public synchronized void init(Path txtDir) {
        try { Files.createDirectories(txtDir); } catch (IOException ignored) {}
        this.usersFile = txtDir.resolve(FILE_NAME);
    }

    // ── Öffentliche API ───────────────────────────────────────────────────

    public boolean hasUsers() {
        if (usersFile == null || !Files.exists(usersFile)) return false;
        return !loadUsers().isEmpty();
    }

    /**
     * Legt Benutzer an. Name wird zu lowercase normalisiert.
     * Duplikat-Prüfung ist case-insensitiv.
     * Erster Benutzer bekommt Rolle "admin".
     */
    public synchronized boolean createUser(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 4)
            return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = loadUsers();
        if (users.stream().anyMatch(u -> canonical.equals(u.get("username")))) return false;
        try {
            byte[] salt = generateSalt();
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("username", canonical);
            entry.put("salt",     Base64.getEncoder().encodeToString(salt));
            entry.put("hash",     hash(password, salt));
            entry.put("role",     users.isEmpty() ? "admin" : "user");
            users.add(entry);
            saveUsers(users);
            return true;
        } catch (Exception e) {
            System.err.println("[UserAuth] createUser: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        String canonical = username.trim().toLowerCase();
        for (Map<String, String> u : loadUsers()) {
            if (!canonical.equals(u.get("username"))) continue;
            try {
                byte[] salt = Base64.getDecoder().decode(u.get("salt"));
                if (MessageDigest.isEqual(
                        Base64.getDecoder().decode(u.get("hash")),
                        Base64.getDecoder().decode(hash(password, salt)))) {
                    currentUser = u.get("username");
                    return true;
                }
            } catch (Exception e) {
                System.err.println("[UserAuth] authenticate: " + e.getMessage());
            }
            return false;
        }
        return false;
    }

    /** true wenn aktueller Benutzer Admin-Rechte hat. */
    public boolean isAdmin() {
        if (currentUser == null) return false;
        return loadUsers().stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .anyMatch(u -> "admin".equals(u.get("role")));
    }

    /** "admin" oder "user". */
    public String getCurrentRole() {
        if (currentUser == null) return "user";
        return loadUsers().stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .map(u -> u.getOrDefault("role", "user"))
                .findFirst().orElse("user");
    }

    public synchronized boolean changePassword(String username, String oldPw, String newPw) {
        if (!authenticate(username, oldPw)) return false;
        if (newPw == null || newPw.length() < 4) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = loadUsers();
        for (Map<String, String> u : users) {
            if (!canonical.equals(u.get("username"))) continue;
            try {
                byte[] salt = generateSalt();
                u.put("salt", Base64.getEncoder().encodeToString(salt));
                u.put("hash", hash(newPw, salt));
                saveUsers(users);
                return true;
            } catch (Exception e) { return false; }
        }
        return false;
    }

    public synchronized boolean deleteUser(String username, String password) {
        if (!authenticate(username, password)) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = loadUsers();
        if (users.size() <= 1) return false;
        users.removeIf(u -> canonical.equals(u.get("username")));
        saveUsers(users);
        if (canonical.equals(currentUser)) currentUser = null;
        return true;
    }

    public void logout() { currentUser = null; }
    public String getCurrentUser() { return currentUser; }

    public List<String> listUsernames() {
        List<String> names = new ArrayList<>();
        loadUsers().forEach(u -> names.add(u.get("username")));
        return Collections.unmodifiableList(names);
    }

    // ── Kryptographie ─────────────────────────────────────────────────────

    private static String hash(String password, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance(ALGO);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN);
        byte[] key = f.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return Base64.getEncoder().encodeToString(key);
    }

    private static byte[] generateSalt() {
        byte[] s = new byte[SALT_LEN];
        new SecureRandom().nextBytes(s);
        return s;
    }

    // ── JSON-Persistenz ───────────────────────────────────────────────────

    private List<Map<String, String>> loadUsers() {
        if (usersFile == null || !Files.exists(usersFile)) return new ArrayList<>();
        try {
            return parseUsersJson(Files.readString(usersFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[UserAuth] Laden: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveUsers(List<Map<String, String>> users) {
        if (usersFile == null) return;
        StringBuilder sb = new StringBuilder("{\n  \"users\": [\n");
        for (int i = 0; i < users.size(); i++) {
            Map<String, String> u = users.get(i);
            sb.append("    {\n")
              .append("      \"username\": \"").append(esc(u.get("username"))).append("\",\n")
              .append("      \"salt\": \"")    .append(esc(u.get("salt"))).append("\",\n")
              .append("      \"hash\": \"")    .append(esc(u.get("hash"))).append("\",\n")
              .append("      \"role\": \"")    .append(esc(u.getOrDefault("role","user"))).append("\"\n")
              .append("    }").append(i < users.size()-1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}");
        try {
            Files.createDirectories(usersFile.getParent());
            Files.writeString(usersFile, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[UserAuth] Speichern: " + e.getMessage());
        }
    }

    private static List<Map<String, String>> parseUsersJson(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        int start = json.indexOf("\"users\"");
        if (start < 0) return result;
        int arr = json.indexOf('[', start);
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

    private static String extractStr(String json, String field) {
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
