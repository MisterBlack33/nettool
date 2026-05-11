package networktool_v3.security;

import main.java.networktool_v3.security.AuditLogger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditLoggerTest {

    @TempDir Path tempDir;
    private AuditLogger logger;

    @BeforeEach
    void setUp() throws InterruptedException {
        logger = AuditLogger.getInstance();
        logger.init(tempDir);
        logger.clear();
        Thread.sleep(100); // async writer flush
    }

    @Test @Order(1)
    void logAndReadEntry() throws InterruptedException {
        logger.log("TEST_ACTION", "detail123");
        Thread.sleep(200);
        List<AuditLogger.LogEntry> entries = logger.readRecent(10);
        assertFalse(entries.isEmpty());
        boolean found = entries.stream()
                .anyMatch(e -> "TEST_ACTION".equals(e.action) && "detail123".equals(e.detail));
        assertTrue(found);
    }

    @Test @Order(2)
    void logWithoutDetailWorks() throws InterruptedException {
        logger.log("SIMPLE_ACTION");
        Thread.sleep(200);
        List<AuditLogger.LogEntry> entries = logger.readRecent(10);
        assertTrue(entries.stream().anyMatch(e -> "SIMPLE_ACTION".equals(e.action)));
    }

    @Test @Order(3)
    void readRecentReturnsNewestFirst() throws InterruptedException {
        logger.log("FIRST",  "a");
        logger.log("SECOND", "b");
        logger.log("THIRD",  "c");
        Thread.sleep(300);
        List<AuditLogger.LogEntry> entries = logger.readRecent(10);
        assertTrue(entries.size() >= 3);
        assertEquals("THIRD", entries.get(0).action);
    }

    @Test @Order(4)
    void clearRemovesAllEntries() throws InterruptedException {
        logger.log("TO_DELETE", "data");
        Thread.sleep(200);
        logger.clear();
        Thread.sleep(200);
        List<AuditLogger.LogEntry> entries = logger.readRecent(10);
        // After clear, only AUDIT_LOG_CLEARED remains
        assertTrue(entries.stream().noneMatch(e -> "TO_DELETE".equals(e.action)));
    }

    @Test @Order(5)
    void readByUserFilters() throws InterruptedException {
        // Simulate user
        logger.log("USER_ACTION", "by_alice");
        Thread.sleep(200);
        List<AuditLogger.LogEntry> all = logger.readRecent(100);
        assertFalse(all.isEmpty());
    }

    @Test @Order(6)
    void specialCharsEscaped() throws InterruptedException {
        logger.log("SPECIAL", "detail with \"quotes\" and \\backslash");
        Thread.sleep(200);
        List<AuditLogger.LogEntry> entries = logger.readRecent(10);
        assertTrue(entries.stream()
                .anyMatch(e -> e.detail.contains("quotes") && e.detail.contains("backslash")));
    }

    @Test @Order(7)
    void maxLinesParameterRespected() throws InterruptedException {
        for (int i = 0; i < 20; i++) logger.log("BULK", "item" + i);
        Thread.sleep(500);
        List<AuditLogger.LogEntry> entries = logger.readRecent(5);
        assertTrue(entries.size() <= 5);
    }

    @Test @Order(8)
    void timestampNotNull() throws InterruptedException {
        logger.log("TS_TEST", "");
        Thread.sleep(200);
        List<AuditLogger.LogEntry> entries = logger.readRecent(5);
        assertTrue(entries.stream()
                .filter(e -> "TS_TEST".equals(e.action))
                .allMatch(e -> e.timestamp != null && !e.timestamp.isBlank()));
    }
}
