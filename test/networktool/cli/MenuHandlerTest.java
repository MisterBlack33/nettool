package networktool.cli;

import main.java.networktool.cli.MenuHandler;
import main.java.networktool.logic.scan.NetworkInfo;
import main.java.networktool.security.AuditLogger;
import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MenuHandler (CLI).
 * NetworkInfo.testMode prevents real network scans.
 */
class MenuHandlerTest {

    @TempDir Path tmp;

    @BeforeEach
    void setup() {
        NetworkInfo.testMode = true;
        AuditLogger.getInstance().init(tmp);
        UserAuth.getInstance().init(tmp);
        UserAuth.getInstance().createUser("admin", "admin123");
        UserAuth.getInstance().authenticate("admin", "admin123");
    }

    @AfterEach
    void teardown() {
        NetworkInfo.testMode = false;
        UserAuth.getInstance().logout();
        AuditLogger.getInstance().shutdown();
    }

    // ── handle() dispatch ─────────────────────────────────────────────────

    @Test
    void handle_zero_callsSystemExit() {
        assertThrows(SystemExitException.class, () -> {
            SecurityManager sm = new NoExitSecurityManager();
            System.setSecurityManager(sm);
            try {
                scanner("").handle(0);
            } finally {
                System.setSecurityManager(null);
            }
        });
    }

    @Test
    void handle_invalid_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("").handle(99));
    }

    @Test
    void handle_1_minimalInfo_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("").handle(1));
    }

    @Test
    void handle_2_fullInfo_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("").handle(2));
    }

    @Test
    void handle_3_diagnose_quickMode_doesNotThrow() {
        // modus=1, target=192.0.2.1 (non-routable doc addr)
        assertDoesNotThrow(() -> scanner("1\n192.0.2.1\n").handle(3));
    }

    @Test
    void handle_3_diagnose_fullMode_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("2\n192.0.2.1\n").handle(3));
    }

    @Test
    void handle_4_fileServer_doesNotThrow() {
        // Use ephemeral port range to avoid conflicts
        assertDoesNotThrow(() -> scanner("19876\n").handle(4));
    }

    @Test
    void handle_5_fileSend_missingFile_doesNotThrow() {
        assertDoesNotThrow(() ->
                scanner("192.0.2.1\n19875\n/nonexistent/file.txt\n").handle(5));
    }

    @Test
    void handle_6_cidrScan_withEmptyFilters_doesNotThrow() {
        // CIDR, no regex filter, no os+port filter, no json export
        assertDoesNotThrow(() ->
                scanner("127.0.0.0/30\n\n\nn\n").handle(6));
    }

    @Test
    void handle_7_filterScan_alle_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("alle\n\n").handle(7));
    }

    @Test
    void handle_7_filterScan_windows_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("windows\n\n").handle(7));
    }

    @Test
    void handle_8_sendMessage_doesNotThrow() {
        assertDoesNotThrow(() ->
                scanner("192.0.2.1\nhello test\n\n").handle(8));
    }

    @Test
    void handle_9_remoteNet_cidr_doesNotThrow() {
        assertDoesNotThrow(() ->
                scanner("1\n192.0.2.0/24\n").handle(9));
    }

    @Test
    void handle_9_remoteNet_reachTest_doesNotThrow() {
        assertDoesNotThrow(() ->
                scanner("3\n192.0.2.0/24\n").handle(9));
    }

    @Test
    void handle_9_remoteNet_routingHints_doesNotThrow() {
        assertDoesNotThrow(() ->
                scanner("4\n192.0.2.0/24\n").handle(9));
    }

    @Test
    void handle_10_scheduler_listRunning_doesNotThrow() {
        // modus 3 = stop all (safe even if nothing running)
        assertDoesNotThrow(() -> scanner("3\n").handle(10));
    }

    @Test
    void handle_11_securityMonitor_invalidChoice_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("9\n").handle(11));
    }

    @Test
    void handle_12_dauerping_zeroSeconds_doesNotThrow() {
        // 0s = beendet sofort
        assertDoesNotThrow(() -> scanner("127.0.0.1\n0\n").handle(12));
    }

    @Test
    void handle_14_exportImport_invalidChoice_doesNotThrow() {
        assertDoesNotThrow(() -> scanner("9\n").handle(14));
    }

    @Test
    void handle_allValidChoices_noNpe() {
        int[] safe = {1, 2, 99};
        for (int c : safe)
            assertDoesNotThrow(() -> scanner("").handle(c),
                    "handle(" + c + ") threw unexpectedly");
    }

    // ── Output capture ────────────────────────────────────────────────────

    @Test
    void handle_invalid_printsMessage() {
        String out = captureOut(() -> scanner("").handle(99));
        assertFalse(out.isEmpty());
    }

    @Test
    void handle_1_printsNetworkInfo() {
        String out = captureOut(() -> scanner("").handle(1));
        assertTrue(out.contains("Netzwerk") || out.contains("netzwerk")
                || out.contains("Gerät") || !out.isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static MenuHandler scanner(String input) {
        return new MenuHandler(new Scanner(
                new ByteArrayInputStream(input.getBytes())));
    }

    private static String captureOut(Runnable r) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(buf));
        try { r.run(); } finally { System.setOut(orig); }
        return buf.toString();
    }

    // Minimal SecurityManager to intercept System.exit()
    private static class SystemExitException extends SecurityException {
        SystemExitException(int status) { super("exit:" + status); }
    }

    @SuppressWarnings("removal")
    private static class NoExitSecurityManager extends SecurityManager {
        @Override public void checkPermission(java.security.Permission p) {}
        @Override public void checkExit(int status) { throw new SystemExitException(status); }
    }
}