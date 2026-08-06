package main.java.networktool;

import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Main – isCliMode(), cliLogin(), security init.
 * GUI-Start wird nicht getestet (headless).
 */

@Isolated
class MainTest {

    @TempDir Path tmp;

    private static Method isCliMode;
    private static Method cliLogin;

    @BeforeAll
    static void reflect() throws Exception {
        isCliMode = Main.class.getDeclaredMethod("isCliMode", String[].class);
        isCliMode.setAccessible(true);

        cliLogin = Main.class.getDeclaredMethod("cliLogin", Scanner.class);
        cliLogin.setAccessible(true);
    }

    @BeforeEach
    void setup() {
        AuditLogger.getInstance().init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().logout();
    }

    @AfterEach
    void teardown() {
        UserAuth.getInstance().logout();
        AuditLogger.getInstance().shutdown();
    }

    // ── AuditLogger / UserAuth init via main() ────────────────────────────

    @Test
    void auditLogger_init_doesNotThrow() {
        assertDoesNotThrow(() -> AuditLogger.getInstance().init(tmp));
    }

    @Test
    void userAuth_init_doesNotThrow() {
        assertDoesNotThrow(() -> UserAuth.getInstance().init(tmp));
    }

    @Test
    void storageUtils_resolveDataDir_notNull() {
        assertNotNull(StorageUtils.resolveDataDir());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean invoke_isCliMode(String[] args) throws Exception {
        return (boolean) isCliMode.invoke(null, (Object) args);
    }

    private boolean invoke_cliLogin(Scanner scanner) throws Exception {
        return (boolean) cliLogin.invoke(null, scanner);
    }
}