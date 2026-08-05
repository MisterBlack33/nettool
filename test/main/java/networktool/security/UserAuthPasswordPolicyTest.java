package main.java.networktool.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthPasswordPolicyTest {

    @TempDir Path tmp;
    UserAuth auth;

    @BeforeEach void setup() {
        auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.logout();
    }

    @Test void isStrongPassword_shortRejected()        { assertFalse(UserAuth.isStrongPassword("ab1234")); }
    @Test void isStrongPassword_noDigitRejected()       { assertFalse(UserAuth.isStrongPassword("abcdefgh")); }
    @Test void isStrongPassword_noLetterRejected()      { assertFalse(UserAuth.isStrongPassword("12345678")); }
    @Test void isStrongPassword_null_rejected()         { assertFalse(UserAuth.isStrongPassword(null)); }
    @Test void isStrongPassword_valid_accepted()        { assertTrue(UserAuth.isStrongPassword("abcd1234")); }

    @Test void createUser_shortPassword_rejected() {
        assertFalse(auth.createUser("bob", "ab12"));
    }

    @Test void createUser_noComplexity_rejected() {
        assertFalse(auth.createUser("bob", "abcdefgh"));
    }

    @Test void createUser_meetsPolicy_accepted() {
        assertTrue(auth.createUser("bob", "goodpw12"));
    }

    @Test void changePassword_weakNewPassword_rejected() {
        auth.createUser("carol", "startpw12");
        auth.authenticate("carol", "startpw12");
        assertFalse(auth.changePassword("carol", "startpw12", "weak"));
    }

    @Test void changePassword_meetsPolicy_accepted() {
        auth.createUser("carol", "startpw12");
        auth.authenticate("carol", "startpw12");
        assertTrue(auth.changePassword("carol", "startpw12", "newgoodpw1"));
        assertTrue(auth.authenticate("carol", "newgoodpw1"));
    }
}