package main.java.networktool.logic.messaging;

import com.sun.net.httpserver.HttpServer;
import main.java.networktool.logging.DebugLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class WebhookDeliveryTest {

    @TempDir Path logDir;
    HttpServer server;
    final AtomicReference<String> body = new AtomicReference<>();
    volatile int status = 200;

    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", ex -> {
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(status, -1);
            ex.close();
        });
        server.start();
    }

    @AfterEach void stop() { server.stop(0); }

    private String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook"; }

    @Test void send_ok_true_andPayloadDelivered() {
        assertTrue(WebhookDelivery.send(url(), "hello"));
        assertTrue(body.get().contains("\"text\":\"hello\""));
        assertTrue(body.get().contains("\"source\":\"NetTool\""));
    }

    @Test void send_serverError_false() {
        status = 500;
        assertFalse(WebhookDelivery.send(url(), "x"));
    }

    @Test void send_invalidUrl_false() {
        assertFalse(WebhookDelivery.send("ftp://example.com", "x"));
        assertFalse(WebhookDelivery.send("not a url", "x"));
        assertFalse(WebhookDelivery.send(null, "x"));
        assertFalse(WebhookDelivery.send("  ", "x"));
    }

    @Test void send_closedPort_falseNotThrow() {
        assertFalse(WebhookDelivery.send("http://127.0.0.1:1/x", "x"));
    }

    @Test void send_failureDoesNotLogUrlOrPayloadSecrets() throws IOException {
        int closedPort;
        try (ServerSocket reservation = new ServerSocket(
                0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            closedPort = reservation.getLocalPort();
        }
        DebugLogger logger = DebugLogger.getInstance();
        logger.init(logDir);
        try {
            assertFalse(WebhookDelivery.send("http://127.0.0.1:" + closedPort
                    + "/secret-path?token=secret-token", "private-message"));
            String log = logger.readRecent(10).stream()
                    .map(entry -> entry.message())
                    .reduce("", (a, b) -> a + "\n" + b);
            assertFalse(log.contains("secret-path"));
            assertFalse(log.contains("secret-token"));
            assertFalse(log.contains("private-message"));
        } finally {
            logger.shutdown();
        }
    }

    @Test void parse_httpAndHttps_accepted() {
        assertNotNull(WebhookDelivery.parse("http://a.b/c"));
        assertNotNull(WebhookDelivery.parse("HTTPS://a.b/c"));
    }

    @Test void parse_noHost_null() {
        assertNull(WebhookDelivery.parse("http:///path"));
    }

    @Test void buildPayload_escapesQuotesAndNewlines() {
        String p = WebhookDelivery.buildPayload("say \"hi\"\nnow");
        assertTrue(p.contains("\\\"hi\\\""));
        assertTrue(p.contains("\\n"));
        assertTrue(p.contains("\"timestamp\""));
    }

    @Test void timeout_positive() { assertTrue(WebhookDelivery.TIMEOUT_MS > 0); }
}
