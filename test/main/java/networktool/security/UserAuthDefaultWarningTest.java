package main.java.networktool.security;

import main.java.networktool.logging.DebugLogger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Shares "userAuthSingleton" lock with other UserAuth-mutating tests. */
@Isolated
@ResourceLock("userAuthSingleton")
class UserAuthDefaultWarningTest {

    @TempDir Path tmp;
    UserAuth auth;
    DebugLogger logger;

    @BeforeEach void setup() {
        auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.logout();
        logger = DebugLogger.getInstance();
        logger.init(tmp);
        logger.clear();
    }

    @AfterEach void teardown() {
        auth.logout();
        logger.shutdown();
    }

    @Test void seedDefaultUsers_logsWarning_whenDefaultPasswordActive() {
        auth.seedDefaultUsers();
        assertTrue(logger.readRecent(20).stream()
                .anyMatch(e -> "WARN".equals(e.level()) && e.message().contains("admin")));
    }

    @Test void seedDefaultUsers_noWarning_afterAdminPasswordChanged() {
        auth.seedDefaultUsers();
        auth.authenticate("admin", "test1234");
        assertTrue(auth.changePassword("admin", "test1234", "newSecurePw1"));
        logger.clear();

        auth.seedDefaultUsers();
        assertFalse(logger.readRecent(20).stream()
                .anyMatch(e -> "WARN".equals(e.level()) && e.message().contains("admin")));
    }

    @Test void seedDefaultUsers_doesNotThrow() {
        assertDoesNotThrow(() -> auth.seedDefaultUsers());
    }
}