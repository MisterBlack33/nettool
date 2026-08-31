package main.java.networktool.gui.security;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class NoteEncryptionExceptionTest {

    @AfterEach void clearSession() { NoteEncryption.clearSession(); }

    @Test void decrypt_correctPassword_returnsPlaintext() throws Exception {
        NoteEncryption.setPassword("correct-pw-123");
        String enc = NoteEncryption.encrypt("secret note");
        assertEquals("secret note", NoteEncryption.decrypt(enc, "correct-pw-123"));
    }

    @Test void decrypt_wrongPassword_throwsNoteDecryptionException() throws Exception {
        NoteEncryption.setPassword("correct-pw-123");
        String enc = NoteEncryption.encrypt("secret note");
        NoteDecryptionException ex = assertThrows(NoteDecryptionException.class,
                () -> NoteEncryption.decrypt(enc, "wrong-pw-000"));
        assertNotNull(ex.getCause());
    }

    @Test void decrypt_corruptData_throwsNoteDecryptionException() {
        assertThrows(NoteDecryptionException.class,
                () -> NoteEncryption.decrypt(NoteEncryption.PREFIX + "not-valid-base64!!", "any-pw"));
    }

    @Test void decrypt_plainText_returnsAsIs_noException() {
        assertDoesNotThrow(() -> assertEquals("plain", NoteEncryption.decrypt("plain", "any-pw")));
    }

    @Test void decrypt_null_returnsNull() {
        assertNull(NoteEncryption.decrypt(null, "any-pw"));
    }

    @Test void isEncrypted_detectsPrefix() throws Exception {
        NoteEncryption.setPassword("pw123456");
        assertTrue(NoteEncryption.isEncrypted(NoteEncryption.encrypt("x")));
        assertFalse(NoteEncryption.isEncrypted("plain"));
    }
}