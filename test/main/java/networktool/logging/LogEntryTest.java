package main.java.networktool.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest {

    @Test void nullFields_defaulted() {
        LogEntry e = new LogEntry(null, null, null, null, null, null);
        assertEquals("", e.timestamp());
        assertEquals("", e.category());
        assertEquals("", e.user());
        assertEquals("", e.level());
        assertEquals("", e.action());
        assertEquals("", e.detail());
    }

    @Test void toNdjson_containsAllFields() {
        String json = new LogEntry("ts", "AUDIT", "bob", "WARN", "SCAN", "cidr").toNdjson();
        assertTrue(json.contains("\"v\":2"));
        assertTrue(json.contains("\"ts\":\"ts\""));
        assertTrue(json.contains("\"category\":\"AUDIT\""));
        assertTrue(json.contains("\"user\":\"bob\""));
        assertTrue(json.contains("\"level\":\"WARN\""));
        assertTrue(json.contains("\"action\":\"SCAN\""));
        assertTrue(json.contains("\"detail\":\"cidr\""));
    }

    @Test void toNdjson_escapesQuotes() {
        assertTrue(new LogEntry("ts", "C", "u", "L", "A", "say \"hi\"").toNdjson().contains("\\\"hi\\\""));
    }

    @Test void toNdjson_escapesBackslash() {
        assertTrue(new LogEntry("ts", "C", "u", "L", "A", "a\\b").toNdjson().contains("\\\\"));
    }

    @Test void toNdjson_escapesNewline() {
        assertTrue(new LogEntry("ts", "C", "u", "L", "A", "line1\nline2").toNdjson().contains("\\n"));
    }

    @Test void toNdjson_escapesTab() {
        assertTrue(new LogEntry("ts", "C", "u", "L", "A", "a\tb").toNdjson().contains("\\t"));
    }

    @Test void toNdjson_stripsCarriageReturn() {
        assertFalse(new LogEntry("ts", "C", "u", "L", "A", "a\rb").toNdjson().contains("\r"));
    }
}