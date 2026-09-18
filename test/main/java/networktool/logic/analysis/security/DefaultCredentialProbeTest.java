package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCredentialProbeTest {

    @Test void checkFtpAnonymous_unreachable_returnsEmpty() {
        assertTrue(DefaultCredentialProbe.checkFtpAnonymous("192.0.2.1").isEmpty());
    }

    @Test void checkFtpAnonymous_doesNotThrow() {
        assertDoesNotThrow(() -> DefaultCredentialProbe.checkFtpAnonymous("192.0.2.1"));
    }

    @Test void checkHttpDefaultLogin_unreachable_returnsEmpty() {
        assertTrue(DefaultCredentialProbe.checkHttpDefaultLogin("192.0.2.1", "admin", "admin").isEmpty());
    }

    @Test void checkHttpDefaultLogin_doesNotThrow() {
        assertDoesNotThrow(() -> DefaultCredentialProbe.checkHttpDefaultLogin("192.0.2.1", "admin", "admin"));
    }
}
