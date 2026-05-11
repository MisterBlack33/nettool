package networktool_v3.security;

import main.java.networktool_v3.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für UserAuth:
 *  - Benutzer anlegen, anmelden, löschen
 *  - Case-insensitive Duplikat-Prüfung
 *  - Admin-Rolle (erster Benutzer)
 *  - Passwort-Änderung
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserAuthTest {

    @TempDir
    Path tempDir;

    private UserAuth auth;

    @BeforeEach
    void setUp() {
        auth = UserAuth.getInstance();
        auth.init(tempDir);
        auth.logout();
    }

    @Test @Order(1)
    void firstUserBecomesAdmin() {
        assertTrue(auth.createUser("admin", "password123"));
        auth.authenticate("admin", "password123");
        assertTrue(auth.isAdmin());
        assertEquals("admin", auth.getCurrentRole());
    }

    @Test @Order(2)
    void secondUserIsNotAdmin() {
        auth.createUser("admin", "password123");
        auth.createUser("bob", "password456");
        auth.authenticate("bob", "password456");
        assertFalse(auth.isAdmin());
        assertEquals("user", auth.getCurrentRole());
    }

    @Test @Order(3)
    void caseInsensitiveDuplicateRejected() {
        assertTrue(auth.createUser("admin", "password123"));
        assertFalse(auth.createUser("Admin", "other"));
        assertFalse(auth.createUser("ADMIN", "other"));
        assertFalse(auth.createUser("aDmIn", "other"));
    }

    @Test @Order(4)
    void authenticateSucceeds() {
        auth.createUser("alice", "secret99");
        assertTrue(auth.authenticate("alice", "secret99"));
        assertEquals("alice", auth.getCurrentUser());
    }

    @Test @Order(5)
    void authenticateFailsWrongPassword() {
        auth.createUser("alice", "secret99");
        assertFalse(auth.authenticate("alice", "wrongpass"));
        assertNull(auth.getCurrentUser());
    }

    @Test @Order(6)
    void authenticateCaseInsensitiveUsername() {
        auth.createUser("alice", "secret99");
        assertTrue(auth.authenticate("Alice", "secret99"));
        assertTrue(auth.authenticate("ALICE", "secret99"));
    }

    @Test @Order(7)
    void changePasswordWorks() {
        auth.createUser("alice", "oldpass");
        auth.authenticate("alice", "oldpass");
        assertTrue(auth.changePassword("alice", "oldpass", "newpass123"));
        auth.logout();
        assertTrue(auth.authenticate("alice", "newpass123"));
        assertFalse(auth.authenticate("alice", "oldpass"));
    }

    @Test @Order(8)
    void deleteUserWorks() {
        auth.createUser("admin", "admin123");
        auth.createUser("bob", "bob123");
        auth.authenticate("bob", "bob123");
        assertTrue(auth.deleteUser("bob", "bob123"));
        assertFalse(auth.authenticate("bob", "bob123"));
    }

    @Test @Order(9)
    void cannotDeleteLastUser() {
        auth.createUser("admin", "admin123");
        auth.authenticate("admin", "admin123");
        assertFalse(auth.deleteUser("admin", "admin123"));
    }

    @Test @Order(10)
    void listUsernamesNormalized() {
        auth.createUser("Alice", "pass1");
        auth.createUser("Bob", "pass2");
        List<String> names = auth.listUsernames();
        assertTrue(names.contains("alice"));
        assertTrue(names.contains("bob"));
    }

    @Test @Order(11)
    void hasUsersReturnsFalseWhenEmpty() {
        assertFalse(auth.hasUsers());
    }

    @Test @Order(12)
    void hasUsersReturnsTrueAfterCreate() {
        auth.createUser("admin", "admin123");
        assertTrue(auth.hasUsers());
    }

    @Test @Order(13)
    void logoutClearsCurrentUser() {
        auth.createUser("admin", "admin123");
        auth.authenticate("admin", "admin123");
        assertNotNull(auth.getCurrentUser());
        auth.logout();
        assertNull(auth.getCurrentUser());
    }

    @Test @Order(14)
    void shortPasswordRejected() {
        assertFalse(auth.createUser("user", "abc")); // < 4 chars
    }

    @Test @Order(15)
    void blankUsernameRejected() {
        assertFalse(auth.createUser("", "password123"));
        assertFalse(auth.createUser("  ", "password123"));
        assertFalse(auth.createUser(null, "password123"));
    }
}
