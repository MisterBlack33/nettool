package networktool.gui.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NtfyJsonParserTest {

    @Test void parsesBasicMessageEvent() {
        String json = "{\"id\":\"abc123\",\"time\":1700000000,\"event\":\"message\","
                + "\"topic\":\"nettool-test\",\"title\":\"Hallo\",\"message\":\"Testnachricht\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertNotNull(ev);
        assertEquals("abc123", ev.id);
        assertEquals("message", ev.event);
        assertEquals("nettool-test", ev.topic);
        assertEquals("Hallo", ev.title);
        assertEquals("Testnachricht", ev.message);
    }

    @Test void parsesKeepaliveEventWithoutMessage() {
        String json = "{\"id\":\"xyz\",\"event\":\"keepalive\",\"topic\":\"nettool-test\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertNotNull(ev);
        assertEquals("keepalive", ev.event);
        assertNull(ev.message);
    }

    @Test void handlesEscapedQuotesInMessage() {
        String json = "{\"id\":\"1\",\"event\":\"message\",\"message\":\"sagt \\\"Hi\\\" zu dir\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertNotNull(ev);
        assertEquals("sagt \"Hi\" zu dir", ev.message);
    }

    @Test void handlesEscapedNewlineInMessage() {
        String json = "{\"id\":\"1\",\"event\":\"message\",\"message\":\"Zeile1\\nZeile2\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertEquals("Zeile1\nZeile2", ev.message);
    }

    @Test void missingFieldYieldsNull() {
        String json = "{\"id\":\"1\",\"event\":\"message\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertNotNull(ev);
        assertNull(ev.title);
        assertNull(ev.message);
    }

    @Test void nullInputReturnsNull() {
        assertNull(NtfyJsonParser.parse(null));
    }

    @Test void blankLineReturnsNull() {
        assertNull(NtfyJsonParser.parse(""));
        assertNull(NtfyJsonParser.parse("   "));
    }

    @Test void nonJsonInputReturnsNull() {
        assertNull(NtfyJsonParser.parse("not json at all"));
    }

    @Test void fieldOrderDoesNotMatter() {
        String json = "{\"message\":\"zuerst\",\"event\":\"message\",\"id\":\"9\"}";
        NtfyJsonParser.NtfyEvent ev = NtfyJsonParser.parse(json);
        assertEquals("9", ev.id);
        assertEquals("zuerst", ev.message);
    }
}
