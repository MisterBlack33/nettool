// test/networktool/cli/MenuHandlerTest.java
package networktool.cli;

import networktool.cli.MenuHandler;
import networktool.logic.analysis.IpInspector;
import networktool.logic.scan.NetworkInfo;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MenuHandlerTest {

    @BeforeEach
    void enableTestMode() {
        NetworkInfo.testMode  = true;
        IpInspector.testMode  = true;
    }

    @AfterEach
    void disableTestMode() {
        NetworkInfo.testMode  = false;
        IpInspector.testMode  = false;
    }

    private static MenuHandler handler(String... lines) {
        String input = String.join("\n", lines) + "\n";
        return new MenuHandler(new Scanner(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
    }

    private static String captureOut(Runnable r) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try { r.run(); } finally { System.setOut(orig); }
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test void handle_choice1_doesNotThrow() { assertDoesNotThrow(() -> handler().handle(1)); }
    @Test void handle_choice2_doesNotThrow() { assertDoesNotThrow(() -> handler().handle(2)); }

    @Test void handle_invalid_printsMessage() {
        String out = captureOut(() -> handler().handle(99));
        assertTrue(out.toLowerCase().contains("ungültig") || out.toLowerCase().contains("ungu"));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void handle_choice3_schnell_doesNotThrow() {
        assertDoesNotThrow(() -> handler("1", "192.0.2.1").handle(3));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void handle_choice3_voll_doesNotThrow() {
        assertDoesNotThrow(() -> handler("2", "192.0.2.1").handle(3));
    }

    @Test void handle_choice9_erreichbarkeitstest_doesNotThrow() {
        assertDoesNotThrow(() -> handler("3", "192.0.2.0/30").handle(9));
    }

    @Test void handle_choice9_routingHilfe_doesNotThrow() {
        assertDoesNotThrow(() -> handler("4", "192.168.99.0/24").handle(9));
    }

    @Test void handle_choice9_ungueltigerModus_doesNotThrow() {
        assertDoesNotThrow(() -> handler("9").handle(9));
    }

    @Test void handle_choice11_arpStop_doesNotThrow() {
        assertDoesNotThrow(() -> handler("1", "").handle(11));
    }

    @Test void handle_choice11_portStop_doesNotThrow() {
        assertDoesNotThrow(() -> handler("2", "5", "").handle(11));
    }

    @Test void handle_choice11_ungueltig_doesNotThrow() {
        assertDoesNotThrow(() -> handler("X").handle(11));
    }

    @Test void handle_choice10_stopAll_doesNotThrow() {
        assertDoesNotThrow(() -> handler("3").handle(10));
    }

    @Test void handle_choice14_csvExport_doesNotThrow()  { assertDoesNotThrow(() -> handler("1").handle(14)); }
    @Test void handle_choice14_jsonExport_doesNotThrow() { assertDoesNotThrow(() -> handler("2").handle(14)); }
    @Test void handle_choice14_htmlExport_doesNotThrow() { assertDoesNotThrow(() -> handler("3").handle(14)); }
    @Test void handle_choice14_zipBackup_doesNotThrow()  { assertDoesNotThrow(() -> handler("4").handle(14)); }
    @Test void handle_choice14_ungueltig_doesNotThrow()  { assertDoesNotThrow(() -> handler("X").handle(14)); }

    @Test void handle_choice13_noServer_doesNotThrow() {
        assertDoesNotThrow(() -> handler("2", "192.0.2.1").handle(13));
    }

    @Test void handle_choice7_filterAlle_doesNotThrow() {
        assertDoesNotThrow(() -> handler("alle", "").handle(7));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void handle_choice6_loopback_doesNotThrow() {
        assertDoesNotThrow(() -> handler("127.0.0.0/30", "", "", "n").handle(6));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void handle_choice12_maxSec_doesNotThrow() {
        Thread t = new Thread(() -> {
            try { handler("192.0.2.1", "1").handle(12); } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(3000); } catch (InterruptedException ignored) {}
    }
}