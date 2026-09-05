package main.java.networktool.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogFileBaseTest {

    @TempDir Path tmp;

    private LogFileBase noLegacy() {
        return new LogFileBase(tmp, "test.log", 100, line -> null);
    }

    @Test void append_and_readRecent() {
        LogFileBase f = noLegacy();
        f.append(new LogEntry("ts", "CAT", "u", "", "ACTION", "detail"));
        List<LogEntry> entries = f.readRecent(10);
        assertEquals(1, entries.size());
        assertEquals("ACTION", entries.get(0).action());
    }

    @Test void readRecent_newestFirst() {
        LogFileBase f = noLegacy();
        f.append(new LogEntry("ts", "C", "u", "", "FIRST", ""));
        f.append(new LogEntry("ts", "C", "u", "", "SECOND", ""));
        assertEquals("SECOND", f.readRecent(10).get(0).action());
    }

    @Test void readRecent_noFile_empty() {
        assertTrue(noLegacy().readRecent(10).isEmpty());
    }

    @Test void clear_removesEntries() {
        LogFileBase f = noLegacy();
        f.append(new LogEntry("ts", "C", "u", "", "A", ""));
        f.clear();
        assertTrue(f.readRecent(10).isEmpty());
    }

    @Test void clear_nonExistent_doesNotThrow() {
        assertDoesNotThrow(() -> noLegacy().clear());
    }

    @Test void maxLines_respected() {
        LogFileBase f = noLegacy();
        for (int i = 0; i < 10; i++) f.append(new LogEntry("ts", "C", "u", "", "A" + i, ""));
        assertEquals(3, f.readRecent(3).size());
    }

    @Test void legacyParser_usedForNonJsonLines() throws Exception {
        LogFileBase f = new LogFileBase(tmp, "legacy.log", 100,
                line -> "legacy-line".equals(line) ? new LogEntry("ts", "C", "", "", "LEGACY", "") : null);
        Files.writeString(tmp.resolve("legacy.log"), "legacy-line\n");
        List<LogEntry> entries = f.readRecent(10);
        assertEquals(1, entries.size());
        assertEquals("LEGACY", entries.get(0).action());
    }

    @Test void parseNdjson_missingTimestamp_returnsNull() {
        assertNull(LogFileBase.parseNdjson("{\"action\":\"A\"}"));
    }

    @Test void parseNdjson_null_returnsNull() {
        assertNull(LogFileBase.parseNdjson(null));
    }

    @Test void parseNdjson_nonJson_returnsNull() {
        assertNull(LogFileBase.parseNdjson("plain text"));
    }

    @Test void parseNdjson_roundtrip() {
        LogEntry original = new LogEntry("ts", "AUDIT", "bob", "", "LOGIN", "ok");
        LogEntry parsed = LogFileBase.parseNdjson(original.toNdjson());
        assertNotNull(parsed);
        assertEquals(original.timestamp(), parsed.timestamp());
        assertEquals(original.action(), parsed.action());
        assertEquals(original.detail(), parsed.detail());
    }

    @Test void nowFormatted_notBlank() {
        assertFalse(LogFileBase.nowFormatted().isBlank());
    }
}