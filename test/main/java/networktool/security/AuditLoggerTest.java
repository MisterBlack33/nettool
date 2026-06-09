package main.java.networktool.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLoggerTest {

    // ── AuditLogEntry ─────────────────────────────────────────────────────

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
            assertEquals("",       e.timestamp());
            assertEquals("system", e.user());
            assertEquals("",       e.action());
            assertEquals("",       e.detail());
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
            assertTrue(new AuditLogEntry("ts","u","A","say \"hi\"").toNdjson().contains("\\\"hi\\\""));
        }

        @Test void toNdjson_escapesNewline() {
            assertTrue(new AuditLogEntry("ts","u","A","line1\nline2").toNdjson().contains("\\n"));
        }

        @Test void toNdjson_escapesBackslash() {
            assertTrue(new AuditLogEntry("ts","u","A","a\\b").toNdjson().contains("\\\\"));
        }
    }

    // ── AuditLogFile parse ────────────────────────────────────────────────

    @Nested
    class AuditLogFileParseTest {

        @Test void parse_ndjsonV1() {
            String line = "{\"v\":1,\"ts\":\"2024-01-01 10:00:00\",\"user\":\"alice\",\"action\":\"LOGIN\",\"detail\":\"ok\"}";
            AuditLogEntry e = AuditLogFile.parse(line);
            assertNotNull(e);
            assertEquals("alice", e.user());
            assertEquals("LOGIN", e.action());
            assertEquals("ok",    e.detail());
        }

        @Test void parse_tabFormat() {
            AuditLogEntry e = AuditLogFile.parse("2024-01-01 10:00:00\tbob\tSCAN\tcidr");
            assertNotNull(e);
            assertEquals("bob",  e.user());
            assertEquals("SCAN", e.action());
            assertEquals("cidr", e.detail());
        }

        @Test void parse_tabFormat_noDetail() {
            AuditLogEntry e = AuditLogFile.parse("2024-01-01\tuser1\tLOGIN");
            assertNotNull(e);
            assertEquals("LOGIN", e.action());
            assertEquals("",      e.detail());
        }

        @Test void parse_legacyJson() {
            AuditLogEntry e = AuditLogFile.parse(
                    "{\"timestamp\":\"2024-01-01\",\"user\":\"u\",\"action\":\"A\",\"detail\":\"d\"}");
            assertNotNull(e);
            assertEquals("A", e.action());
            assertEquals("d", e.detail());
        }

        @Test void parse_blank_null()       { assertNull(AuditLogFile.parse("")); }
        @Test void parse_null_null()        { assertNull(AuditLogFile.parse(null)); }
        @Test void parse_tooShortTab_null() { assertNull(AuditLogFile.parse("only one")); }
        @Test void parse_emptyJson_null()   { assertNull(AuditLogFile.parse("{}")); }

        @Test void parse_ndjson_withEscapes() {
            AuditLogEntry e = AuditLogFile.parse(
                    "{\"v\":1,\"ts\":\"ts\",\"user\":\"u\",\"action\":\"A\",\"detail\":\"say \\\"hi\\\"\"}");
            assertNotNull(e);
            assertTrue(e.detail().contains("\"hi\""));
        }
    }

    // ── AuditLogFile I/O ──────────────────────────────────────────────────

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
            f.append(new AuditLogEntry("ts", "u", "FIRST",  ""));
            f.append(new AuditLogEntry("ts", "u", "SECOND", ""));
            assertEquals("SECOND", f.readRecent(10).get(0).action());
        }

        @Test void readRecent_noFile_empty() {
            assertTrue(new AuditLogFile(tmp).readRecent(10).isEmpty());
        }

        @Test void clear_removesFile() {
            AuditLogFile f = new AuditLogFile(tmp);
            f.append(new AuditLogEntry("ts", "u", "A", ""));
            f.clear();
            assertFalse(Files.exists(tmp.resolve(AuditLogFile.FILE_NAME)));
        }

        @Test void clear_nonExistentFile_doesNotThrow() {
            assertDoesNotThrow(() -> new AuditLogFile(tmp).clear());
        }

        @Test void persistsLegacyTabFormat() throws IOException {
            Path log = tmp.resolve(AuditLogFile.FILE_NAME);
            Files.writeString(log, "2024-01-01 10:00:00\tlegacyUser\tOLD_ACTION\tdetail\n",
                    StandardCharsets.UTF_8);
            List<AuditLogEntry> entries = new AuditLogFile(tmp).readRecent(10);
            assertEquals(1, entries.size());
            assertEquals("legacyUser", entries.get(0).user());
            assertEquals("OLD_ACTION", entries.get(0).action());
        }

        @Test void mixedFormats_allRead() throws IOException {
            Path log = tmp.resolve(AuditLogFile.FILE_NAME);
            Files.writeString(log,
                    "2024-01-01 10:00:00\ttabUser\tTAB_ACTION\t\n"
                            + "{\"v\":1,\"ts\":\"2024-01-02 10:00:00\",\"user\":\"jsonUser\","
                            + "\"action\":\"JSON_ACTION\",\"detail\":\"\"}\n",
                    StandardCharsets.UTF_8);
            List<AuditLogEntry> entries = new AuditLogFile(tmp).readRecent(10);
            assertEquals(2, entries.size());
            assertTrue(entries.stream().anyMatch(e -> "TAB_ACTION".equals(e.action())));
            assertTrue(entries.stream().anyMatch(e -> "JSON_ACTION".equals(e.action())));
        }
    }

    // ── AuditLogger API ───────────────────────────────────────────────────

    @Nested
    class AuditLoggerApiTest {

        @TempDir Path tmp;
        AuditLogger logger;
        UserAuth    auth;

        @BeforeEach void setup() {
            logger = AuditLogger.getInstance();
            logger.init(tmp);
            auth = UserAuth.getInstance();
            auth.init(tmp);
        }

        @AfterEach void teardown() {
            auth.logout();
            logger.shutdown(); // schließt Writer-Thread → TempDir kann gelöscht werden
        }

        @Test void log_singleAction_persisted() {
            logger.log("TEST_ACTION");
            assertTrue(logger.readRecent(10).stream()
                    .anyMatch(e -> "TEST_ACTION".equals(e.action())));
        }

        @Test void log_withDetail_persisted() {
            logger.log("ACTION", "detail123");
            assertTrue(logger.readRecent(10).stream()
                    .anyMatch(e -> "ACTION".equals(e.action()) && "detail123".equals(e.detail())));
        }

        @Test void readByUser_filtersCorrectly() {
            auth.createUser("loguser", "pass123");
            auth.authenticate("loguser", "pass123");
            logger.log("USER_LOG");
            assertTrue(logger.readByUser("loguser").stream()
                    .anyMatch(e -> "USER_LOG".equals(e.action())));
        }

        @Test void readByUser_null_returnsEmpty() {
            assertTrue(logger.readByUser(null).isEmpty());
        }

        @Test void clear_nonAdmin_throws() {
            auth.logout();
            assertThrows(SecurityException.class, () -> logger.clear());
        }

        @Test void clear_admin_succeeds() {
            auth.createUser("adminUser", "adminPass");
            auth.authenticate("adminUser", "adminPass");
            logger.log("BEFORE_CLEAR");
            assertDoesNotThrow(() -> logger.clear());
        }

        @Test void clear_secondUser_notAdmin_throws() {
            auth.createUser("admin2", "adminPass2");
            auth.createUser("regular", "regularPass");
            auth.authenticate("regular", "regularPass");
            assertThrows(SecurityException.class, () -> logger.clear());
        }

        @Test void log_noInit_doesNotThrow() {
            assertDoesNotThrow(() -> AuditLogger.getInstance().log("NOINIT"));
        }

        @Test void multipleLog_allPersisted() {
            for (int i = 0; i < 5; i++) logger.log("MULTI", "entry" + i);
            assertEquals(5, logger.readRecent(100).stream()
                    .filter(e -> "MULTI".equals(e.action())).count());
        }

        @Test void persistence_survives_reinit() {
            logger.log("PERSISTENT_ACTION");
            logger.init(tmp); // flush + reinit
            assertTrue(logger.readRecent(100).stream()
                    .anyMatch(e -> "PERSISTENT_ACTION".equals(e.action())));
        }

        @Test void parse_delegatesToAuditLogFile() {
            AuditLogEntry e = AuditLogger.parse("2024-01-01 10:00:00\tuser\tLOGIN\tdetail");
            assertNotNull(e);
            assertEquals("LOGIN", e.action());
        }

        @Test void parse_invalid_returnsNull() {
            assertNull(AuditLogger.parse(""));
            assertNull(AuditLogger.parse(null));
        }
    }

    // ── LogEntry Legacy ───────────────────────────────────────────────────

    @Nested
    class LogEntryCompatTest {

        @Test void from_convertsRecord() {
            AuditLogger.LogEntry legacy = AuditLogger.LogEntry.from(
                    new AuditLogEntry("ts", "user", "ACTION", "detail"));
            assertEquals("user",   legacy.user);
            assertEquals("ACTION", legacy.action);
            assertEquals("detail", legacy.detail);
        }

        @Test void logEntry_nullDetail_empty() {
            assertEquals("", new AuditLogger.LogEntry("ts", "u", "A", null).detail);
        }
    }
}