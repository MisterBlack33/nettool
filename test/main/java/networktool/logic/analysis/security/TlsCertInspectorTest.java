package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TlsCertInspectorTest {

    @Test void inspect_unreachable_returnsEmpty() {
        assertTrue(TlsCertInspector.inspect("192.0.2.1").isEmpty());
    }

    @Test void inspect_doesNotThrow() {
        assertDoesNotThrow(() -> TlsCertInspector.inspect("192.0.2.1"));
    }

    @Test void inspect_localClosedPort_returnsEmpty() {
        // 127.0.0.1 ohne laufenden TLS-Dienst auf 443/8443 -> kein Handshake möglich
        assertTrue(TlsCertInspector.inspect("127.0.0.1").isEmpty());
    }
}
