package main.java.networktool.transfer;

import networktool.util.PollHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workstream B: risikobasierte Regressionstests für den Transfer-Ablauf
 * (FileClient → FileServer → FileReceiver). Deckt fünf bisher ungetestete
 * Verhaltensfälle ab: Null-Byte-Dateien, exakte Puffer-Vielfache,
 * Port-Kollision, parallele Clients und vorzeitig abgebrochene Übertragung.
 *
 * Nutzt ausschließlich Loopback-Sockets mit dynamisch vergebenen Ports
 * (kein Administratorrechte-Bedarf, kein externer Host).
 */
@Tag("slow")
class FileTransferRegressionTest {

    private static final String RECEIVED_PREFIX = "empfangen_";

    @AfterEach
    void cleanup() throws IOException {
        try (var stream = Files.list(Path.of("."))) {
            stream.filter(p -> p.getFileName().toString().startsWith(RECEIVED_PREFIX))
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }

    private int freePort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    private void awaitServerReady(int port) {
        assertTrue(PollHelper.waitFor(() -> {
            try (Socket s = new Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 300);
                return true;
            } catch (Exception e) { return false; }
        }, 2000), "Server sollte innerhalb von 2s erreichbar sein");
    }

    // ── 1) Null-Byte-Datei ───────────────────────────────────────────────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void emptyFile_transfersAsZeroByteFile(@TempDir Path tmp) throws Exception {
        int port = freePort();
        new FileServer(port).start();
        awaitServerReady(port);

        Path src = tmp.resolve("empty.txt");
        Files.writeString(src, "");
        new FileClient("127.0.0.1", port).sendFile(src.toString());

        Path received = Path.of(RECEIVED_PREFIX + "empty.txt");
        assertTrue(PollHelper.waitFor(() -> Files.exists(received), 2000));
        assertEquals(0L, Files.size(received));
    }

    // ── 2) Exaktes Vielfaches der internen Puffergröße (4096 Byte) ───────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void exactBufferMultiple_transfersCompletely(@TempDir Path tmp) throws Exception {
        int port = freePort();
        new FileServer(port).start();
        awaitServerReady(port);

        byte[] payload = new byte[4096 * 3]; // exakt 3 volle FileClient-Puffer, keine Restbytes
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i % 251);
        Path src = tmp.resolve("exact.bin");
        Files.write(src, payload);
        new FileClient("127.0.0.1", port).sendFile(src.toString());

        Path received = Path.of(RECEIVED_PREFIX + "exact.bin");
        assertTrue(PollHelper.waitFor(() -> Files.exists(received)
                && sizeOrZero(received) == payload.length, 3000));
        assertArrayEquals(payload, Files.readAllBytes(received));
    }

    private long sizeOrZero(Path p) {
        try { return Files.size(p); } catch (IOException e) { return -1; }
    }

    // ── 3) Port-Kollision: zweiter Server auf demselben Port ─────────────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void portAlreadyBound_secondServerDoesNotThrow_firstStaysUsable(@TempDir Path tmp) throws Exception {
        int port = freePort();
        FileServer first = new FileServer(port);
        first.start();
        awaitServerReady(port);

        // Zweiter Server auf demselben Port: Bind schlägt intern fehl,
        // darf aber weder den Aufrufer-Thread noch den ersten Server stören.
        assertDoesNotThrow(() -> new FileServer(port).start());

        Path src = tmp.resolve("still-works.txt");
        Files.writeString(src, "erster Server bleibt aktiv");
        new FileClient("127.0.0.1", port).sendFile(src.toString());

        Path received = Path.of(RECEIVED_PREFIX + "still-works.txt");
        assertTrue(PollHelper.waitFor(() -> Files.exists(received), 2000));
    }

    // ── 4) Zwei parallele Clients ohne Datenvermischung ──────────────────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentClients_bothFilesArriveIntact(@TempDir Path tmp) throws Exception {
        int port = freePort();
        new FileServer(port).start();
        awaitServerReady(port);

        Path srcA = tmp.resolve("client-a.txt");
        Path srcB = tmp.resolve("client-b.txt");
        Files.writeString(srcA, "Inhalt A ".repeat(500));
        Files.writeString(srcB, "Inhalt B ".repeat(500));

        Thread ta = new Thread(() -> new FileClient("127.0.0.1", port).sendFile(srcA.toString()));
        Thread tb = new Thread(() -> new FileClient("127.0.0.1", port).sendFile(srcB.toString()));
        ta.start(); tb.start();
        ta.join(3000); tb.join(3000);

        Path recA = Path.of(RECEIVED_PREFIX + "client-a.txt");
        Path recB = Path.of(RECEIVED_PREFIX + "client-b.txt");
        assertTrue(PollHelper.waitFor(() -> Files.exists(recA) && Files.exists(recB), 3000));
        assertEquals(Files.readString(srcA), Files.readString(recA));
        assertEquals(Files.readString(srcB), Files.readString(recB));
        Files.deleteIfExists(recA);
        Files.deleteIfExists(recB);
    }

    // ── 5) Abgebrochene Übertragung: kein Hängenbleiben ──────────────────

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void truncatedStream_serverReturnsWithoutHanging() throws Exception {
        int port = freePort();
        new FileServer(port).start();
        awaitServerReady(port);

        // Manuelles Low-Level-Protokoll: kündigt mehr Bytes an, als tatsächlich
        // gesendet werden, und schließt die Verbindung danach sofort.
        try (Socket socket = new Socket("127.0.0.1", port);
             java.io.DataOutputStream dos = new java.io.DataOutputStream(socket.getOutputStream())) {
            dos.writeUTF("truncated.bin");
            dos.writeLong(10_000L); // angekündigt: 10 KB
            dos.write(new byte[]{1, 2, 3, 4, 5}); // tatsächlich: 5 Byte
            dos.flush();
        } // Socket-Close beendet den Stream vorzeitig → FileReceiver muss dies überleben

        Path received = Path.of(RECEIVED_PREFIX + "truncated.bin");
        assertTrue(PollHelper.waitFor(() -> Files.exists(received), 3000),
                "FileReceiver darf bei abgebrochener Übertragung nicht hängen bleiben");
        assertTrue(Files.size(received) <= 10_000L);
        Files.deleteIfExists(received);
    }
}
