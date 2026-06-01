package networktool_v3.security;

import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserAuth and AuditLogger.
 * Uses @TempDir so no real files are touched.
 */
class SecurityTest {

    // ══════════════════════════════════════════════════════════════
    //  UserAuth
    // ══════════════════════════════════════════════════════════════

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

        @Test
        void hasUsers_empty() {
            assertFalse(auth.hasUsers());
        }

        @Test
        void createUser_success() {
            assertTrue(auth.createUser("alice", "secret1"));
            assertTrue(auth.hasUsers());
        }

        @Test
        void createUser_shortPassword_rejected() {
            assertFalse(auth.createUser("bob", "ab"));
        }

        @Test
        void createUser_blankName_rejected() {
            assertFalse(auth.createUser("  ", "password1"));
        }

        @Test
        void createUser_duplicate_caseInsensitive_rejected() {
            auth.createUser("Alice", "pass12");
            assertFalse(auth.createUser("alice", "other1"));
        }

        @Test
        void authenticate_correct() {
            auth.createUser("carol", "mypass1");
            assertTrue(auth.authenticate("carol", "mypass1"));
            assertEquals("carol", auth.getCurrentUser());
        }

        @Test
        void authenticate_wrongPassword() {
            auth.createUser("dave", "right12");
            assertFalse(auth.authenticate("dave", "wrong1"));
        }

        @Test
        void authenticate_unknownUser() {
            assertFalse(auth.authenticate("nobody", "pass123"));
        }

        @Test
        void authenticate_caseInsensitiveUsername() {
            auth.createUser("Eve", "pass123");
            assertTrue(auth.authenticate("EVE", "pass123"));
        }

        @Test
        void firstUser_isAdmin() {
            auth.createUser("admin1", "admin12");
            auth.authenticate("admin1", "admin12");
            assertTrue(auth.isAdmin());
        }

        @Test
        void secondUser_isNotAdmin() {
            auth.createUser("admin1", "admin12");
            auth.createUser("user1", "user123");
            auth.authenticate("user1", "user123");
            assertFalse(auth.isAdmin());
        }

        @Test
        void logout_clearsCurrentUser() {
            auth.createUser("frank", "frank12");
            auth.authenticate("frank", "frank12");
            auth.logout();
            assertNull(auth.getCurrentUser());
        }

        @Test
        void changePassword_success() {
            auth.createUser("grace", "old1234");
            auth.authenticate("grace", "old1234");
            assertTrue(auth.changePassword("grace", "old1234", "new1234"));
            assertTrue(auth.authenticate("grace", "new1234"));
        }

        @Test
        void changePassword_wrongOld_fails() {
            auth.createUser("hal", "pass123");
            auth.authenticate("hal", "pass123");
            assertFalse(auth.changePassword("hal", "wrong12", "new1234"));
        }

        @Test
        void deleteUser_success() {
            auth.createUser("ira", "pass123");
            auth.createUser("joe", "pass456");
            auth.authenticate("joe", "pass456");
            assertTrue(auth.deleteUser("joe", "pass456"));
        }

        @Test
        void deleteUser_lastUser_rejected() {
            auth.createUser("solo", "pass123");
            auth.authenticate("solo", "pass123");
            assertFalse(auth.deleteUser("solo", "pass123"));
        }

        @Test
        void listUsernames_returnsAll() {
            auth.createUser("u1", "pass111");
            auth.createUser("u2", "pass222");
            List<String> names = auth.listUsernames();
            assertTrue(names.contains("u1"));
            assertTrue(names.contains("u2"));
        }

        @Test
        void getCurrentRole_admin() {
            auth.createUser("root", "root123");
            auth.authenticate("root", "root123");
            assertEquals("admin", auth.getCurrentRole());
        }

        @Test
        void getCurrentRole_user() {
            auth.createUser("root", "root123");
            auth.createUser("regular", "reg1234");
            auth.authenticate("regular", "reg1234");
            assertEquals("user", auth.getCurrentRole());
        }

        @Test
        void isAdmin_notLoggedIn() {
            assertFalse(auth.isAdmin());
        }

        @Test
        void persistence_survivesReinit() {
            auth.createUser("persistent", "pass123");
            // Re-init with same dir
            auth.init(tmp);
            assertTrue(auth.authenticate("persistent", "pass123"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AuditLogger
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AuditLoggerTest {

        @TempDir Path tmp;
        AuditLogger logger;

        @BeforeEach
        void setup() throws InterruptedException {
            logger = AuditLogger.getInstance();
            logger.init(tmp);
            logger.clear();
            Thread.sleep(100);
        }

        @Test
        void log_createsFile() throws InterruptedException {
            logger.log("TEST_ACTION", "detail1");
            Thread.sleep(300);
            assertTrue(Files.exists(tmp.resolve("audit.log")));
        }

        @Test
        void log_singleParam() throws InterruptedException {
            logger.log("SIMPLE");
            Thread.sleep(300);
            List<AuditLogger.LogEntry> entries = logger.readRecent(10);
            assertTrue(entries.stream().anyMatch(e -> "SIMPLE".equals(e.action)));
        }

        @Test
        void log_withDetail() throws InterruptedException {
            logger.log("ACTION", "someDetail");
            Thread.sleep(300);
            List<AuditLogger.LogEntry> entries = logger.readRecent(10);
            assertTrue(entries.stream().anyMatch(e ->
                    "ACTION".equals(e.action) && "someDetail".equals(e.detail)));
        }

        @Test
        void readRecent_respects_maxLines() throws InterruptedException {
            for (int i = 0; i < 10; i++) logger.log("FILL", "x" + i);
            Thread.sleep(400);
            List<AuditLogger.LogEntry> entries = logger.readRecent(3);
            assertTrue(entries.size() <= 3);
        }

        @Test
        void readRecent_emptyFile_returnsEmpty() throws IOException {
            Path log = tmp.resolve("audit.log");
            Files.writeString(log, "", StandardCharsets.UTF_8);
            List<AuditLogger.LogEntry> entries = logger.readRecent(100);
            assertTrue(entries.isEmpty());
        }

        @Test
        void clear_removesEntries() throws InterruptedException {
            logger.log("BEFORE", "x");
            Thread.sleep(300);
            logger.clear();
            Thread.sleep(200);
            // Only AUDIT_LOG_CLEARED should remain (logged by clear itself)
            List<AuditLogger.LogEntry> entries = logger.readRecent(100);
            assertTrue(entries.stream().allMatch(e ->
                    "AUDIT_LOG_CLEARED".equals(e.action)));
        }

        @Test
        void readByUser_filtersCorrectly() throws InterruptedException {
            // Need a logged-in user
            UserAuth.getInstance().init(tmp);
            UserAuth.getInstance().createUser("testuser", "pass123");
            UserAuth.getInstance().authenticate("testuser", "pass123");
            logger.log("USER_ACTION", "by testuser");
            Thread.sleep(300);
            List<AuditLogger.LogEntry> entries = logger.readByUser("testuser");
            assertTrue(entries.stream().anyMatch(e -> "USER_ACTION".equals(e.action)));
        }

        @Test
        void parse_tabFormat() {
            String line = "2024-01-01 10:00:00\tuser1\tLOGIN\tdetail";
            AuditLogger.LogEntry e = AuditLogger.parse(line);
            assertNotNull(e);
            assertEquals("user1", e.user);
            assertEquals("LOGIN", e.action);
            assertEquals("detail", e.detail);
        }

        @Test
        void parse_legacyJson() {
            String json = "{\"timestamp\":\"2024-01-01\",\"user\":\"u\",\"action\":\"A\",\"detail\":\"d\"}";
            AuditLogger.LogEntry e = AuditLogger.parse(json);
            assertNotNull(e);
            assertEquals("A", e.action);
        }

        @Test
        void parse_invalidLine_returnsNull() {
            assertNull(AuditLogger.parse(""));
            assertNull(AuditLogger.parse(null));
            assertNull(AuditLogger.parse("only one field"));
        }

        @Test
        void multipleLogCalls_allPersisted() throws InterruptedException {
            for (int i = 0; i < 5; i++) logger.log("MULTI", "entry" + i);
            Thread.sleep(500);
            List<AuditLogger.LogEntry> entries = logger.readRecent(100);
            long count = entries.stream().filter(e -> "MULTI".equals(e.action)).count();
            assertEquals(5, count);
        }

        @Test
        void logEntry_defaultDetail_empty() throws InterruptedException {
            logger.log("NO_DETAIL");
            Thread.sleep(300);
            List<AuditLogger.LogEntry> entries = logger.readRecent(10);
            AuditLogger.LogEntry e = entries.stream()
                    .filter(x -> "NO_DETAIL".equals(x.action))
                    .findFirst().orElse(null);
            assertNotNull(e);
            assertNotNull(e.detail);
        }

        @Test
        void noInit_doesNotThrow() {
            AuditLogger uninit = AuditLogger.getInstance();
            // calling log before init should not throw
            assertDoesNotThrow(() -> uninit.log("TEST"));
        }
    }
}
