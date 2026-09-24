package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusTagsTest {
    @Test void tags_areNotBlank() {
        assertFalse(StatusTags.OK.isBlank());
        assertFalse(StatusTags.FEHLER.isBlank());
        assertFalse(StatusTags.WARN.isBlank());
        assertFalse(StatusTags.INFO.isBlank());
    }
    @Test void tags_areDistinct() {
        assertEquals(4, java.util.Set.of(StatusTags.OK, StatusTags.FEHLER, StatusTags.WARN, StatusTags.INFO).size());
    }
    @Test void tags_areBracketed() {
        assertTrue(StatusTags.OK.startsWith("[") && StatusTags.OK.endsWith("]"));
        assertTrue(StatusTags.FEHLER.startsWith("[") && StatusTags.FEHLER.endsWith("]"));
    }
}
