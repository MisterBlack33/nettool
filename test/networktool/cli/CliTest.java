package networktool.cli;

import main.java.networktool.cli.MenuPrinter;
import main.java.networktool.filter.ClipboardUtil;
import main.java.networktool.filter.JsonExporter;
import main.java.networktool.gui.notification.LocalToast;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MenuPrinter, JsonExporter, ClipboardUtil, LocalToast.
 * HostJsonBuilder tests → StorageTest.java (same package: main.java.networktool_v3.storage)
 */
class CliTest {

    // ══════════════════════════════════════════════════════════════
    //  MenuPrinter
    // ══════════════════════════════════════════════════════════════

    @Nested
    class MenuPrinterTest {

        @Test
        void print_containsMenuItems() {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream orig = System.out;
            System.setOut(new PrintStream(buf));
            try {
                MenuPrinter.print();
            } finally {
                System.setOut(orig);
            }
            String out = buf.toString();
            assertTrue(out.contains("0"));
            assertTrue(out.contains("Netzwerk"));
        }

        @Test
        void print_containsZeroToExit() {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream orig = System.out;
            System.setOut(new PrintStream(buf));
            try {
                MenuPrinter.print();
            } finally {
                System.setOut(orig);
            }
            assertTrue(buf.toString().contains("Beenden"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  JsonExporter
    // ══════════════════════════════════════════════════════════════

    @Nested
    class JsonExporterTest {

        @TempDir Path tmp;

        @Test
        void save_createsFile() {
            String path = tmp.resolve("out.json").toString();
            JsonExporter.save(List.of(), path);
            assertTrue(Files.exists(Path.of(path)));
        }

        @Test
        void save_emptyList_validJson() throws IOException {
            String path = tmp.resolve("empty.json").toString();
            JsonExporter.save(List.of(), path);
            String content = Files.readString(Path.of(path));
            assertTrue(content.trim().startsWith("["));
            assertTrue(content.trim().endsWith("]"));
        }

        @Test
        void save_withResults_containsIp() throws IOException {
            String path = tmp.resolve("results.json").toString();
            Map<Integer, String> ports = Map.of(22, "SSH");
            ScanResult r = new ScanResult("10.0.0.1", "server", ports, "Linux");
            JsonExporter.save(List.of(r), path);
            String content = Files.readString(Path.of(path));
            assertTrue(content.contains("10.0.0.1"));
        }

        @Test
        void save_specialCharsInHostname_escaped() throws IOException {
            String path = tmp.resolve("escaped.json").toString();
            ScanResult r = new ScanResult("1.1.1.1", "host\"name", new HashMap<>(), "Win");
            JsonExporter.save(List.of(r), path);
            String content = Files.readString(Path.of(path));
            assertFalse(content.contains("host\"name"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ClipboardUtil
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ClipboardUtilTest {

        @Test
        void copy_null_doesNotThrow() {
            assertDoesNotThrow(() -> ClipboardUtil.copy(null));
        }

        @Test
        void copy_blank_doesNotThrow() {
            assertDoesNotThrow(() -> ClipboardUtil.copy("   "));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LocalToast (PowerShell escaping)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class LocalToastTest {

        @Test
        void ps_escapeSingleQuote() {
            String result = LocalToast.ps("it's fine");
            assertTrue(result.contains("''"));
        }

        @Test
        void ps_null_returnsEmpty() {
            assertEquals("", LocalToast.ps(null));
        }

        @Test
        void ps_noSpecialChars_unchanged() {
            assertEquals("hello world",
                    LocalToast.ps("hello world"));
        }

        @Test
        void ps_newlineReplaced() {
            String result = LocalToast.ps("line1\nline2");
            assertFalse(result.contains("\n"));
        }
    }
}
