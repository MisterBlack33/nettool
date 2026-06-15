package main.java.networktool.logic.messaging;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MessagingPackageTest {

    @Test
    void tryListener_closedPort_returnsFalse() {
        // Verwende eine non-routable Dokumentationsadresse, die üblicherweise nicht erreichbar ist
        assertFalse(MessageDelivery.tryListener("192.0.2.1", "hello"));
    }

    @Test
    void tryWinRM_closedPort_returnsFalse() {
        assertFalse(MessageDelivery.tryWinRM("192.0.2.1", "hello"));
    }

    @Test
    void trySsh_closedPort_returnsFalse() {
        assertFalse(MessageDelivery.trySsh("192.0.2.1", "hello", false));
    }

    @Test
    void tryNtfy_invalidTopic_doesNotThrow() {
        assertDoesNotThrow(() -> MessageDelivery.tryNtfy("__invalid__topic__xyz__", "msg"));
    }

    @Test
    void readStream_emptyStream_returnsEmpty() throws Exception {
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
        String r = MessageDelivery.readStream(empty);
        assertEquals("", r);
    }

    @Test
    void readStream_withContent_returnsText() throws Exception {
        byte[] bytes = "hello\nworld\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        String r = MessageDelivery.readStream(in);
        assertTrue(r.contains("hello"));
        assertTrue(r.contains("world"));
    }

    @Test
    void timeout_constant_positive() {
        assertTrue(MessageDelivery.TIMEOUT_MS > 0);
    }
}