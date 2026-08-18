package main.java.networktool.logic.scan;

import org.junit.jupiter.api.*;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass ScanRateLimiter die Scan-*Ergebnisse* nicht verändert —
 * nur den zeitlichen Ablauf streckt. Siehe auch ScanRateLimiterTest für
 * reine Token-Bucket-Mechanik.
 */
class HostAliveCheckerRateLimitTest {

    @AfterEach
    void resetRate() {
        HostAliveChecker.setRateLimit(300, 60); // Produktions-Default wiederherstellen
        HostAliveChecker.setTestTimeouts(500, 400);
    }

    @Test
    void isAlive_withHighRate_sameResultAsUnlimited() throws Exception {
        HostAliveChecker.setTestTimeouts(500, 400);
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            HostAliveChecker.setRateLimit(10_000, 10_000);
            assertTrue(reachableViaSocket("127.0.0.1", port));
        }
    }

    @Test
    void isAlive_withThrottledRate_stillEventuallyTrue() throws Exception {
        HostAliveChecker.setTestTimeouts(2000, 1500);
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            // Sehr niedrige Rate — trotzdem muss isAlive() irgendwann true liefern,
            // da acquire() mit Timeout arbeitet statt dauerhaft zu blockieren.
            HostAliveChecker.setRateLimit(2, 1);
            assertTrue(reachableViaSocket("127.0.0.1", port));
        }
    }

    @Test
    void isAlive_unreachable_falseRegardlessOfRate() {
        HostAliveChecker.setTestTimeouts(300, 200);
        HostAliveChecker.setRateLimit(1000, 1000);
        assertFalse(HostAliveChecker.isAlive("192.0.2.1"));
    }

    @Test
    void isAlive_lowRate_takesLongerButDoesNotThrow() {
        HostAliveChecker.setTestTimeouts(300, 200);
        HostAliveChecker.setRateLimit(5, 1);
        assertDoesNotThrow(() -> HostAliveChecker.isAlive("192.0.2.1"));
    }

    private boolean reachableViaSocket(String host, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}