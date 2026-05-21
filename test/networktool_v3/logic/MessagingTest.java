package networktool_v3.logic;

import main.java.networktool_v3.logic.messaging.MessageSender;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MessagingTest {

    @Test
    void send_unreachableHost_doesNotThrow() {
        assertDoesNotThrow(() -> MessageSender.send("192.0.2.1", "test", ""));
    }

    @Test
    void send_noTopic_doesNotThrow() {
        assertDoesNotThrow(() -> MessageSender.send("192.0.2.1", "msg"));
    }

    @Test
    void showLocalToast_doesNotThrow() {
        assertDoesNotThrow(() -> MessageSender.showLocalToast("Title", "Body"));
    }

    @Test
    void startListener_doesNotThrow() {
        assertDoesNotThrow(MessageSender::startListener);
    }

    @Test
    void listenerPort_isCorrect() {
        assertEquals(9999, MessageSender.NETTOOL_LISTENER_PORT);
    }
}