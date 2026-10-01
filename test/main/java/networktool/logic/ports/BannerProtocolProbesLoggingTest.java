package main.java.networktool.logic.ports;

import main.java.networktool.logging.DebugLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BannerProtocolProbesLoggingTest {

    @TempDir Path logDir;
    private final DebugLogger logger = DebugLogger.getInstance();

    @BeforeEach
    void initializeLogger() {
        logger.init(logDir);
    }

    @AfterEach
    void stopLogger() {
        logger.shutdown();
    }

    @Test
    void refusedHttpProbe_preservesFallbackAndLogsWithoutExceptionMessage() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) {
            port = reservation.getLocalPort();
        }

        assertEquals("HTTP", BannerProtocolProbes.grabHttp("127.0.0.1", port, 200, false));

        var entry = logger.readRecent(10).stream()
                .filter(item -> item.message().contains("http-banner"))
                .findFirst().orElseThrow();
        assertEquals("FINE", entry.level());
        assertEquals("[BannerProtocolProbes] [CONNECTION_REFUSED] 127.0.0.1:" + port
                + " scan=unknown op=http-banner", entry.message());
    }
}
