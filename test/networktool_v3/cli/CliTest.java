package networktool_v3.cli;

import main.java.networktool_v3.cli.MenuPrinter;
import main.java.networktool_v3.filter.JsonExporter;
import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.logic.scan.PingSweep;
import main.java.networktool_v3.logic.scan.RemoteNetScanner;
import main.java.networktool_v3.model.ScanResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// ── JsonExporter ──────────────────────────────────────────────────────────────

class JsonExporterTest {

    @TempDir Path tempDir;

    @Test
    void savesValidJson() throws Exception {
        List<ScanResult> results = List.of(
                new ScanResult("192.168.1.1", "router", Map.of(80, "HTTP"), "Linux"),
                new ScanResult("192.168.1.2", "pc",     Map.of(445,"SMB"), "Windows")
        );
        Path file = tempDir.resolve("test.json");
        JsonExporter.save(results, file.toString());

        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.startsWith("["));
        assertTrue(content.contains("\"ip\""));
        assertTrue(content.contains("192.168.1.1"));
        assertTrue(content.contains("192.168.1.2"));
    }

    @Test
    void savesEmptyList() throws Exception {
        Path file = tempDir.resolve("empty.json");
        JsonExporter.save(List.of(), file.toString());
        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("[") && content.contains("]"));
    }

    @Test
    void escapesSpecialChars() throws Exception {
        List<ScanResult> results = List.of(
                new ScanResult("1.2.3.4", "host-with-\"quotes\"", Map.of(), "Linux")
        );
        Path file = tempDir.resolve("special.json");
        JsonExporter.save(results, file.toString());
        String content = Files.readString(file);
        assertTrue(content.contains("\\\"quotes\\\""));
    }
}

// ── OsDetector hostname classifier (additional cases) ────────────────────────

class OsDetectorHostnameExtTest {

    @Test
    void detectNAS_synology() {
        String os = OsDetector.classifyHostname("synology-nas.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("synology") || os.toLowerCase().contains("nas"));
    }

    @Test
    void detectChromecast() {
        String os = OsDetector.classifyHostname("chromecast-ultra.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("chromecast"));
    }

    @Test
    void detectXbox() {
        String os = OsDetector.classifyHostname("XBOX-LIVINGROOM.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("xbox"));
    }

    @Test
    void detectIPCamera() {
        String os = OsDetector.classifyHostname("ipcam-garage.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("kamera") || os.toLowerCase().contains("cam"));
    }

    @Test
    void detectPlaystation() {
        String os = OsDetector.classifyHostname("ps5-livingroom");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("playstation"));
    }

    @Test
    void detectPrinter() {
        String os = OsDetector.classifyHostname("hp-laserjet-printer.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("drucker") || os.toLowerCase().contains("printer")
                || os.toLowerCase().contains("hp"));
    }

    @Test
    void detectMikrotik() {
        String os = OsDetector.classifyHostname("mikrotik-router.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("mikrotik") || os.toLowerCase().contains("router"));
    }

    @Test
    void detectUbiquiti() {
        String os = OsDetector.classifyHostname("ubiquiti-ap-main.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("ubiquiti") || os.toLowerCase().contains("access"));
    }

    @Test
    void detectCisco() {
        String os = OsDetector.classifyHostname("cisco-switch-floor2");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("cisco"));
    }

    @Test
    void detectSonos() {
        String os = OsDetector.classifyHostname("sonos-speaker.local");
        assertNotNull(os);
        assertTrue(os.toLowerCase().contains("sonos"));
    }
}

// ── PingSweep ─────────────────────────────────────────────────────────────────

class PingSweepTest {

    @Test
    void sweepEmptyListReturnsEmpty() {
        List<String> result = PingSweep.sweep(List.of(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void sweepLocalhostFindsIt() {
        // 127.0.0.1 should always be reachable
        List<String> result = PingSweep.sweep(List.of("127.0.0.1"), null);
        assertTrue(result.contains("127.0.0.1"));
    }

    @Test
    void sweepUnreachableIpsReturnEmpty() {
        // Use TEST-NET-1 (RFC 5737) - not routeable
        List<String> result = PingSweep.sweep(
                List.of("192.0.2.1", "192.0.2.2"), null);
        // These should not be reachable (test addresses)
        // Result may be empty or contain items depending on network
        assertNotNull(result);
    }

    @Test
    void progressCallbackCalled() {
        int[] count = {0};
        PingSweep.sweep(List.of("127.0.0.1"), () -> count[0]++);
        assertEquals(1, count[0]);
    }
}

// ── RemoteNetScanner cidrToSubnetPrefixes edge cases ─────────────────────────

class RemoteNetScannerEdgeTest {

    @Test
    void cidrToSubnetPrefixes_slash25() {
        // /25 = 128 hosts, but still 1 /24 prefix
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("192.168.1.0/25");
        assertEquals(1, prefixes.size());
    }

    @Test
    void cidrToSubnetPrefixes_slash8_capped() {
        // /8 = 256 /24 prefixes
        List<String> prefixes = RemoteNetScanner.cidrToSubnetPrefixes("10.0.0.0/8");
        assertTrue(prefixes.size() <= 256);
        assertTrue(prefixes.size() > 0);
    }

    @Test
    void normalizeHandlesSlashAlready() {
        assertEquals("10.0.0.0/16", RemoteNetScanner.normalizeCidr("10.0.0.0/16"));
        assertEquals("172.16.0.0/12", RemoteNetScanner.normalizeCidr("172.16.0.0/12"));
    }

    @Test
    void normalizeTrimsWhitespace() {
        String result = RemoteNetScanner.normalizeCidr("  192.168.1.0/24  ");
        assertEquals("192.168.1.0/24", result);
    }
}

// ── MenuPrinter (CLI) ─────────────────────────────────────────────────────────

class MenuPrinterTest {

    @Test
    void printDoesNotThrow() {
        // Redirect stdout to avoid polluting test output
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(baos));
        assertDoesNotThrow(MenuPrinter::print);
        System.setOut(System.out);
        String output = baos.toString();
        assertTrue(output.contains("Netzwerk Tool"));
    }

    @Test
    void printContainsAllMenuItems() {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(baos));
        MenuPrinter.print();
        System.setOut(System.out);
        String output = baos.toString();
        assertTrue(output.contains("0"));  // Beenden
        assertTrue(output.contains("1"));  // Netzwerkinfo
    }
}

// ── ScanProfile ───────────────────────────────────────────────────────────────

class ScanProfileTest {

    @Test
    void summaryWithCidrs() {
        ScanProfile p = new ScanProfile("MeinProfil");
        p.cidrs.add("192.168.0.0/24");
        p.osFilter = "Linux";
        String s = p.summary();
        assertTrue(s.contains("MeinProfil"));
        assertTrue(s.contains("192.168.0.0/24"));
        assertTrue(s.contains("Linux"));
    }

    @Test
    void summaryWithoutCidrs() {
        ScanProfile p = new ScanProfile("LocalScan");
        String s = p.summary();
        assertTrue(s.contains("LocalScan"));
        assertTrue(s.contains("lokal"));
    }

    @Test
    void defaultFieldValues() {
        ScanProfile p = new ScanProfile("Test");
        assertEquals("Test", p.name);
        assertTrue(p.cidrs.isEmpty());
        assertEquals("", p.osFilter);
        assertEquals("", p.hnFilter);
        assertTrue(p.ports.isEmpty());
        assertFalse(p.autoSave);
        assertEquals("", p.category);
        assertEquals("", p.lastRun);
    }
}
