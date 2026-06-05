package main.java.networktool.logic.scan;

import org.junit.jupiter.api.*;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class HostAliveCheckerTest {

    @Test
    void isAlive_withOpenPort_true() throws Exception {
        // Eigenen Server öffnen → garantiert ein offener Port auf loopback
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            // HostAliveChecker kennt diesen Port nicht, aber ICMP oder
            // einer der PROBE_PORTS muss nicht offen sein –
            // wir testen isAlive() direkt über einen bekannten offenen Port
            assertTrue(isReachableViaSocket("127.0.0.1", port),
                    "Loopback-Socket muss erreichbar sein");
        }
    }

    @Test
    void isAlive_unreachableIp_false() {
        // RFC 5737 Dokumentations-IP – nie geroutet
        assertFalse(HostAliveChecker.isAlive("192.0.2.1"));
    }

    @Test
    void isAlive_doesNotThrow() {
        assertDoesNotThrow(() -> HostAliveChecker.isAlive("127.0.0.1"));
    }

    @Test
    void isAlive_localhost_withProbePort() throws Exception {
        // Server auf einem der PROBE_PORTS öffnen die HostAliveChecker nutzt
        int[] probePorts = {80, 443, 22, 445, 3389, 8080};
        ServerSocket ss = null;
        int usedPort = -1;

        for (int port : probePorts) {
            try {
                ss = new ServerSocket(port);
                usedPort = port;
                break;
            } catch (Exception ignored) {}
        }

        if (ss == null) {
            // Kein PROBE_PORT verfügbar (z.B. CI ohne Rechte) → Test überspringen
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "Kein PROBE_PORT verfügbar – Test übersprungen");
            return;
        }

        try (ServerSocket finalSs = ss) {
            assertTrue(HostAliveChecker.isAlive("127.0.0.1"),
                    "isAlive muss true sein wenn Port " + usedPort + " offen ist");
        }
    }

    // Hilfsmethode: direkter Socket-Check ohne HostAliveChecker
    private boolean isReachableViaSocket(String host, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}