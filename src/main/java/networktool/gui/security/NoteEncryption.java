package main.java.networktool.gui.security;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * AES-256-GCM Verschlüsselung für Notizen.
 * Salt + IV werden als Teil des Strings gespeichert: PREFIX + Base64(salt+iv+ct)
 */
public final class NoteEncryption {

    private NoteEncryption() {}

    private static final int    SALT_LEN   = 16;
    private static final int    IV_LEN     = 12;
    private static final int    KEY_LEN    = 256;
    private static final int    ITERATIONS = 100_000;
    public  static final String PREFIX     = "ENC:";

    private static volatile SecretKey sessionKey  = null;
    private static volatile byte[]    sessionSalt = null;

    public static boolean hasSessionKey() { return sessionKey != null; }

    public static void setPassword(String password) throws Exception {
        sessionSalt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(sessionSalt);
        sessionKey = deriveKey(password, sessionSalt);
    }

    public static void clearSession() { sessionKey = null; sessionSalt = null; }

    public static String encrypt(String plaintext) {
        if (sessionKey == null || plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, sessionKey, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[SALT_LEN + IV_LEN + ct.length];
            System.arraycopy(sessionSalt, 0, out, 0,                  SALT_LEN);
            System.arraycopy(iv,          0, out, SALT_LEN,           IV_LEN);
            System.arraycopy(ct,          0, out, SALT_LEN + IV_LEN,  ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) { return plaintext; }
    }

    public static String decrypt(String encrypted, String password) {
        if (encrypted == null || !encrypted.startsWith(PREFIX)) return encrypted;
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            byte[] salt = java.util.Arrays.copyOfRange(combined, 0, SALT_LEN);
            byte[] iv   = java.util.Arrays.copyOfRange(combined, SALT_LEN, SALT_LEN + IV_LEN);
            byte[] ct   = java.util.Arrays.copyOfRange(combined, SALT_LEN + IV_LEN, combined.length);
            SecretKey key = deriveKey(password, salt);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            return "[Falsches Passwort]";
        } catch (Exception e) {
            return "[Entschlusselung fehlgeschlagen]";
        }
    }

    public static boolean isEncrypted(String s) { return s != null && s.startsWith(PREFIX); }

    private static SecretKey deriveKey(String pw, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(
                f.generateSecret(new PBEKeySpec(pw.toCharArray(), salt, ITERATIONS, KEY_LEN))
                        .getEncoded(), "AES");
    }
}