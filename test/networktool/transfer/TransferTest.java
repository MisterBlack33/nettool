package networktool.transfer;

import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class TransferTest {

    static boolean loopbackReachable() {
        try {
            return InetAddress.getByName("127.0.0.1").isReachable(500);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Startet BW-Server und wartet bis er wirklich erreichbar ist (max 3 s).
     */
    static boolean startBwServerAndWait() throws InterruptedException {
        BandwidthTester.startServer();
        for (int i = 0; i < 30; i++) {
            Thread.sleep(100);
            if (BandwidthTester.isServerReachable("127.0.0.1")) return true;
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    //  BandwidthTesterTest
    // ══════════════════════════════════════════════════════════════

    @Nested
    class BandwidthTesterTest {

        @Test
        void isServerReachable_unreachable() {
            assertFalse(BandwidthTester.isServerReachable("192.0.2.1"));
        }

        @Test
        void testDownload_noServer_returnsNegative() {
            assertTrue(BandwidthTester.testDownload("192.0.2.1") < 0);
        }

        @Test
        void testUpload_noServer_returnsNegative() {
            assertTrue(BandwidthTester.testUpload("192.0.2.1") < 0);
        }

        @Test
        void testBoth_noServer_doesNotThrow() {
            assertDoesNotThrow(() -> BandwidthTester.testBoth("192.0.2.1"));
        }

        @Test
        void serverStart_andReachable() throws InterruptedException {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assumeTrue(startBwServerAndWait(), "BW-Server nicht gestartet");
            assertTrue(BandwidthTester.isServerReachable("127.0.0.1"));
        }

        @BeforeEach
        void reduceTestVolume() {
            // 256 KB statt 20 MB — Loopback-Test soll schnell sein
            BandwidthTester.testBytes = 256 * 1024L;
        }

        @AfterEach
        void restoreTestVolume() {
            BandwidthTester.testBytes = BandwidthTester.TEST_BYTES;
        }

        @Test
        void testDownload_withServer_positive() throws InterruptedException {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assumeTrue(startBwServerAndWait(), "BW-Server nicht erreichbar");
            assertTrue(BandwidthTester.testDownload("127.0.0.1") > 0);
        }

        @Test
        void testUpload_withServer_positive() throws InterruptedException {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            assumeTrue(startBwServerAndWait(), "BW-Server nicht erreichbar");
            assertTrue(BandwidthTester.testUpload("127.0.0.1") > 0);
        }

        // ══════════════════════════════════════════════════════════════
        //  FileTransferTest
        // ══════════════════════════════════════════════════════════════

        @Nested
        class FileTransferTest {

            @Test
            void sendAndReceive_roundtrip(@TempDir Path tmp) throws Exception {
                assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
                int port = 19876;
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