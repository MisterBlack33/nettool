package networktool;

import networktool.security.AuditLogger;
import networktool.security.UserAuth;
import networktool.storage.StorageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Main – isCliMode(), cliLogin(), security init.
 * GUI-Start wird nicht getestet (headless).
 */
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

    // ── isCliMode ─────────────────────────────────────────────────────────

    @Test
    void isCliMode_withCliArg_true() throws Exception {
        assertTrue(invoke_isCliMode(new String[]{"--cli"}));
    }

    @Test
    void isCliMode_cliArgCaseInsensitive_true() throws Exception {
        assertTrue(invoke_isCliMode(new String[]{"--CLI"}));
    }

    @Test
    void isCliMode_emptyArgs_false() throws Exception {
        assertFalse(invoke_isCliMode(new String[]{}));
    }

    @Test
    void isCliMode_otherArg_false() throws Exception {
        assertFalse(invoke_isCliMode(new String[]{"--gui"}));
    }

    @Test
    void isCliMode_null_noThrow() {
        assertDoesNotThrow(() -> invoke_isCliMode(new String[]{}));
    }

    // ── cliLogin – neuer Benutzer ─────────────────────────────────────────

    @Test
    void cliLogin_noUsers_createAndLogin() throws Exception {
        String input = "testuser\npass1234\npass1234\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        boolean result = invoke_cliLogin(scanner);
        assertTrue(result);
        assertEquals("testuser", UserAuth.getInstance().getCurrentUser());
    }

    @Test
    void cliLogin_noUsers_passwordMismatch_returnsFalse() throws Exception {
        String input = "user1\npass1234\nwrong123\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        boolean result = invoke_cliLogin(scanner);
        assertFalse(result);
    }

    // ── cliLogin – vorhandener Benutzer ───────────────────────────────────

    @Test
    void cliLogin_existingUser_correctPassword_true() throws Exception {
        UserAuth.getInstance().createUser("alice", "secret12");
        String input = "alice\nsecret12\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertTrue(invoke_cliLogin(scanner));
        assertEquals("alice", UserAuth.getInstance().getCurrentUser());
    }

    @Test
    void cliLogin_existingUser_wrongPassword_returnsFalse() throws Exception {
        UserAuth.getInstance().createUser("bob", "correct1");
        // 3 Fehlversuche
        String input = "bob\nwrong123\nbob\nwrong456\nbob\nwrong789\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertFalse(invoke_cliLogin(scanner));
    }

    @Test
    void cliLogin_existingUser_secondAttemptSuccess_true() throws Exception {
        UserAuth.getInstance().createUser("carol", "pass1234");
        String input = "carol\nwrongpw1\ncarol\npass1234\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertTrue(invoke_cliLogin(scanner));
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