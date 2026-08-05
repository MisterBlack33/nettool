package main.java.networktool.storage;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * AES-256-GCM Verschlüsselung für Backup-ZIPs (gleicher Ansatz wie
 * NoteEncryption, aber dateibasiert statt String-basiert).
 * Layout: salt(16) + iv(12) + ciphertext, roh in Datei geschrieben (kein Base64 nötig).
 */
final class BackupCrypto {

    private BackupCrypto() {}

    private static final int SALT_LEN   = 16;
    private static final int IV_LEN     = 12;
    private static final int KEY_LEN    = 256;
    private static final int ITERATIONS = 100_000;
    static final String ENCRYPTED_SUFFIX = ".enc";

    static void encryptFile(Path plainZip, Path outEncrypted, String password) throws Exception {
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(Files.readAllBytes(plainZip));

        byte[] out = new byte[SALT_LEN + IV_LEN + ct.length];
        System.arraycopy(salt, 0, out, 0, SALT_LEN);
        System.arraycopy(iv, 0, out, SALT_LEN, IV_LEN);
        System.arraycopy(ct, 0, out, SALT_LEN + IV_LEN, ct.length);
        Files.write(outEncrypted, out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void decryptFile(Path encrypted, Path outPlainZip, String password) throws Exception {
        byte[] data = Files.readAllBytes(encrypted);
        if (data.length < SALT_LEN + IV_LEN) throw new IOException("Datei zu kurz für gültiges Backup.");
        byte[] salt = java.util.Arrays.copyOfRange(data, 0, SALT_LEN);
        byte[] iv   = java.util.Arrays.copyOfRange(data, SALT_LEN, SALT_LEN + IV_LEN);
        byte[] ct   = java.util.Arrays.copyOfRange(data, SALT_LEN + IV_LEN, data.length);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
        Files.write(outPlainZip, c.doFinal(ct), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt)
            throws java.security.NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN)).getEncoded();
        return new SecretKeySpec(key, "AES");
    }
}
