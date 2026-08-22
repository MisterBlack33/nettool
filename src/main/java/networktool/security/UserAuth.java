package main.java.networktool.security;

import main.java.networktool.storage.StorageUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.Path;
import java.security.*;
import java.util.*;

public final class UserAuth {

    private static final class Holder { static final UserAuth INSTANCE = new UserAuth(); }
    public static UserAuth getInstance() { return Holder.INSTANCE; }

    private static final int    ITERATIONS   = 310_000;
    private static final int    KEY_LEN      = 256;
    private static final int    SALT_LEN     = 32;
    private static final String ALGO         = "PBKDF2WithHmacSHA256";
    /** Passwort-Policy: mind. 8 Zeichen, mind. ein Buchstabe und eine Ziffer. */
    private static final int    MIN_PW_LEN   = 8;

    private Path dataDir;
    private volatile String currentUser;

    // Default-Konten: Hash wird beim Seeding zur Laufzeit aus dem Klartext-Passwort
    // berechnet (siehe seedDefaultUsers()) statt stale vorberechneter Base64-Werte,
    // damit die dokumentierten Zugangsdaten tatsächlich funktionieren.
    private static final String DEFAULT_ADMIN_USER     = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin1234";

    private static final String DEFAULT_USER_USER      = "user1";
    private static final String DEFAULT_USER_PASSWORD  = "test1234";

    private UserAuth() {}

    public synchronized void init(Path dir) {
        // Wenn kein Verzeichnis übergeben wurde, verwende das zentrale Datenverzeichnis
        if (dir == null) this.dataDir = StorageUtils.resolveDataDir();
        else this.dataDir = dir;
    }

    public synchronized void seedDefaultUsers() {
        if (hasUsers()) return;
        try {
            List<Map<String, String>> users = new ArrayList<>();
            users.add(seedEntry(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD, "admin"));
            users.add(seedEntry(DEFAULT_USER_USER,  DEFAULT_USER_PASSWORD,  "user"));
            UserAuthPersistence.save(dataDir, users);
        } catch (Exception e) {
            System.err.println("[UserAuth] seedDefaultUsers: " + e.getMessage());
        }
    }

    private static Map<String, String> seedEntry(String user, String password, String role) throws Exception {
        byte[] salt = generateSalt();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("username", user);
        m.put("salt", Base64.getEncoder().encodeToString(salt));
        m.put("hash", hash(password, salt));
        m.put("role", role);
        return m;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public boolean hasUsers() {
        return dataDir != null && !UserAuthPersistence.load(dataDir).isEmpty();
    }

    public synchronized boolean createUser(String username, String password) {
        if (isBlank(username) || !isStrongPassword(password)) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(dataDir);
        if (users.stream().anyMatch(u -> canonical.equals(u.get("username")))) return false;
        try {
            byte[] salt = generateSalt();
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("username", canonical);
            entry.put("salt",     Base64.getEncoder().encodeToString(salt));
            entry.put("hash",     hash(password, salt));
            entry.put("role",     users.isEmpty() ? "admin" : "user");
            users.add(entry);
            UserAuthPersistence.save(dataDir, users);
            return true;
        } catch (Exception e) {
            System.err.println("[UserAuth] createUser: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        String canonical = username.trim().toLowerCase();
        for (Map<String, String> u : UserAuthPersistence.load(dataDir)) {
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
        return UserAuthPersistence.load(dataDir).stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .anyMatch(u -> "admin".equals(u.get("role")));
    }

    public String getCurrentRole() {
        if (currentUser == null) return "user";
        return UserAuthPersistence.load(dataDir).stream()
                .filter(u -> currentUser.equals(u.get("username")))
                .map(u -> u.getOrDefault("role", "user"))
                .findFirst().orElse("user");
    }

    public synchronized boolean changePassword(String username, String oldPw, String newPw) {
        if (!authenticate(username, oldPw) || !isStrongPassword(newPw)) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(dataDir);
        for (Map<String, String> u : users) {
            if (!canonical.equals(u.get("username"))) continue;
            try {
                byte[] salt = generateSalt();
                u.put("salt", Base64.getEncoder().encodeToString(salt));
                u.put("hash", hash(newPw, salt));
                UserAuthPersistence.save(dataDir, users);
                return true;
            } catch (Exception e) { return false; }
        }
        return false;
    }

    public synchronized boolean deleteUser(String username, String password) {
        if (!authenticate(username, password)) return false;
        String canonical = username.trim().toLowerCase();
        List<Map<String, String>> users = UserAuthPersistence.load(dataDir);
        if (users.size() <= 1) return false;
        users.removeIf(u -> canonical.equals(u.get("username")));
        UserAuthPersistence.save(dataDir, users);
        if (canonical.equals(currentUser)) currentUser = null;
        return true;
    }

    public void   logout()         { currentUser = null; }
    public String getCurrentUser() { return currentUser; }

    public List<String> listUsernames() {
        List<String> names = new ArrayList<>();
        UserAuthPersistence.load(dataDir).forEach(u -> names.add(u.get("username")));
        return Collections.unmodifiableList(names);
    }

    // ── Passwort-Policy ───────────────────────────────────────────────────

    /** Mind. 8 Zeichen, mind. ein Buchstabe und eine Ziffer. */
    public static boolean isStrongPassword(String pw) {
        return pw != null && pw.length() >= MIN_PW_LEN
                && pw.matches(".*[A-Za-z].*")
                && pw.matches(".*[0-9].*");
    }

    // ── Crypto ────────────────────────────────────────────────────────────

    static String hash(String password, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance(ALGO);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN);
        byte[] key = f.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return Base64.getEncoder().encodeToString(key);
    }

    static byte[] generateSalt() {
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