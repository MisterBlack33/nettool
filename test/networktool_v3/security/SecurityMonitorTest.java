package networktool_v3.security;

import main.java.networktool.security.SecurityMonitor;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityMonitorTest {

    @AfterEach
    void stop() {
        SecurityMonitor.getInstance().stop();
    }

    @Test
    void isActive_initiallyFalse() {
        assertFalse(SecurityMonitor.getInstance().isActive());
    }

    @Test
    void start_setsActive() {
        SecurityMonitor.getInstance().start("");
        assertTrue(SecurityMonitor.getInstance().isActive());
    }

    @Test
    void stop_clearsActive() {
        SecurityMonitor.getInstance().start("");
        SecurityMonitor.getInstance().stop();
        assertFalse(SecurityMonitor.getInstance().isActive());
    }

    @Test
    void startTwice_doesNotThrow() {
        SecurityMonitor.getInstance().start("");
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().start(""));
    }

    @Test
    void addToWhitelist_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().addToWhitelist("192.168.1.100"));
    }

    @Test
    void addToWhitelist_null_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().addToWhitelist(null));
    }

    @Test
    void addToWhitelist_blank_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().addToWhitelist("   "));
    }

    @Test
    void addBaseline_doesNotThrow() {
        assertDoesNotThrow(() ->
                SecurityMonitor.getInstance().addBaseline("10.0.0.1", "AA:BB:CC:DD:EE:FF"));
    }

    @Test
    void addBaseline_null_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().addBaseline(null, null));
    }

    @Test
    void stop_whenNotActive_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityMonitor.getInstance().stop());
    }
}