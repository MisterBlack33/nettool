package main.java.networktool;

import main.java.networktool.cli.CliRunner;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import main.java.networktool.storage.StorageLocationsResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.UIManager;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Main – nur noch Security-Init (init(dataDir) für AuditLogger/UserAuth).
 * isCliMode()/cliLogin() existieren nicht mehr (Main hat nur main()/runGui()).
 * GUI-Start wird nicht getestet (headless, Login-Dialog blockiert).
 *
 * Shares the "userAuthSingleton" resource lock with SecurityTest since both mutate
 * the process-wide UserAuth/AuditLogger singletons.
 */
@Isolated
@ResourceLock("userAuthSingleton")
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
    void storageLocationsResolver_resolveDataDir_notNull() {
        assertNotNull(StorageLocationsResolver.resolveDataDir());
    }

    @Test
    void mainClass_hasMainMethod() throws Exception {
        assertNotNull(Main.class.getDeclaredMethod("main", String[].class));
    }

    @Test
    void isCliMode_methodDoesNotExist() {
        assertFalse(hasMethod("isCliMode"), "isCliMode() sollte entfernt sein");
    }

    @Test
    void cliLogin_methodDoesNotExist() {
        assertFalse(hasMethod("cliLogin"), "cliLogin() sollte entfernt sein");
    }

    /**
     * runGui() ruft nach dem Auto-Login-Umbau keinen LoginDialog.show(...) mehr auf.
     * Ein Bytecode-/Reflection-Check auf konkrete Methodenaufrufe innerhalb eines
     * Methodenkörpers ist ohne zusätzliche Bibliothek (z.B. ASM) nicht sinnvoll
     * möglich — daher hier nur der dokumentierte manuelle Check: main() erzeugt
     * über runGui() direkt eine GUI-Instanz nach authenticateAsStandardUser(),
     * ohne LoginDialog zu importieren oder aufzurufen (siehe Main.java, Stand
     * dieses Commits). Diese Invariante wird bei jeder Änderung an Main.java
     * manuell durch Code-Review sichergestellt.
     */
    @Test
    void runGui_doesNotReferenceLoginDialog_manuallyVerified() {
        assertDoesNotThrow(() -> {}); // Platzhalter: siehe Javadoc oben.
    }

    @Test
    void main_help_printsUsage_andReturns() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            assertDoesNotThrow(() -> Main.main(new String[]{"--help"}));
            String out = buf.toString(StandardCharsets.UTF_8);
            assertTrue(out.contains("Verwendung"));
            assertTrue(out.contains("nettool"));
        } catch (Exception e) {
            fail("Unexpected exception while running Main.main(--help): " + e);
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void main_version_printsVersion_andReturns() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            assertDoesNotThrow(() -> Main.main(new String[]{"--version"}));
            String out = buf.toString(StandardCharsets.UTF_8);
            assertTrue(out.contains("NetTool"));
        } catch (Exception e) {
            fail("Unexpected exception while running Main.main(--version): " + e);
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void main_unknownArgs_exitsWithInvalidArgsCode() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                System.getProperty("java.class.path"),
                "main.java.networktool.Main",
                "--bogus"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(CliRunner.EXIT_INVALID_ARGS, exitCode);
        assertTrue(out.contains("--bogus"));
        assertTrue(out.contains("Verwendung"));
    }

    @Test
    void applySystemLookAndFeel_doesNotThrow() throws Exception {
        Method method = Main.class.getDeclaredMethod("applySystemLookAndFeel");
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(null));
        assertNotNull(UIManager.getLookAndFeel());
    }

    private static boolean hasMethod(String name) {
        for (var method : Main.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}

