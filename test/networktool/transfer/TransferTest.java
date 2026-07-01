package networktool.transfer;

import main.java.networktool.transfer.BandwidthTester;
import main.java.networktool.transfer.FileClient;
import main.java.networktool.transfer.FileServer;
import networktool.util.PollHelper;
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

    @Nested
    class BandwidthTesterTest {

        @Test
        void testDownload_unreachable_negative() {
            assertTrue(BandwidthTester.testDownload("192.0.2.1") < 0);
        }

        @Test
        void testUpload_unreachable_negative() {
            assertTrue(BandwidthTester.testUpload("192.0.2.1") < 0);
        }

        @Test
        void testBoth_doesNotThrow() {
            assertDoesNotThrow(() -> BandwidthTester.testBoth("192.0.2.1"));
        }
    }

    @Nested
    class FileTransferTest {

        @Test
        void sendAndReceive_roundtrip(@TempDir Path tmp) throws Exception {
            assumeTrue(loopbackReachable(), "Loopback nicht erreichbar");
            int port;
            try (ServerSocket probe = new ServerSocket(0)) {
                port = probe.getLocalPort();
            }
            new FileServer(port).start();
            assertTrue(PollHelper.waitFor(() -> {
                try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
                catch (Exception e) { return false; }
            }, 2000));

            Path src = tmp.resolve("send.txt");
            Files.writeString(src, "hello transfer test");
            new FileClient("127.0.0.1", port).sendFile(src.toString());

            assertTrue(PollHelper.waitFor(() ->
                    Files.exists(Path.of("empfangen_send.txt")), 2000));

            Path received = Path.of("empfangen_send.txt");
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