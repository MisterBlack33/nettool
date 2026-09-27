package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressionstest zu Finding 2: ntfy-Topics wurden bisher unvalidiert direkt
 * in HTTP-URLs eingebettet (MessageDelivery.tryNtfy, NtfySubscriptions) —
 * dieser Validator erzwingt jetzt ein sicheres Zeichenset vor jeder Nutzung.
 */
class PlatformSupportNtfyTopicTest {

    @Test void isSafeNtfyTopic_validAlphanumeric()      { assertTrue(PlatformSupport.isSafeNtfyTopic("my-topic_123")); }
    @Test void isSafeNtfyTopic_null_rejected()          { assertFalse(PlatformSupport.isSafeNtfyTopic(null)); }
    @Test void isSafeNtfyTopic_empty_rejected()         { assertFalse(PlatformSupport.isSafeNtfyTopic("")); }
    @Test void isSafeNtfyTopic_slash_rejected()         { assertFalse(PlatformSupport.isSafeNtfyTopic("evil.com/x")); }
    @Test void isSafeNtfyTopic_crlf_rejected()          { assertFalse(PlatformSupport.isSafeNtfyTopic("topic\r\nX-Injected: 1")); }
    @Test void isSafeNtfyTopic_queryChars_rejected()    { assertFalse(PlatformSupport.isSafeNtfyTopic("topic?x=1&y=2")); }
    @Test void isSafeNtfyTopic_shellMetachars_rejected(){ assertFalse(PlatformSupport.isSafeNtfyTopic("__test__'; calc; '")); }
    @Test void isSafeNtfyTopic_tooLong_rejected()       { assertFalse(PlatformSupport.isSafeNtfyTopic("a".repeat(65))); }
    @Test void isSafeNtfyTopic_maxLength_accepted()     { assertTrue(PlatformSupport.isSafeNtfyTopic("a".repeat(64))); }
}
