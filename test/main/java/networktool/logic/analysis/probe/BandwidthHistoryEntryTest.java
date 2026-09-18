package main.java.networktool.logic.analysis.probe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BandwidthHistoryEntryTest {

    @Test void roundtrip() {
        BandwidthHistoryEntry e = new BandwidthHistoryEntry(1000L, "10.0.0.1", 12.5, 3.25);
        BandwidthHistoryEntry parsed = BandwidthHistoryEntry.parse(e.toNdjson());
        assertNotNull(parsed);
        assertEquals(1000L, parsed.timestampMs());
        assertEquals("10.0.0.1", parsed.ip());
        assertEquals(12.5, parsed.downMbps());
        assertEquals(3.25, parsed.upMbps());
    }

    @Test void nullIp_defaultsToEmpty() {
        assertEquals("", new BandwidthHistoryEntry(1, null, 1, 1).ip());
    }

    @Test void parse_null_returnsNull()    { assertNull(BandwidthHistoryEntry.parse(null)); }
    @Test void parse_blank_returnsNull()   { assertNull(BandwidthHistoryEntry.parse("  ")); }
    @Test void parse_notJson_returnsNull() { assertNull(BandwidthHistoryEntry.parse("plain text")); }

    @Test void parse_missingField_returnsNull() {
        assertNull(BandwidthHistoryEntry.parse("{\"ts\":1,\"ip\":\"1.1.1.1\"}"));
    }

    @Test void toNdjson_escapesQuotes() {
        BandwidthHistoryEntry e = new BandwidthHistoryEntry(1, "host\"1", 1, 1);
        assertTrue(e.toNdjson().contains("\\\""));
    }

    @Test void negativeValues_parsedCorrectly() {
        BandwidthHistoryEntry e = new BandwidthHistoryEntry(5, "1.1.1.1", -1.5, 0.0);
        BandwidthHistoryEntry parsed = BandwidthHistoryEntry.parse(e.toNdjson());
        assertEquals(-1.5, parsed.downMbps());
    }
}
