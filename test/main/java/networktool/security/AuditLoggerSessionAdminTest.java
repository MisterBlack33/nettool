package main.java.networktool.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die Audit-Integration der Session-Admin-Freischaltung: clear()
 * funktioniert nach grantSessionAdmin() ohne persistierte Admin-Rolle, und
 * Erfolg/Fehlschlag werden als GET_ADMIN / GET_ADMIN_FAILED geloggt.
 * Separate Klasse statt Ergänzung in AuditLoggerTest (Größenlimit).
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("userAuthSingleton")
class AuditLoggerSessionAdminTest {

    private static final String ADMIN_PW = "test1234";

    @TempDir Path tmp;
    AuditLogger logger;
    UserAuth auth;

    @BeforeEach void setup() {
        logger = AuditLogger.getInstance();
        logger.init(tmp);
        auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.logout();
        auth.seedDefaultUsers();
        auth.authenticateAsStandardUser();
    }

    @AfterEach void teardown() {
        auth.logout();
        logger.shutdown();
    }

    @Test void clear_afterSessionAdminOverride_succeedsWithoutPersistedAdminRole() {
        auth.grantSessionAdmin(ADMIN_PW);
        assertDoesNotThrow(() -> logger.clear());
    }

    @Test void getAdmin_success_logsAction() {
        boolean ok = auth.grantSessionAdmin(ADMIN_PW);
        logger.log(ok ? "GET_ADMIN" : "GET_ADMIN_FAILED", "User");
        assertTrue(logger.readRecent(10).stream()
                .anyMatch(e -> "GET_ADMIN".equals(e.action()) && "User".equals(e.detail())));
    }

    @Test void getAdmin_failure_logsAction() {
        boolean ok = auth.grantSessionAdmin("wrong-password");
        logger.log(ok ? "GET_ADMIN" : "GET_ADMIN_FAILED", "User");
        assertTrue(logger.readRecent(10).stream()
                .anyMatch(e -> "GET_ADMIN_FAILED".equals(e.action()) && "User".equals(e.detail())));
    }
}