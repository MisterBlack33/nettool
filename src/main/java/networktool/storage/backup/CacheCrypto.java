package main.java.networktool.storage.backup;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM Verschlüsselung für kleine Key-Value-Cache-Einträge (z.B. ARP-,
 * OS-Erkennungs-Cache). Analog zu {@link BackupCrypto}, aber String- statt
 * datei-basiert.
 *
 * Layout: Base64(salt(16) + iv(12) + ciphertext).
 * Kein Klartext-Fallback: jeder Fehler wirft eine Exception, die der Aufrufer
 * behandeln muss (Eintrag verwerfen statt unverschlüsselt speichern).
 */
public final class CacheCrypto {

    private CacheCrypto() {}

    private static final int SALT_LEN   = 16;
    private static final int IV_LEN     = 12;
    private static final int KEY_LEN    = 256;
    private static final int ITERATIONS = 100_000;

    public static String encrypt(String plaintext, String password) throws Exception {
        byte[] salt = randomBytes(SALT_LEN);
        byte[] iv   = randomBytes(IV_LEN);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[SALT_LEN + IV_LEN + ct.length];
        System.arraycopy(salt, 0, out, 0, SALT_LEN);
        System.arraycopy(iv, 0, out, SALT_LEN, IV_LEN);
        System.arraycopy(ct, 0, out, SALT_LEN + IV_LEN, ct.length);
        return Base64.getEncoder().encodeToString(out);
    }

    public static String decrypt(String encrypted, String password) throws Exception {
        byte[] data = Base64.getDecoder().decode(encrypted);
        if (data.length < SALT_LEN + IV_LEN)
            throw new IllegalArgumentException("Eintrag zu kurz für gültigen Cache-Wert.");
        byte[] salt = Arrays.copyOfRange(data, 0, SALT_LEN);
        byte[] iv   = Arrays.copyOfRange(data, SALT_LEN, SALT_LEN + IV_LEN);
        byte[] ct   = Arrays.copyOfRange(data, SALT_LEN + IV_LEN, data.length);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
        return new String(c.doFinal(ct), StandardCharsets.UTF_8);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN))
                .getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }
}