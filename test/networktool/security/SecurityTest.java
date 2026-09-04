package networktool.security;

import main.java.networktool.security.AuditLogEntry;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserAuth and AuditLogger.
 *
 * UserAuth/AuditLogger are process-wide singletons, and AuditLogger.currentUser()
 * reads UserAuth.getInstance().getCurrentUser(). @Isolated on a @Nested class does
 * NOT reliably exclude other top-level test classes (MainTest,
 * UserAuthPasswordPolicyTest, the standalone AuditLoggerTest) that also mutate these
 * singletons — so every class touching them must additionally take the same named
 * @ResourceLock to get real mutual exclusion across the whole suite.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("userAuthSingleton")
class SecurityTest {

    // ── UserAuth ──────────────────────────────────────────────────────────

    @Isolated
    @Execution(ExecutionMode.SAME_THREAD)
    @ResourceLock("userAuthSingleton")
    @Nested
    class UserAuthTest {

        @TempDir Path tmp;
        UserAuth auth;

        @BeforeEach
        void setup() {
            auth = UserAuth.getInstance();
            auth.init(tmp);
            auth.logout();
        }

        @Test void hasUsers_empty()                        { assertFalse(auth.hasUsers()); }
        @Test void createUser_success()                    { assertTrue(auth.createUser("alice", "secret12")); assertTrue(auth.hasUsers()); }
        @Test void createUser_shortPassword_rejected()     { assertFalse(auth.createUser("bob", "ab")); }
        @Test void createUser_blankName_rejected()         { assertFalse(auth.createUser("  ", "password1")); }

        @Test void createUser_duplicate_caseInsensitive_rejected() {
            auth.createUser("Alice", "pass1234");
            assertFalse(auth.createUser("alice", "other123"));
        }

        @Test void authenticate_correct() {
            auth.createUser("carol", "mypass12");
            assertTrue(auth.authenticate("carol", "mypass12"));
            assertEquals("carol", auth.getCurrentUser());
        }

        @Test void authenticate_wrongPassword()            { auth.createUser("dave", "right123"); assertFalse(auth.authenticate("dave", "wrong1")); }
        @Test void authenticate_unknownUser()              { assertFalse(auth.authenticate("nobody", "pass123")); }

        @Test void authenticate_caseInsensitiveUsername() {
            auth.createUser("Eve", "pass1234");
            assertTrue(auth.authenticate("EVE", "pass1234"));
        }

        @Test void firstUser_isAdmin() {
            auth.createUser("admin1", "admin123");
            auth.authenticate("admin1", "admin123");
            assertTrue(auth.isAdmin());
        }

        @Test void secondUser_isNotAdmin() {
            auth.createUser("admin1", "admin123");
            auth.createUser("user1",  "user1234");
            auth.authenticate("user1", "user1234");
            assertFalse(auth.isAdmin());
        }

        @Test void logout_clearsCurrentUser() {
            auth.createUser("frank", "frank123");
            auth.authenticate("frank", "frank123");
            auth.logout();
            assertNull(auth.getCurrentUser());
        }

        @Test void changePassword_success() {
            auth.createUser("grace", "old12345");
            auth.authenticate("grace", "old12345");
            assertTrue(auth.changePassword("grace", "old12345", "new12345"));
            assertTrue(auth.authenticate("grace", "new12345"));
        }

        @Test void changePassword_wrongOld_fails() {
            auth.createUser("hal", "pass1234");
            auth.authenticate("hal", "pass1234");
            assertFalse(auth.changePassword("hal", "wrong12", "new1234"));
        }

        @Test void deleteUser_success() {
            auth.createUser("ira", "pass1234");
            auth.createUser("joe", "pass4567");
            auth.authenticate("joe", "pass4567");
            assertTrue(auth.deleteUser("joe", "pass4567"));
        }

        @Test void deleteUser_lastUser_rejected() {
            auth.createUser("solo", "pass1234");
            auth.authenticate("solo", "pass1234");
            assertFalse(auth.deleteUser("solo", "pass1234"));
        }

        @Test void listUsernames_returnsAll() {
            auth.createUser("u1", "pass1111");
            auth.createUser("u2", "pass2222");
            List<String> names = auth.listUsernames();
            assertTrue(names.contains("u1"));
            assertTrue(names.contains("u2"));
        }

        @Test void getCurrentRole_admin() {
            auth.createUser("root", "root1234");
            auth.authenticate("root", "root1234");
            assertEquals("admin", auth.getCurrentRole());
        }

        @Test void getCurrentRole_user() {
            auth.createUser("root",    "root1234");
            auth.createUser("regular", "reg12345");
            auth.authenticate("regular", "reg12345");
            assertEquals("user", auth.getCurrentRole());
        }

        @Test void isAdmin_notLoggedIn()             { assertFalse(auth.isAdmin()); }

        @Test void persistence_survivesReinit() {
            auth.createUser("persistent", "pass1234");
            auth.init(tmp);
            assertTrue(auth.authenticate("persistent", "pass1234"));
        }
    }

    // ── AuditLogger ───────────────────────────────────────────────────────
    // @Isolated: this class also drives UserAuth (login as "admin" so clear()
    // is permitted). Without isolation it runs concurrently with the sibling
    // UserAuthTest and both fight over the same UserAuth singleton state.

    @Isolated
    @Execution(ExecutionMode.SAME_THREAD)
    @ResourceLock("userAuthSingleton")
    @Nested
    class AuditLoggerTest {

        @TempDir Path tmp;
        AuditLogger logger;

        @BeforeEach
        void setup() throws InterruptedException {
            logger = AuditLogger.getInstance();
            logger.init(tmp);
            // Admin einloggen damit clear() funktioniert
            UserAuth.getInstance().init(tmp);
            UserAuth.getInstance().createUser("admin", "admin123");
            UserAuth.getInstance().authenticate("admin", "admin123");
            logger.clear();
            Thread.sleep(100);
        }

        @AfterEach
        void teardown() { UserAuth.getInstance().logout(); }

        @Test void log_createsFile() throws InterruptedException {
            logger.log("TEST_ACTION", "detail1");
            Thread.sleep(300);
            assertTrue(Files.exists(tmp.resolve("audit.log")));
        }

        @Test void log_singleParam() throws InterruptedException {
            logger.log("SIMPLE");
            Thread.sleep(300);
            assertTrue(logger.readRecent(10).stream().anyMatch(e -> "SIMPLE".equals(e.action())));
        }

        @Test void log_withDetail() throws InterruptedException {
            logger.log("ACTION", "someDetail");
            Thread.sleep(300);
            assertTrue(logger.readRecent(10).stream()
                    .anyMatch(e -> "ACTION".equals(e.action()) && "someDetail".equals(e.detail())));
        }

        @Test void readRecent_respects_maxLines() throws InterruptedException {
            for (int i = 0; i < 10; i++) logger.log("FILL", "x" + i);
            Thread.sleep(400);
            assertTrue(logger.readRecent(3).size() <= 3);
        }

        @Test void readRecent_emptyFile_returnsEmpty() throws IOException {
            Path log = tmp.resolve("audit.log");
            Files.writeString(log, "", StandardCharsets.UTF_8);
            assertTrue(logger.readRecent(100).isEmpty());
        }

        @Test void clear_removesEntries() throws InterruptedException {
            logger.log("BEFORE", "x");
            Thread.sleep(300);
            logger.clear();
            Thread.sleep(200);
            assertTrue(logger.readRecent(100).stream()
                    .allMatch(e -> "AUDIT_LOG_CLEARED".equals(e.action())));
        }

        @Test void readByUser_filtersCorrectly() throws InterruptedException {
            UserAuth.getInstance().createUser("loguser", "pass1234");
            UserAuth.getInstance().authenticate("loguser", "pass1234");
            logger.log("USER_ACTION", "by loguser");
            Thread.sleep(300);
            assertTrue(logger.readByUser("loguser").stream()
                    .anyMatch(e -> "USER_ACTION".equals(e.action())));
        }

        @Test void parse_ndjsonFormat() {
            String line = "{\"v\":1,\"ts\":\"2024-01-01 10:00:00\",\"user\":\"user1\",\"action\":\"LOGIN\",\"detail\":\"detail\"}";
            AuditLogEntry e = AuditLogger.parse(line);
            assertNotNull(e);
            assertEquals("user1", e.user());
            assertEquals("LOGIN", e.action());
            assertEquals("detail", e.detail());
        }

        @Test void parse_invalidLine_returnsNull() {
            assertNull(AuditLogger.parse(""));
            assertNull(AuditLogger.parse(null));
            assertNull(AuditLogger.parse("only one field"));
        }

        @Test void multipleLogCalls_allPersisted() throws InterruptedException {
            for (int i = 0; i < 5; i++) logger.log("MULTI", "entry" + i);
            Thread.sleep(500);
            long count = logger.readRecent(100).stream()
                    .filter(e -> "MULTI".equals(e.action())).count();
            assertEquals(5, count);
        }

        @Test void logEntry_defaultDetail_empty() throws InterruptedException {
            logger.log("NO_DETAIL");
            Thread.sleep(300);
            AuditLogEntry e = logger.readRecent(10).stream()
                    .filter(x -> "NO_DETAIL".equals(x.action()))
                    .findFirst().orElse(null);
            assertNotNull(e);
            assertNotNull(e.detail());
        }

        @Test void noInit_doesNotThrow() {
            assertDoesNotThrow(() -> AuditLogger.getInstance().log("TEST"));
        }
    }
}