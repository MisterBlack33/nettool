package main.java.networktool.storage;

import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;

import static org.junit.jupiter.api.Assertions.*;

class CacheCryptoTest {

    @Test void encryptDecrypt_roundtrip() throws Exception {
        String enc = CacheCrypto.encrypt("192.168.1.5=AA:BB:CC:DD:EE:FF", "pw123456");
        assertEquals("192.168.1.5=AA:BB:CC:DD:EE:FF", CacheCrypto.decrypt(enc, "pw123456"));
    }

    @Test void encrypt_producesDifferentCiphertextEachTime() throws Exception {
        String a = CacheCrypto.encrypt("same-value", "pw123456");
        String b = CacheCrypto.encrypt("same-value", "pw123456");
        assertNotEquals(a, b, "Salt/IV müssen zufällig sein");
    }

    @Test void encrypt_outputDoesNotContainPlaintext() throws Exception {
        String enc = CacheCrypto.encrypt("secret-mac-AA:BB:CC", "pw123456");
        assertFalse(enc.contains("secret-mac"));
    }

    @Test void decrypt_wrongPassword_throwsAndLeaksNothing() throws Exception {
        String enc = CacheCrypto.encrypt("topsecret", "correct-pw");
        assertThrows(AEADBadTagException.class, () -> CacheCrypto.decrypt(enc, "wrong-pw"));
    }

    @Test void decrypt_corruptData_throws() {
        assertThrows(Exception.class, () -> CacheCrypto.decrypt("not-valid-base64!!", "pw123456"));
    }

    @Test void decrypt_tooShort_throwsIllegalArgument() throws Exception {
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> CacheCrypto.decrypt(tooShort, "pw123456"));
    }

    @Test void encryptDecrypt_emptyString_roundtrip() throws Exception {
        String enc = CacheCrypto.encrypt("", "pw123456");
        assertEquals("", CacheCrypto.decrypt(enc, "pw123456"));
    }

    @Test void encryptDecrypt_unicodeContent_roundtrip() throws Exception {
        String plain = "café ü ö ß 10.0.0.1";
        String enc = CacheCrypto.encrypt(plain, "pw123456");
        assertEquals(plain, CacheCrypto.decrypt(enc, "pw123456"));
    }
}