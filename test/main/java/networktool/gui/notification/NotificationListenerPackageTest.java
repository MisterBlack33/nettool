package main.java.networktool.gui.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NotificationListener.parseNtfyJson (package-private).
 * NtfyEvent is private static → only null/non-null checks possible.
 */
class NotificationListenerPackageTest {

    @Test
    void parseEvent_validMessage_notNull() {
        String json = "{\"id\":\"abc\",\"event\":\"message\",\"topic\":\"test\"," +
                "\"title\":\"Hello\",\"message\":\"World\"}";
        assertNotNull(NotificationListener.parseNtfyJson(json));
    }

    @Test
    void parseEvent_openEvent_notNull() {
        assertNotNull(NotificationListener.parseNtfyJson("{\"event\":\"open\",\"topic\":\"test\"}"));
    }

    @Test
    void parseEvent_null_returnsNull() {
        assertNull(NotificationListener.parseNtfyJson(null));
    }

    @Test
    void parseEvent_notJsonObject_returnsNull() {
        assertNull(NotificationListener.parseNtfyJson("not json"));
    }

    @Test
    void parseEvent_emptyObject_notNull() {
        assertNotNull(NotificationListener.parseNtfyJson("{}"));
    }

    @Test
    void parseEvent_withEscapedChars_notNull() {
        String json = "{\"event\":\"message\",\"message\":\"line1\\nline2\"}";
        assertNotNull(NotificationListener.parseNtfyJson(json));
    }
}
