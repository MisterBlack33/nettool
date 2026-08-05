package main.java.networktool.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.AEADBadTagException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class BackupCryptoTest {

    @TempDir Path tmp;

    @Test void encryptDecrypt_roundtrip() throws Exception {
        Path plain = tmp.resolve("data.bin");
        Files.write(plain, "hello world".getBytes());
        Path enc = tmp.resolve("data.enc");
        Path dec = tmp.resolve("data.dec");

        BackupCrypto.encryptFile(plain, enc, "pw12345");
        BackupCrypto.decryptFile(enc, dec, "pw12345");

        assertEquals("hello world", Files.readString(dec));
    }

    @Test void encrypt_producesDifferentBytes() throws Exception {
        Path plain = tmp.resolve("data.bin");
        Files.write(plain, "secret".getBytes());
        Path enc = tmp.resolve("data.enc");
        BackupCrypto.encryptFile(plain, enc, "pw12345");
        assertFalse(Files.readString(enc, java.nio.charset.StandardCharsets.ISO_8859_1).contains("secret"));
    }

    @Test void decrypt_wrongPassword_throws() throws Exception {
        Path plain = tmp.resolve("data.bin");
        Files.write(plain, "secret".getBytes());
        Path enc = tmp.resolve("data.enc");
        BackupCrypto.encryptFile(plain, enc, "correct-pw");
        Path dec = tmp.resolve("data.dec");
        assertThrows(AEADBadTagException.class,
                () -> BackupCrypto.decryptFile(enc, dec, "wrong-pw"));
    }

    @Test void decrypt_tooShortFile_throwsIOException() throws Exception {
        Path tiny = tmp.resolve("tiny.enc");
        Files.write(tiny, new byte[]{1, 2, 3});
        assertThrows(java.io.IOException.class,
                () -> BackupCrypto.decryptFile(tiny, tmp.resolve("out.dec"), "pw"));
    }

    @Test void exportEncryptedBackup_thenRestore_roundtrip() throws Exception {
        Path outDir = tmp.resolve("out");
        Path enc = DataExporter.exportEncryptedBackup(outDir, "pw12345");
        assertTrue(Files.exists(enc));
        assertTrue(enc.getFileName().toString().endsWith(BackupCryptoAccess.suffix()));

        int count = DataImporter.restoreEncryptedBackup(enc, "pw12345");
        assertTrue(count >= 0);
    }

    @Test void exportEncryptedBackup_doesNotLeavePlainZip() throws Exception {
        Path outDir = tmp.resolve("out2");
        DataExporter.exportEncryptedBackup(outDir, "pw12345");
        try (var files = Files.list(outDir)) {
            assertTrue(files.allMatch(p -> p.getFileName().toString().endsWith(".enc")));
        }
    }

    /** Kleiner Zugriffshelfer, da ENCRYPTED_SUFFIX package-private ist. */
    static final class BackupCryptoAccess {
        static String suffix() { return BackupCrypto.ENCRYPTED_SUFFIX; }
    }
}
