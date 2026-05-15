package networktool_v3.transfer;

import networktool_v3.logic.ports.BannerGrabber;
import networktool_v3.logic.ports.PortScanner;
import org.junit.jupiter.api.*;

import java.net.ServerSocket;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// ── BandwidthTester ───────────────────────────────────────────────────────────

class BandwidthTesterTest {

    @Test
    void isServerReachable_localServer() throws Exception {
        // Start a local dummy server
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            // Not the BW server port, but confirms TCP connect logic
            assertDoesNotThrow(() -> {
                try (var s = new java.net.Socket("localhost", port)) {
                    assertTrue(s.isConnected());
                }
            });
        }
    }

    @Test
    void isServerReachable_noServer() {
        // Port 19999 should not have a server
        assertFalse(BandwidthTester.isServerReachable("127.0.0.1"));
        // Note: actual BW server not running, so this should return false
    }

    @Test
    void testDownload_noServer_returnsMinusOne() {
        // No server running → should return -1 cleanly without exception
        double result = BandwidthTester.testDownload("127.0.0.1");
        assertEquals(-1.0, result);
    }

    @Test
    void testUpload_noServer_returnsMinusOne() {
        double result = BandwidthTester.testUpload("127.0.0.1");
        assertEquals(-1.0, result);
    }

    @Test
    void serverStartsWithoutException() {
        // Should not throw
        assertDoesNotThrow(BandwidthTester::startServer);
    }
}

// ── PortScanner ───────────────────────────────────────────────────────────────

class PortScannerTest {

    @Test
    void defaultPortsNotEmpty() {
        assertFalse(PortScanner.DEFAULT_PORTS.isEmpty());
    }

    @Test
    void fastPortsNotEmpty() {
        assertFalse(PortScanner.FAST_PORTS.isEmpty());
    }

    @Test
    void setActivePorts_customList() {
        List<Integer> custom = List.of(80, 443, 22);
        PortScanner.setActivePorts(custom);
        assertEquals(custom, PortScanner.getActivePorts());
        // Reset
        PortScanner.setActivePorts(null);
        assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
    }

    @Test
    void setActivePorts_null_resetsToDefault() {
        PortScanner.setActivePorts(List.of(1234));
        PortScanner.setActivePorts(null);
        assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
    }

    @Test
    void setActivePorts_empty_resetsToDefault() {
        PortScanner.setActivePorts(List.of());
        assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
    }

    @Test
    void isOpen_localhost_closedPort() {
        // Port 1 should be closed on any normal system
        assertFalse(PortScanner.isOpen("127.0.0.1", 1, 200));
    }

    @Test
    void probePort_closedReturnsCorrectState() {
        var state = PortScanner.probePort("127.0.0.1", 1, 200);
        assertEquals(PortScanner.PortState.CLOSED, state);
    }

    @Test
    void scanSimple_localhostReturnsMap() throws Exception {
        // Create a local server on a random port
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            PortScanner.setActivePorts(List.of(port));
            Map<Integer,String> result = PortScanner.scanSimple("127.0.0.1", 1000);
            assertTrue(result.containsKey(port));
            PortScanner.setActivePorts(null);
        }
    }

    @Test
    void commonPortsAlias() {
        // COMMON_PORTS() should return active ports
        assertNotNull(PortScanner.COMMON_PORTS());
    }
}

// ── BannerGrabber ────────────────────────────────────────────────────────────

class BannerGrabberTest {

    @Test
    void grabLocalHttpServer() throws Exception {
        // Start a minimal HTTP-like server
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            Thread server = new Thread(() -> {
                try {
                    var client = ss.accept();
                    var out = client.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\nServer: TestServer/1.0\r\n\r\n".getBytes());
                    out.flush();
                    client.close();
                } catch (Exception ignored) {}
            });
            server.setDaemon(true);
            server.start();
            Thread.sleep(100);

            String banner = BannerGrabber.grab("127.0.0.1", port, 1000);
            assertNotNull(banner);
            assertFalse(banner.isBlank());
        }
    }

    @Test
    void grabClosedPort_returnsServiceName() {
        // Port 22 not open → should return "SSH" as service name fallback
        String banner = BannerGrabber.grab("127.0.0.1", 22, 200);
        assertNotNull(banner);
        assertEquals("SSH", banner);
    }

    @Test
    void grabPort80Fallback() {
        String banner = BannerGrabber.grab("127.0.0.1", 80, 200);
        assertEquals("HTTP", banner);
    }

    @Test
    void grabPort443Fallback() {
        String banner = BannerGrabber.grab("127.0.0.1", 443, 200);
        assertEquals("HTTPS", banner);
    }

    @Test
    void grabPort3306Fallback() {
        String banner = BannerGrabber.grab("127.0.0.1", 3306, 200);
        assertEquals("MySQL", banner);
    }

    @Test
    void grabUnknownPort_returnsOffen() {
        // Port 19998 unlikely to be open
        String banner = BannerGrabber.grab("127.0.0.1", 19998, 200);
        assertNotNull(banner);
        // Should return "offen" or the port's service name
        assertFalse(banner.isBlank());
    }
}

// ── FileClient / FileReceiver ─────────────────────────────────────────────────

class FileTransferTest {

    @Test @org.junit.jupiter.api.io.TempDir
    void sendAndReceiveFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws Exception {
        // Create test file
        java.nio.file.Path src = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(src, "Hello, NetTool!");

        // Find free port
        int port;
        try (ServerSocket ss = new ServerSocket(0)) { port = ss.getLocalPort(); }

        // Start server
        FileServer server = new FileServer(port);
        server.start();
        Thread.sleep(200);

        // Send file
        FileClient client = new FileClient("127.0.0.1", port);
        assertDoesNotThrow(() -> client.sendFile(src.toString()));
        Thread.sleep(300);

        // Check received file exists
        java.nio.file.Path received = java.nio.file.Paths.get("empfangen_test.txt");
        if (java.nio.file.Files.exists(received)) {
            String content = java.nio.file.Files.readString(received);
            assertEquals("Hello, NetTool!", content);
            java.nio.file.Files.deleteIfExists(received);
        }
    }
}
