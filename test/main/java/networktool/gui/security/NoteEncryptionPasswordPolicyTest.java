package main.java.networktool.gui.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressionstest zu Finding 3: NoteEncryption.setPassword() akzeptierte bisher
 * jede Zeichenkette als KDF-Eingabe — schwache Passphrasen schwächen die
 * PBKDF2-Ableitung trotz hoher Iterationszahl. Jetzt wird dieselbe Policy wie
 * beim Login erzwungen (UserAuth.isStrongPassword).
 */
class NoteEncryptionPasswordPolicyTest {

    @AfterEach void clearSession() { NoteEncryption.clearSession(); }

    @Test void setPassword_tooShort_throws() {
        assertThrows(IllegalArgumentException.class, () -> NoteEncryption.setPassword("ab1"));
        assertFalse(NoteEncryption.hasSessionKey());
    }

    @Test void setPassword_noDigit_throws() {
        assertThrows(IllegalArgumentException.class, () -> NoteEncryption.setPassword("onlyletters"));
    }

    @Test void setPassword_noLetter_throws() {
        assertThrows(IllegalArgumentException.class, () -> NoteEncryption.setPassword("12345678"));
    }

    @Test void setPassword_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> NoteEncryption.setPassword(null));
    }

    @Test void setPassword_strong_accepted() throws Exception {
        NoteEncryption.setPassword("strong-pw-1");
        assertTrue(NoteEncryption.hasSessionKey());
    }
}
