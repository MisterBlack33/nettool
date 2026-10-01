package networktool.security;

import main.java.networktool.security.AuditLogEntry;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("userAuthSingleton")
class SecurityTest {

    private Path tmp;

    @BeforeEach
    void resetSingletons() throws IOException {
        tmp = Files.createTempDirectory("security-test-");
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().logout();
        AuditLogger.getInstance().shutdown();
        AuditLogger.getInstance().init(tmp);
    }

    @Test
    void userAuth_hasUsers_empty() {
        assertFalse(UserAuth.getInstance().hasUsers());
    }

    @Test
    void userAuth_createUser_success() {
        UserAuth auth = UserAuth.getInstance();
        assertTrue(auth.createUser("alice", "secret12"));
        assertTrue(auth.hasUsers());
    }

    @Test
    void userAuth_createUser_shortPassword_rejected() {
        assertFalse(UserAuth.getInstance().createUser("bob", "ab"));
    }

    @Test
    void userAuth_createUser_blankName_rejected() {
        assertFalse(UserAuth.getInstance().createUser("  ", "password1"));
    }

    @Test
    void userAuth_createUser_duplicate_caseInsensitive_rejected() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("Alice", "pass1234");
        assertFalse(auth.createUser("alice", "other123"));
    }

    @Test
    void userAuth_authenticate_correct() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("carol", "mypass12");
        assertTrue(auth.authenticate("carol", "mypass12"));
        assertEquals("carol", auth.getCurrentUser());
    }

    @Test
    void userAuth_authenticate_wrongPassword() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("dave", "right123");
        assertFalse(auth.authenticate("dave", "wrong1"));
    }

    @Test
    void userAuth_authenticate_unknownUser() {
        assertFalse(UserAuth.getInstance().authenticate("nobody", "pass123"));
    }

    @Test
    void userAuth_authenticate_caseInsensitiveUsername() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("Eve", "pass1234");
        assertTrue(auth.authenticate("EVE", "pass1234"));
    }

    @Test
    void userAuth_firstUser_isAdmin() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("admin1", "admin123");
        auth.authenticate("admin1", "admin123");
        assertTrue(auth.isAdmin());
    }

    @Test
    void userAuth_secondUser_isNotAdmin() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("admin1", "admin123");
        auth.createUser("user1", "user1234");
        auth.authenticate("user1", "user1234");
        assertFalse(auth.isAdmin());
    }

    @Test
    void userAuth_logout_clearsCurrentUser() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("frank", "frank123");
        auth.authenticate("frank", "frank123");
        auth.logout();
        assertNull(auth.getCurrentUser());
    }

    @Test
    void userAuth_changePassword_success() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("grace", "old12345");
        auth.authenticate("grace", "old12345");
        assertTrue(auth.changePassword("grace", "old12345", "new12345"));
        assertTrue(auth.authenticate("grace", "new12345"));
    }

    @Test
    void userAuth_changePassword_wrongOld_fails() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("hal", "pass1234");
        auth.authenticate("hal", "pass1234");
        assertFalse(auth.changePassword("hal", "wrong12", "new1234"));
    }

    @Test
    void userAuth_deleteUser_success() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("ira", "pass1234");
        auth.createUser("joe", "pass4567");
        auth.authenticate("joe", "pass4567");
        assertTrue(auth.deleteUser("joe", "pass4567"));
    }

    @Test
    void userAuth_deleteUser_lastUser_rejected() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("solo", "pass1234");
        auth.authenticate("solo", "pass1234");
        assertFalse(auth.deleteUser("solo", "pass1234"));
    }

    @Test
    void userAuth_listUsernames_returnsAll() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("u1", "pass1111");
        auth.createUser("u2", "pass2222");
        List<String> names = auth.listUsernames();
        assertTrue(names.contains("u1"));
        assertTrue(names.contains("u2"));
    }

    @Test
    void userAuth_getCurrentRole_admin() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("root", "root1234");
        auth.authenticate("root", "root1234");
        assertEquals("admin", auth.getCurrentRole());
    }

    @Test
    void userAuth_getCurrentRole_user() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("root", "root1234");
        auth.createUser("regular", "reg12345");
        auth.authenticate("regular", "reg12345");
        assertEquals("user", auth.getCurrentRole());
    }

    @Test
    void userAuth_isAdmin_notLoggedIn() {
        assertFalse(UserAuth.getInstance().isAdmin());
    }

    @Test
    void userAuth_persistence_survivesReinit() {
        UserAuth auth = UserAuth.getInstance();
        auth.createUser("persistent", "pass1234");
        auth.init(tmp);
        assertTrue(auth.authenticate("persistent", "pass1234"));
    }

    @Test
    void auditLogger_log_createsFile() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");

        logger.log("TEST_ACTION", "detail1");

        assertTrue(Files.exists(tmp.resolve("audit.log")));
    }

    @Test
    void auditLogger_log_singleParam() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");

        logger.log("SIMPLE");

        assertTrue(logger.readRecent(10).stream().anyMatch(e -> "SIMPLE".equals(e.action())));
    }

    @Test
    void auditLogger_log_withDetail() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");

        logger.log("ACTION", "someDetail");

        assertTrue(logger.readRecent(10).stream()
                .anyMatch(e -> "ACTION".equals(e.action()) && "someDetail".equals(e.detail())));
    }

    @Test
    void auditLogger_readRecent_respects_maxLines() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");

        for (int i = 0; i < 10; i++) logger.log("FILL", "x" + i);

        assertTrue(logger.readRecent(3).size() <= 3);
    }

    @Test
    void auditLogger_readRecent_emptyFile_returnsEmpty() throws IOException {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        Path log = tmp.resolve("audit.log");
        Files.writeString(log, "", StandardCharsets.UTF_8);

        assertTrue(logger.readRecent(100).isEmpty());
    }

    @Test
    void auditLogger_clear_removesEntries() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");

        logger.log("BEFORE", "x");
        logger.clear();

        assertTrue(logger.readRecent(100).stream().anyMatch(e -> "AUDIT_LOG_CLEARED".equals(e.action())));
    }

    @Test
    void auditLogger_readByUser_filtersCorrectly() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.createUser("admin", "admin123");
        auth.authenticate("admin", "admin123");
        auth.createUser("loguser", "pass1234");
        auth.authenticate("loguser", "pass1234");

        logger.log("USER_ACTION", "by loguser");

        assertTrue(logger.readByUser("loguser").stream().anyMatch(e -> "USER_ACTION".equals(e.action())));
    }

    @Test
    void auditLogger_parse_ndjsonFormat() {
        String line = "{\"v\":1,\"ts\":\"2024-01-01 10:00:00\",\"user\":\"user1\",\"action\":\"LOGIN\",\"detail\":\"detail\"}";
        AuditLogEntry e = AuditLogger.parse(line);
        assertNotNull(e);
        assertEquals("user1", e.user());
        assertEquals("LOGIN", e.action());
        assertEquals("detail", e.detail());
    }

    @Test
    void auditLogger_parse_invalidLine_returnsNull() {
        assertNull(AuditLogger.parse(""));
        assertNull(AuditLogger.parse(null));
        assertNull(AuditLogger.parse("only one field"));
    }

    @Test
    void auditLogger_multipleLogCalls_allPersisted() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.createUser("admin", "admin123");
        auth.authenticate("admin", "admin123");

        for (int i = 0; i < 5; i++) logger.log("MULTI", "entry" + i);

        assertEquals(5, logger.readRecent(100).stream().filter(e -> "MULTI".equals(e.action())).count());
    }

    @Test
    void auditLogger_logEntry_defaultDetail_empty() {
        AuditLogger logger = AuditLogger.getInstance();
        logger.init(tmp);
        UserAuth auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.createUser("admin", "admin123");
        auth.authenticate("admin", "admin123");

        logger.log("NO_DETAIL");

        AuditLogEntry e = logger.readRecent(10).stream()
                .filter(x -> "NO_DETAIL".equals(x.action()))
                .findFirst().orElse(null);
        assertNotNull(e);
        assertNotNull(e.detail());
    }

    @Test
    void auditLogger_noInit_doesNotThrow() {
        assertDoesNotThrow(() -> AuditLogger.getInstance().log("TEST"));
    }
}
