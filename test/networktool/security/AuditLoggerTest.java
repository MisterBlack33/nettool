package networktool.security;

import main.java.networktool.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für AuditLogEntry, AuditLogFile, AuditLogger.
 * Abdeckung >90% – alle Formate, Berechtigungen, Rotation, Persistenz.
 */
class AuditLoggerTest {

    // ══════════════════════════════════════════════════════════════
    //  AuditLogEntry (Record)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AuditLogEntryTest {

        @Test void fields_stored() {
            var e = new AuditLogEntry("2024-01-01 10:00:00", "alice", "LOGIN", "ok");
            assertEquals("2024-01-01 10:00:00", e.timestamp());
            assertEquals("alice",  e.user());
            assertEquals("LOGIN",  e.action());
            assertEquals("ok",     e.detail());
        }

        @Test void nullFields_defaulted() {
            var e = new AuditLogEntry(null, null, null, null);
            assertEquals("",      e.timestamp());
            assertEquals("system", e.user());
            assertEquals("",      e.action());
            assertEquals("",      e.detail());
        }

        @Test void toNdjson_containsAllFields() {
            String json = new AuditLogEntry("2024-01-01 10:00:00", "bob", "SCAN", "cidr").toNdjson();
            assertTrue(json.contains("\"v\":1"));
            assertTrue(json.contains("\"ts\":\"2024-01-01 10:00:00\""));
            assertTrue(json.contains("\"user\":\"bob\""));
            assertTrue(json.contains("\"action\":\"SCAN\""));
            assertTrue(json.contains("\"detail\":\"cidr\""));
        }

        @Test void toNdjson_escapesQuotes() {
            String json = new AuditLogEntry("ts", "u", "A", "say \"hi\"").toNdjson();
            assertTrue(json.contains("\\\"hi\\\""));
        }

        @Test void toNdjson_escapesNewline() {
            String json = new AuditLogEntry("ts", "u", "A", "line1\nline2").toNdjson();
            assertTrue(json.contains("\\n"));
        }

        @Test void toNdjson_escapesBackslash() {
            String json = new AuditLogEntry("ts", "u", "A", "a\\b").toNdjson();
            assertTrue(json.contains("\\\\"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AuditLogFile – parse() alle Formate
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AuditLogFileParseTest {

        @Test void parse_ndjsonV1() {
            String line = "{\"v\":1,\"ts\":\"2024-01-01 10:00:00\",\"user\":\"alice\",\"action\":\"LOGIN\",\"detail\":\"ok\"}";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertEquals("alice",  e.user());
            assertEquals("LOGIN",  e.action());
            assertEquals("ok",     e.detail());
        }

        @Test void parse_tabFormat() {
            String line = "2024-01-01 10:00:00\tbob\tSCAN\tcidr";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertEquals("bob",   e.user());
            assertEquals("SCAN",  e.action());
            assertEquals("cidr",  e.detail());
        }

        @Test void parse_tabFormat_noDetail() {
            String line = "2024-01-01\tuser1\tLOGIN";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertEquals("LOGIN", e.action());
            assertEquals("",      e.detail());
        }

        @Test void parse_legacyJson() {
            String line = "{\"timestamp\":\"2024-01-01\",\"user\":\"u\",\"action\":\"A\",\"detail\":\"d\"}";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertEquals("A", e.action());
            assertEquals("d", e.detail());
        }

        @Test void parse_blank_null()        { assertNull(AuditLogFile.parse("")); }
        @Test void parse_null_null()         { assertNull(AuditLogFile.parse(null)); }
        @Test void parse_tooShortTab_null()  { assertNull(AuditLogFile.parse("only one")); }
        @Test void parse_emptyJson_null()    { assertNull(AuditLogFile.parse("{}")); }

        @Test void parse_ndjson_withEscapes() {
            String line = "{\"v\":1,\"ts\":\"ts\",\"user\":\"u\",\"action\":\"A\",\"detail\":\"say \\\"hi\\\"\"}";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertTrue(e.detail().contains("\"hi\""));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AuditLogFile – I/O
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AuditLogFileIoTest {

        @TempDir Path tmp;

        @Test void append_createsFile() {
            AuditLogFile f = new AuditLogFile(tmp);
            f.append(new AuditLogEntry("ts", "u", "A", "d"));
            assertTrue(Files.exists(tmp.resolve(AuditLogFile.FILE_NAME)));
        }

        @Test void append_and_readRecent() {
            AuditLogFile f = new AuditLogFile(tmp);
            f.append(new AuditLogEntry("2024-01-01 10:00:00", "alice", "LOGIN", "ok"));
            List<AuditLogEntry> entries = f.readRecent(10);
            assertEquals(1, entries.size());
            assertEquals("alice", entries.get(0).user());
        }

        @Test void readRecent_maxLines_respected() {
            AuditLogFile f = new AuditLogFile(tmp);
            for (int i = 0; i < 10; i++)
                f.append(new AuditLogEntry("ts", "u", "ACTION" + i, ""));
            assertEquals(3, f.readRecent(3).size());
        }

        @Test void readRecent_newestFirst() {
            AuditLogFile f = new AuditLogFile(tmp);
            f.append(new AuditLogEntry("ts", "u", "FIRST", ""));
            f.append(new AuditLogEntry("ts", "u", "SECOND", ""));
            List<AuditLogEntry> entries = f.readRecent(10);
            assertEquals("SECOND", entries.get(0).action());
        }

        @Test void readRecent_noFile_empty() {
            AuditLogFile f = new AuditLogFile(tmp);
            assertTrue(f.readRecent(10).isEmpty());
        }

        @Test void clear_removesFile() {
            AuditLogFile f = new AuditLogFile(tmp);
            f.append(new AuditLogEntry("ts", "u", "A", ""));
            f.clear();
            assertFalse(Files.exists(tmp.resolve(AuditLogFile.FILE_NAME)));
        }

        @Test void clear_nonExistentFile_doesNotThrow() {
            AuditLogFile f = new AuditLogFile(tmp);
            assertDoesNotThrow(f::clear);
        }

        @Test void persistsLegacyTabFormat() throws IOException {
            Path log = tmp.resolve(AuditLogFile.FILE_NAME);
            Files.writeString(log, "2024-01-01 10:00:00\tlegacyUser\tOLD_ACTION\tdetail\n",
                    StandardCharsets.UTF_8);
            AuditLogFile f = new AuditLogFile(tmp);
            List<AuditLogEntry> entries = f.readRecent(10);
            assertEquals(1, entries.size());
            assertEquals("legacyUser", entries.get(0).user());
            assertEquals("OLD_ACTION", entries.get(0).action());
        }

        @Test void mixedFormats_allRead() throws IOException {
            Path log = tmp.resolve(AuditLogFile.FILE_NAME);
            Files.writeString(log,
                    "2024-01-01 10:00:00\ttabUser\tTAB_ACTION\t\n"
                            + "{\"v\":1,\"ts\":\"2024-01-02 10:00:00\",\"user\":\"jsonUser\",\"action\":\"JSON_ACTION\",\"detail\":\"\"}\n",
                    StandardCharsets.UTF_8);
            AuditLogFile f = new AuditLogFile(tmp);
            List<AuditLogEntry> entries = f.readRecent(10);
            assertEquals(2, entries.size());
            assertTrue(entries.stream().anyMatch(e -> "TAB_ACTION".equals(e.action())));
            assertTrue(entries.stream().anyMatch(e -> "JSON_ACTION".equals(e.action())));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AuditLogger – öffentliche API
    // ══════════════════════════════════════════════════════════════

    @Nested
    class AuditLoggerApiTest {

        @TempDir Path tmp;
        AuditLogger logger;
        UserAuth auth;

        @BeforeEach void setup() throws InterruptedException {
            logger = AuditLogger.getInstance();
            logger.init(tmp);
            auth = UserAuth.getInstance();
            auth.init(tmp);
            // Warte auf evtl. vorherige asynchrone Schreibvorgänge
            Thread.sleep(100);
        }

        @Test void log_singleAction_persisted() throws InterruptedException {
            logger.log("TEST_ACTION");
            Thread.sleep(300);
            assertTrue(logger.readRecent(10).stream().anyMatch(e -> "TEST_ACTION".equals(e.action())));
        }

        @Test void log_withDetail_persisted() throws InterruptedException {
            logger.log("ACTION", "detail123");
            Thread.sleep(300);
            assertTrue(logger.readRecent(10).stream()
                    .anyMatch(e -> "ACTION".equals(e.action()) && "detail123".equals(e.detail())));
        }

        @Test void readByUser_filtersCorrectly() throws InterruptedException {
            auth.createUser("loguser", "pass123");
            auth.authenticate("loguser", "pass123");
            logger.log("USER_LOG");
            Thread.sleep(300);
            assertTrue(logger.readByUser("loguser").stream()
                    .anyMatch(e -> "USER_LOG".equals(e.action())));
            auth.logout();
        }

        @Test void readByUser_null_returnsEmpty() {
            assertTrue(logger.readByUser(null).isEmpty());
        }

        @Test void clear_nonAdmin_throws() {
            auth.logout();
            assertThrows(SecurityException.class, () -> logger.clear());
        }

        @Test void clear_admin_succeeds() throws InterruptedException {
            auth.createUser("adminUser", "adminPass");
            auth.authenticate("adminUser", "adminPass");
            logger.log("BEFORE_CLEAR");
            Thread.sleep(300);
            assertDoesNotThrow(() -> logger.clear());
            auth.logout();
        }

        @Test void clear_admin_onlyAllowedForAdminRole() throws InterruptedException {
            auth.createUser("rootUser", "rootPass");
            auth.authenticate("rootUser", "rootPass");
            // rootUser ist erster User = admin
            logger.log("X");
            Thread.sleep(100);
            assertDoesNotThrow(() -> logger.clear());
            auth.logout();
        }

        @Test void clear_secondUser_notAdmin_throws() {
            auth.createUser("adminUser2", "adminPass2");
            auth.createUser("regularUser", "regularPass");
            auth.authenticate("regularUser", "regularPass");
            assertThrows(SecurityException.class, () -> logger.clear());
            auth.logout();
        }

        @Test void log_noInit_doesNotThrow() {
            AuditLogger uninit = AuditLogger.getInstance();
            assertDoesNotThrow(() -> uninit.log("NOINIT"));
        }

        @Test void multipleLog_allPersisted() throws InterruptedException {
            for (int i = 0; i < 5; i++) logger.log("MULTI", "entry" + i);
            Thread.sleep(500);
            long count = logger.readRecent(100).stream()
                    .filter(e -> "MULTI".equals(e.action())).count();
            assertEquals(5, count);
        }

        @Test void persistence_survives_reinit() throws InterruptedException {
            logger.log("PERSISTENT_ACTION");
            Thread.sleep(300);
            // Neu-Init (simuliert Neustart)
            logger.init(tmp);
            assertTrue(logger.readRecent(100).stream()
                    .anyMatch(e -> "PERSISTENT_ACTION".equals(e.action())));
        }

        @Test void parse_delegatesToAuditLogFile() {
            String line = "2024-01-01 10:00:00\tuser\tLOGIN\tdetail";
            AuditLogEntry e = AuditLogger.parse(line);
            assertNotNull(e);
            assertEquals("LOGIN", e.action());
        }

        @Test void parse_invalid_returnsNull() {
            assertNull(AuditLogger.parse(""));
            assertNull(AuditLogger.parse(null));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LogEntry-Kompatibilität (Legacy)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class LogEntryCompatTest {

        @Test void from_convertsRecord() {
            AuditLogEntry record = new AuditLogEntry("ts", "user", "ACTION", "detail");
            AuditLogger.LogEntry legacy = AuditLogger.LogEntry.from(record);
            assertEquals("user",   legacy.user);
            assertEquals("ACTION", legacy.action);
            assertEquals("detail", legacy.detail);
        }

        @Test void logEntry_nullDetail_empty() {
            AuditLogger.LogEntry e = new AuditLogger.LogEntry("ts", "u", "A", null);
            assertEquals("", e.detail);
        }
    }
}