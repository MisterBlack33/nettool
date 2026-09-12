package main.java.networktool.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für Standard-User-Auto-Login und die Session-Admin-Freischaltung
 * (grantSessionAdmin). Teilt sich die "userAuthSingleton"-Sperre mit den
 * übrigen UserAuth-mutierenden Testklassen (siehe SecurityTest).
 */
@Isolated
@ResourceLock("userAuthSingleton")
class UserAuthSessionAdminTest {

    private static final String ADMIN_PW = "test1234";

    @TempDir Path tmp;
    UserAuth auth;

    @BeforeEach void setup() {
        auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.logout();
        auth.seedDefaultUsers();
    }

    @AfterEach void teardown() { auth.logout(); }

    @Test void authenticateAsStandardUser_setsCurrentUserToUser() {
        auth.authenticateAsStandardUser();
        assertEquals("User", auth.getCurrentUser());
    }

    @Test void grantSessionAdmin_correctPassword_setsAdminTrue() {
        auth.authenticateAsStandardUser();
        assertTrue(auth.grantSessionAdmin(ADMIN_PW));
        assertTrue(auth.isAdmin());
    }

    @Test void grantSessionAdmin_correctPassword_roleBecomesAdmin() {
        auth.authenticateAsStandardUser();
        auth.grantSessionAdmin(ADMIN_PW);
        assertEquals("admin", auth.getCurrentRole());
    }

    @Test void grantSessionAdmin_wrongPassword_staysNonAdmin() {
        auth.authenticateAsStandardUser();
        assertFalse(auth.grantSessionAdmin("wrong-password"));
        assertFalse(auth.isAdmin());
    }

    @Test void grantSessionAdmin_nullPassword_returnsFalse() {
        auth.authenticateAsStandardUser();
        assertFalse(auth.grantSessionAdmin(null));
    }

    @Test void grantSessionAdmin_doesNotChangeCurrentUser() {
        auth.authenticateAsStandardUser();
        auth.grantSessionAdmin(ADMIN_PW);
        assertEquals("User", auth.getCurrentUser());
    }

    @Test void logout_clearsSessionAdminOverride() {
        auth.authenticateAsStandardUser();
        auth.grantSessionAdmin(ADMIN_PW);
        auth.logout();
        assertFalse(auth.isAdmin());
        assertEquals("user", auth.getCurrentRole());
    }
}