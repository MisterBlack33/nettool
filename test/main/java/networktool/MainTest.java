package main.java.networktool;

import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Main – nur noch Security-Init (init(dataDir) für AuditLogger/UserAuth).
 * isCliMode()/cliLogin() existieren nicht mehr (Main hat nur main()/runGui()).
 * GUI-Start wird nicht getestet (headless, Login-Dialog blockiert).
 */
@Isolated
class MainTest {

    @TempDir Path tmp;

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

    @Test
    void mainClass_hasMainMethod() throws Exception {
        assertNotNull(Main.class.getDeclaredMethod("main", String[].class));
    }

    @Test
    void isCliMode_methodDoesNotExist() {
        boolean found = false;
        for (var m : Main.class.getDeclaredMethods()) {
            if (m.getName().equals("isCliMode")) { found = true; break; }
        }
        assertFalse(found, "isCliMode() sollte entfernt sein");
    }

    @Test
    void cliLogin_methodDoesNotExist() {
        boolean found = false;
        for (var m : Main.class.getDeclaredMethods()) {
            if (m.getName().equals("cliLogin")) { found = true; break; }
        }
        assertFalse(found, "cliLogin() sollte entfernt sein");
    }
}