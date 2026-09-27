package main.java.networktool.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Regressionstest zu Finding 1: grantSessionAdmin() muss nach wiederholten Fehlversuchen sperren. */
@Isolated
@ResourceLock("userAuthSingleton")
class UserAuthSessionAdminLockoutTest {

    private static final String ADMIN_PW = "test1234";

    @TempDir Path tmp;
    UserAuth auth;

    @BeforeEach void setup() {
        auth = UserAuth.getInstance();
        auth.init(tmp); // resettet auch den Rate-Limiter
        auth.logout();
        auth.seedDefaultUsers();
        auth.authenticateAsStandardUser();
    }

    @AfterEach void teardown() { auth.logout(); }

    @Test void repeatedWrongPassword_locksOutFurtherAttempts() {
        for (int i = 0; i < SessionAdminRateLimiter.MAX_ATTEMPTS; i++)
            assertFalse(auth.grantSessionAdmin("wrong-" + i));
        // Sperre aktiv: selbst das korrekte Passwort wird jetzt abgelehnt
        assertFalse(auth.grantSessionAdmin(ADMIN_PW));
        assertFalse(auth.isAdmin());
    }

    @Test void successfulGrant_resetsFailureCounter() {
        auth.grantSessionAdmin("wrong");
        assertTrue(auth.grantSessionAdmin(ADMIN_PW));
        assertTrue(auth.isAdmin());
    }

    @Test void belowThreshold_stillAllowsCorrectPassword() {
        auth.grantSessionAdmin("wrong");
        auth.grantSessionAdmin("wrong");
        assertTrue(auth.grantSessionAdmin(ADMIN_PW));
    }
}
