package main.java.networktool.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.Path;
import java.security.*;
import java.util.*;

public final class UserAuth {

    private static final class Holder { static final UserAuth INSTANCE = new UserAuth(); }
    public static UserAuth getInstance() { return Holder.INSTANCE; }

    private static final int    ITERATIONS = 310_000;
    private static final int    KEY_LEN    = 256;
    private static final int    SALT_LEN   = 32;
    private static final String ALGO       = "PBKDF2WithHmacSHA256";

    private Path txtDir;
    private volatile String currentUser;

    private UserAuth() {}

    public synchronized void init(Path dir) {
        this.txtDir = dir;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public boolean hasUsers() {
        return txtDir != null && !UserAuthPersistence.load(txtDir).isEmpty();
    }

    public synchronized boolean createUser(String username, String password) {
        if (isBlank(username) || isBlank(password) || password.length() < 4) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(txtDir);
        if (users.stream().anyMatch(u -> canonical.equals(u.get("username")))) return false;
        try {
            byte[] salt = generateSalt();
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("username", canonical);
            entry.put("salt",     Base64.getEncoder().encodeToString(salt));
            entry.put("hash",     hash(password, salt));
            entry.put("role",     users.isEmpty() ? "admin" : "user");
            users.add(entry);
            UserAuthPersistence.save(txtDir, users);
            return true;
        } catch (Exception e) {
            System.err.println("[UserAuth] createUser: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        String canonical = username.trim().toLowerCase();
        for (Map<String, String> u : UserAuthPersistence.load(txtDir)) {
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

    public boolean isAdmin() {
        if (currentUser == null) return false;
        return UserAuthPersistence.load(txtDir).stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .anyMatch(u -> "admin".equals(u.get("role")));
    }

    public String getCurrentRole() {
        if (currentUser == null) return "user";
        return UserAuthPersistence.load(txtDir).stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .map(u -> u.getOrDefault("role", "user"))
                .findFirst().orElse("user");
    }

    public synchronized boolean changePassword(String username, String oldPw, String newPw) {
        if (!authenticate(username, oldPw) || isBlank(newPw) || newPw.length() < 4) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(txtDir);
        for (Map<String, String> u : users) {
            if (!canonical.equals(u.get("username"))) continue;
            try {
                byte[] salt = generateSalt();
                u.put("salt", Base64.getEncoder().encodeToString(salt));
                u.put("hash", hash(newPw, salt));
                UserAuthPersistence.save(txtDir, users);
                return true;
            } catch (Exception e) { return false; }
        }
        return false;
    }

    public synchronized boolean deleteUser(String username, String password) {
        if (!authenticate(username, password)) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(txtDir);
        if (users.size() <= 1) return false;
        users.removeIf(u -> canonical.equals(u.get("username")));
        UserAuthPersistence.save(txtDir, users);
        if (canonical.equals(currentUser)) currentUser = null;
        return true;
    }

    public void   logout()         { currentUser = null; }
    public String getCurrentUser() { return currentUser; }

    public List<String> listUsernames() {
        List<String> names = new ArrayList<>();
        UserAuthPersistence.load(txtDir).forEach(u -> names.add(u.get("username")));
        return Collections.unmodifiableList(names);
    }

    // ── Crypto ────────────────────────────────────────────────────────────

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

    // Used by AuditLogger (same package)
    static String extractStr(String json, String field) {
        return UserAuthPersistence.extractStr(json, field);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}