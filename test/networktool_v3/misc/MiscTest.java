package networktool_v3.misc;

import networktool_v3.gui.notification.LocalToast;
import networktool_v3.gui.notification.NotificationListener;
import networktool_v3.gui.security.NoteEncryption;
import networktool_v3.logic.analysis.WakeOnLan;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

// ── WakeOnLan ─────────────────────────────────────────────────────────────────

class WakeOnLanTest {

    @Test
    void deriveBroadcast_slash24() {
        assertEquals("192.168.1.255", WakeOnLan.deriveBroadcast("192.168.1.42", 24));
    }

    @Test
    void deriveBroadcast_slash16() {
        assertEquals("192.168.255.255", WakeOnLan.deriveBroadcast("192.168.5.10", 16));
    }

    @Test
    void deriveBroadcast_slash8() {
        assertEquals("10.255.255.255", WakeOnLan.deriveBroadcast("10.1.2.3", 8));
    }

    @Test
    void deriveBroadcast_slash32() {
        assertEquals("192.168.1.42", WakeOnLan.deriveBroadcast("192.168.1.42", 32));
    }

    @Test
    void deriveBroadcast_invalid() {
        // Invalid IP → fallback 255.255.255.255
        assertEquals("255.255.255.255", WakeOnLan.deriveBroadcast("not-an-ip", 24));
    }

    @Test
    void extractMacFromHostname_valid() {
        String mac = WakeOnLan.extractMacFromHostname("server [AA:BB:CC:DD:EE:FF]");
        assertEquals("AA:BB:CC:DD:EE:FF", mac);
    }

    @Test
    void extractMacFromHostname_dashFormat() {
        String mac = WakeOnLan.extractMacFromHostname("pc [AA-BB-CC-DD-EE-FF]");
        assertEquals("AA-BB-CC-DD-EE-FF", mac);
    }

    @Test
    void extractMacFromHostname_noBrackets() {
        assertNull(WakeOnLan.extractMacFromHostname("simple-hostname"));
    }

    @Test
    void extractMacFromHostname_null() {
        assertNull(WakeOnLan.extractMacFromHostname(null));
    }
}

// ── NoteEncryption ────────────────────────────────────────────────────────────

class NoteEncryptionTest {

    @Test
    void encryptAndDecrypt() throws Exception {
        NoteEncryption.setPassword("testPassword123!");
        String plain     = "Meine geheime Notiz";
        String encrypted = NoteEncryption.encrypt(plain);

        assertTrue(NoteEncryption.isEncrypted(encrypted));
        assertTrue(encrypted.startsWith(NoteEncryption.PREFIX));

        String decrypted = NoteEncryption.decrypt(encrypted, "testPassword123!");
        assertEquals(plain, decrypted);

        NoteEncryption.clearSession();
    }

    @Test
    void wrongPasswordReturnsErrorMessage() throws Exception {
        NoteEncryption.setPassword("correctPass");
        String encrypted = NoteEncryption.encrypt("secret");
        String result    = NoteEncryption.decrypt(encrypted, "wrongPass");
        assertEquals("[Falsches Passwort]", result);
        NoteEncryption.clearSession();
    }

    @Test
    void noSessionKeyReturnsPlaintext() {
        NoteEncryption.clearSession();
        String plain = "not encrypted";
        assertEquals(plain, NoteEncryption.encrypt(plain));
    }

    @Test
    void isEncrypted_prefixDetected() {
        assertTrue(NoteEncryption.isEncrypted(NoteEncryption.PREFIX + "abc"));
        assertFalse(NoteEncryption.isEncrypted("plain text"));
        assertFalse(NoteEncryption.isEncrypted(null));
    }

    @Test
    void encryptNull_returnsNull() {
        NoteEncryption.clearSession();
        assertNull(NoteEncryption.encrypt(null));
    }

    @Test
    void decryptNonEncrypted_returnsAsIs() {
        String s = "plain text";
        assertEquals(s, NoteEncryption.decrypt(s, "anyPass"));
    }

    @Test
    void clearSessionRemovesKey() throws Exception {
        NoteEncryption.setPassword("pass");
        assertTrue(NoteEncryption.hasSessionKey());
        NoteEncryption.clearSession();
        assertFalse(NoteEncryption.hasSessionKey());
    }
}

// ── NotificationListener JSON parser ─────────────────────────────────────────

class NotificationListenerParserTest {

    @Test
    void parseMessage() {
        String json = "{\"id\":\"abc123\",\"time\":1234567890,\"event\":\"message\","
                + "\"topic\":\"mytopic\",\"title\":\"Titel\",\"message\":\"Hallo Welt\"}";
        var ev = NotificationListener.parseNtfyJson(json);
        assertNotNull(ev);
        assertEquals("abc123",    ev.id);
        assertEquals("message",   ev.event);
        assertEquals("mytopic",   ev.topic);
        assertEquals("Titel",     ev.title);
        assertEquals("Hallo Welt",ev.message);
    }

    @Test
    void parseKeepalive() {
        String json = "{\"id\":\"xyz\",\"time\":1234,\"event\":\"keepalive\",\"topic\":\"t\"}";
        var ev = NotificationListener.parseNtfyJson(json);
        assertNotNull(ev);
        assertEquals("keepalive", ev.event);
        assertNull(ev.message);
    }

    @Test
    void parseNull_returnsNull() {
        assertNull(NotificationListener.parseNtfyJson(null));
    }

    @Test
    void parseEmpty_returnsNull() {
        assertNull(NotificationListener.parseNtfyJson(""));
        assertNull(NotificationListener.parseNtfyJson("not-json"));
    }

    @Test
    void parseMessageWithEscapedChars() {
        String json = "{\"id\":\"1\",\"event\":\"message\",\"topic\":\"t\","
                + "\"message\":\"line1\\nline2\",\"title\":\"say \\\"hi\\\"\"}";
        var ev = NotificationListener.parseNtfyJson(json);
        assertNotNull(ev);
        assertTrue(ev.message.contains("\n") || ev.message.contains("line"));
        assertTrue(ev.title.contains("hi"));
    }

    @Test
    void parseMessageFieldNotConfusedWithEventValue() {
        // Bug-Regression: "event":"message" darf nicht als message-Feld gelesen werden
        String json = "{\"id\":\"1\",\"event\":\"message\",\"topic\":\"t\","
                + "\"title\":\"T\",\"message\":\"actual message text\"}";
        var ev = NotificationListener.parseNtfyJson(json);
        assertNotNull(ev);
        assertEquals("actual message text", ev.message);
        assertEquals("message", ev.event);
    }
}

// ── LocalToast PowerShell escape ─────────────────────────────────────────────

class LocalToastTest {

    @Test
    void psEscape_singleQuote() {
        assertEquals("it''s", LocalToast.ps("it's"));
    }

    @Test
    void psEscape_newline() {
        assertEquals("line1 line2", LocalToast.ps("line1\nline2"));
    }

    @Test
    void psEscape_carriageReturn() {
        assertEquals("no cr", LocalToast.ps("no\rcr"));
    }

    @Test
    void psEscape_null() {
        assertEquals("", LocalToast.ps(null));
    }

    @Test
    void psEscape_noSpecialChars() {
        assertEquals("Hello World", LocalToast.ps("Hello World"));
    }
}
