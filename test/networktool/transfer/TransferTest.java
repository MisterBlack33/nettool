package networktool.transfer;

import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class TransferTest {

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    /** Findet einen freien Port und startet den BW-Server darauf. */
    static int startBwServerOnFreePort() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        BandwidthTester.startServerOnPort(port);
        for (int i = 0; i < 30; i++) {
            Thread.sleep(100);
            if (BandwidthTester.isServerReachable("127.0.0.1", port)) return port;
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════
    //  BandwidthTesterTest
    // ══════════════════════════════════════════════════════════════

    @Nested
    class BandwidthTesterTest {

        @Test
        void isServerReachable_unreachable() {
            assertFalse(BandwidthTester.isServerReachable("192.0.2.1", BandwidthTester.TEST_PORT));
        }

        @Test
        void testDownload_noServer_returnsNegative() {
            assertTrue(BandwidthTester.testDownload("192.0.2.1", BandwidthTester.TEST_PORT) < 0);
        }

        @Test
        void testUpload_noServer_returnsNegative() {
            assertTrue(BandwidthTester.testUpload("192.0.2.1", BandwidthTester.TEST_PORT) < 0);
        }

        @Test
        void testBoth_noServer_doesNotThrow() {
            assertDoesNotThrow(() -> BandwidthTester.testBoth("192.0.2.1"));
        }

        @BeforeEach
        void reduceTestVolume() {
            BandwidthTester.testBytes = 256 * 1024L;
        }

        @AfterEach
        void restoreTestVolume() {
            BandwidthTester.testBytes = BandwidthTester.TEST_BYTES;
        }

        @Test
        void testDownload_withServer_positive() throws Exception {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            int port = startBwServerOnFreePort();
            assumeTrue(port > 0, "BW-Server nicht gestartet");
            assertTrue(BandwidthTester.testDownload("127.0.0.1", port) > 0);
        }

        @Test
        void testUpload_withServer_positive() throws Exception {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            int port = startBwServerOnFreePort();
            assumeTrue(port > 0, "BW-Server nicht gestartet");
            assertTrue(BandwidthTester.testUpload("127.0.0.1", port) > 0);
        }

        @Nested
        class FileTransferTest {

            @Test
            void sendAndReceive_roundtrip(@TempDir Path tmp) throws Exception {
                assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
                int port;
                try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
                new FileServer(port).start();
                Thread.sleep(200);
                Path src = tmp.resolve("send.txt");
                Files.writeString(src, "hello transfer test");
                new FileClient("127.0.0.1", port).sendFile(src.toString());
                Thread.sleep(400);
                Path received = Path.of("empfangen_send.txt");
                assertTrue(Files.exists(received));
                assertEquals("hello transfer test", Files.readString(received));
                Files.deleteIfExists(received);
            }

            @Test
            void fileClient_missingFile_doesNotThrow() {
                assertDoesNotThrow(() ->
                        new FileClient("127.0.0.1", 19877).sendFile("/nonexistent/file.txt"));
            }

            @Test
            void fileServer_start_doesNotThrow() {
                assertDoesNotThrow(() -> new FileServer(19878).start());
            }
        }
    }
}