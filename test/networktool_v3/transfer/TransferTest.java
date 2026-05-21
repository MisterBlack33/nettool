package networktool_v3.transfer;

import main.java.networktool_v3.transfer.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class TransferTest {

    // ══════════════════════════════════════════════════════════════
    //  BandwidthTester
    // ══════════════════════════════════════════════════════════════

    @Nested
    class BandwidthTesterTest {

        @Test
        void isServerReachable_unreachable() {
            assertFalse(BandwidthTester.isServerReachable("127.0.0.1"));
        }

        @Test
        void testDownload_noServer_returnsNegative() {
            double r = BandwidthTester.testDownload("127.0.0.1");
            assertTrue(r < 0);
        }

        @Test
        void testUpload_noServer_returnsNegative() {
            double r = BandwidthTester.testUpload("127.0.0.1");
            assertTrue(r < 0);
        }

        @Test
        void testBoth_noServer_doesNotThrow() {
            assertDoesNotThrow(() -> BandwidthTester.testBoth("127.0.0.1"));
        }

        @Test
        void serverStart_andReachable() throws InterruptedException {
            BandwidthTester.startServer();
            Thread.sleep(300);
            assertTrue(BandwidthTester.isServerReachable("127.0.0.1"));
        }

        @Test
        void testDownload_withServer_positive() throws InterruptedException {
            BandwidthTester.startServer();
            Thread.sleep(200);
            double r = BandwidthTester.testDownload("127.0.0.1");
            assertTrue(r > 0);
        }

        @Test
        void testUpload_withServer_positive() throws InterruptedException {
            BandwidthTester.startServer();
            Thread.sleep(200);
            double r = BandwidthTester.testUpload("127.0.0.1");
            assertTrue(r > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FileServer + FileClient + FileReceiver roundtrip
    // ══════════════════════════════════════════════════════════════

    @Nested
    class FileTransferTest {

        @Test
        void sendAndReceive_roundtrip(@TempDir Path tmp) throws Exception {
            int port = 19876;
            FileServer srv = new FileServer(port);
            srv.start();
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