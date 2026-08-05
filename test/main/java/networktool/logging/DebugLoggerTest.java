package main.java.networktool.logging;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebugLoggerTest {

    @Nested
    class DebugLogEntryTest {

        @Test void nullFields_defaulted() {
            var e = new DebugLogEntry(null, null, null);
            assertEquals("", e.timestamp());
            assertEquals("INFO", e.level());
            assertEquals("", e.message());
        }

        @Test void toLine_containsFields() throws Exception {
            var e = new DebugLogEntry("ts", "WARN", "msg");
            var m = DebugLogEntry.class.getDeclaredMethod("toLine");
            m.setAccessible(true);
            String line = (String) m.invoke(e);
            assertTrue(line.contains("ts") && line.contains("WARN") && line.contains("msg"));
        }

        @Test void toLine_sanitizesTabsAndNewlines() throws Exception {
            var e = new DebugLogEntry("ts", "INFO", "a\tb\nc");
            var m = DebugLogEntry.class.getDeclaredMethod("toLine");
            m.setAccessible(true);
            String line = (String) m.invoke(e);
            assertEquals(2, line.chars().filter(c -> c == '\t').count());
        }
    }

    @Nested
    class DebugLogFileTest {

        @TempDir Path tmp;

        @Test void append_and_readRecent() {
            DebugLogFile f = new DebugLogFile(tmp);
            f.append(new DebugLogEntry("ts", "INFO", "hello"));
            List<DebugLogEntry> entries = f.readRecent(10);
            assertEquals(1, entries.size());
            assertEquals("hello", entries.get(0).message());
        }

        @Test void readRecent_newestFirst() {
            DebugLogFile f = new DebugLogFile(tmp);
            f.append(new DebugLogEntry("ts", "INFO", "first"));
            f.append(new DebugLogEntry("ts", "INFO", "second"));
            assertEquals("second", f.readRecent(10).get(0).message());
        }

        @Test void readRecent_noFile_empty() {
            assertTrue(new DebugLogFile(tmp).readRecent(10).isEmpty());
        }

        @Test void clear_removesFile() {
            DebugLogFile f = new DebugLogFile(tmp);
            f.append(new DebugLogEntry("ts", "INFO", "x"));
            f.clear();
            assertTrue(f.readRecent(10).isEmpty());
        }

        @Test void clear_nonExistent_doesNotThrow() {
            assertDoesNotThrow(() -> new DebugLogFile(tmp).clear());
        }

        @Test void parse_invalidLine_null() {
            assertNull(DebugLogFile.parse(""));
            assertNull(DebugLogFile.parse(null));
            assertNull(DebugLogFile.parse("onlyone"));
        }

        @Test void parse_valid() {
            DebugLogEntry e = DebugLogFile.parse("ts\tWARN\tmsg");
            assertNotNull(e);
            assertEquals("WARN", e.level());
        }

        @Test void maxLines_respected() {
            DebugLogFile f = new DebugLogFile(tmp);
            for (int i = 0; i < 10; i++) f.append(new DebugLogEntry("ts", "INFO", "m" + i));
            assertEquals(3, f.readRecent(3).size());
        }
    }

    @Nested
    class DebugLoggerApiTest {

        @TempDir Path tmp;
        DebugLogger logger;

        @BeforeEach void setup() { logger = DebugLogger.getInstance(); logger.init(tmp); }
        @AfterEach  void teardown() { logger.shutdown(); }

        @Test void info_persisted() {
            logger.info("hello");
            assertTrue(logger.readRecent(10).stream().anyMatch(e -> "hello".equals(e.message())));
        }

        @Test void levels_setCorrectly() {
            logger.debug("d"); logger.warn("w"); logger.error("e");
            var recent = logger.readRecent(10);
            assertTrue(recent.stream().anyMatch(e -> "DEBUG".equals(e.level())));
            assertTrue(recent.stream().anyMatch(e -> "WARN".equals(e.level())));
            assertTrue(recent.stream().anyMatch(e -> "ERROR".equals(e.level())));
        }

        @Test void clear_removesEntries() {
            logger.info("x");
            logger.clear();
            assertTrue(logger.readRecent(10).isEmpty());
        }

        @Test void persistsAcrossReinit() {
            logger.info("persist-me");
            logger.init(tmp);
            assertTrue(logger.readRecent(10).stream().anyMatch(e -> "persist-me".equals(e.message())));
        }
    }
}
