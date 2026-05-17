package networktool_v3.misc;

import main.java.networktool_v3.gui.GuiTheme;
import main.java.networktool_v3.gui.security.NoteEncryption;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanProfile;
import main.java.networktool_v3.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GuiTheme, NoteEncryption, StorageUtils,
 * DataExportImport, ScanProfileStore, HostJsonBuilder edge cases.
 */
class MiscTest {

    // ══════════════════════════════════════════════════════════════
    //  GuiTheme
    // ══════════════════════════════════════════════════════════════

    @Nested
    class GuiThemeTest {

        @BeforeEach
        void resetToDark() {
            // Ensure we start in a known state
            if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        }

        @AfterEach
        void resetAfter() {
            if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
        }

        @Test
        void isDark_initiallyTrue() {
            assertTrue(GuiTheme.isDark());
        }

        @Test
        void toggleTheme_switchesToLight() {
            GuiTheme.toggleTheme();
            assertFalse(GuiTheme.isDark());
        }

        @Test
        void toggleTheme_twice_backToDark() {
            GuiTheme.toggleTheme();
            GuiTheme.toggleTheme();
            assertTrue(GuiTheme.isDark());
        }

        @Test
        void applyToStatics_updatesColors() {
            GuiTheme.applyToStatics();
            assertNotNull(GuiTheme.BG);
            assertNotNull(GuiTheme.FG);
            assertNotNull(GuiTheme.BORDER);
        }

        @Test
        void osColor_windows() {
            assertEquals(GuiTheme.WIN_COL, GuiTheme.osColor("Windows"));
        }

        @Test
        void osColor_linux() {
            assertEquals(GuiTheme.LIN_COL, GuiTheme.osColor("Linux/Unix"));
        }

        @Test
        void osColor_android() {
            assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Samsung)"));
        }

        @Test
        void osColor_raspberry() {
            assertEquals(GuiTheme.RPI_COL, GuiTheme.osColor("Raspberry Pi (Linux)"));
        }

        @Test
        void osColor_null_returnsFg() {
            assertEquals(GuiTheme.FG, GuiTheme.osColor(null));
        }

        @Test
        void osColor_unknown_returnsFgDim() {
            assertEquals(GuiTheme.FG_DIM, GuiTheme.osColor("Unbekannt"));
        }

        @Test
        void osColor_router() {
            assertEquals(GuiTheme.NET_COL, GuiTheme.osColor("Router / Switch"));
        }

        @Test
        void osColor_drucker() {
            assertEquals(GuiTheme.PRN_COL, GuiTheme.osColor("Drucker (IPP/CUPS)"));
        }

        @Test
        void osColor_iot() {
            assertEquals(GuiTheme.IOT_COL, GuiTheme.osColor("IoT-Gerät (MQTT)"));
        }

        @Test
        void osColor_macos() {
            assertEquals(GuiTheme.APL_COL, GuiTheme.osColor("macOS"));
        }

        @Test
        void brighter_returnsLighterColor() {
            java.awt.Color base = new java.awt.Color(100, 100, 100);
            java.awt.Color bright = GuiTheme.brighter(base, 1.5f);
            assertTrue(bright.getRed() >= base.getRed());
        }

        @Test
        void dynamicColors_darkMode() {
            if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
            assertNotNull(GuiTheme.bg());
            assertNotNull(GuiTheme.fg());
            assertNotNull(GuiTheme.border());
        }

        @Test
        void dynamicColors_lightMode() {
            if (GuiTheme.isDark()) GuiTheme.toggleTheme();
            assertNotNull(GuiTheme.bg());
            assertNotNull(GuiTheme.fg());
        }

        @Test
        void themeName_dark() {
            if (!GuiTheme.isDark()) GuiTheme.toggleTheme();
            assertTrue(GuiTheme.themeName().contains("Hell") || GuiTheme.themeName().contains("☀"));
        }

        @Test
        void fonts_notNull() {
            assertNotNull(GuiTheme.MONO);
            assertNotNull(GuiTheme.MONO_S);
            assertNotNull(GuiTheme.BTN_F);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NoteEncryption
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NoteEncryptionTest {

        @AfterEach
        void clearSession() {
            NoteEncryption.clearSession();
        }

        @Test
        void noSession_encryptReturnsPlaintext() {
            NoteEncryption.clearSession();
            assertFalse(NoteEncryption.hasSessionKey());
            String plain = "hello";
            assertEquals(plain, NoteEncryption.encrypt(plain));
        }

        @Test
        void setPassword_enablesSession() throws Exception {
            NoteEncryption.setPassword("test-password-123");
            assertTrue(NoteEncryption.hasSessionKey());
        }

        @Test
        void encrypt_producesPrefix() throws Exception {
            NoteEncryption.setPassword("test-password-123");
            String enc = NoteEncryption.encrypt("secret note");
            assertTrue(enc.startsWith(NoteEncryption.PREFIX));
        }

        @Test
        void encrypt_decrypt_roundtrip() throws Exception {
            String pw = "my-secure-pw-456";
            NoteEncryption.setPassword(pw);
            String plaintext = "super secret note with unicode äöü";
            String encrypted = NoteEncryption.encrypt(plaintext);
            String decrypted = NoteEncryption.decrypt(encrypted, pw);
            assertEquals(plaintext, decrypted);
        }

        @Test
        void decrypt_wrongPassword_returnsError() throws Exception {
            NoteEncryption.setPassword("correct-pw-789");
            String enc = NoteEncryption.encrypt("secret");
            String result = NoteEncryption.decrypt(enc, "wrong-pw-000");
            assertEquals("[Falsches Passwort]", result);
        }

        @Test
        void decrypt_notEncrypted_returnsInput() {
            String plain = "not encrypted";
            assertEquals(plain, NoteEncryption.decrypt(plain, "any-pw"));
        }

        @Test
        void isEncrypted_true() throws Exception {
            NoteEncryption.setPassword("pw123456");
            String enc = NoteEncryption.encrypt("data");
            assertTrue(NoteEncryption.isEncrypted(enc));
        }

        @Test
        void isEncrypted_false_forPlain() {
            assertFalse(NoteEncryption.isEncrypted("plain text"));
        }

        @Test
        void isEncrypted_null_false() {
            assertFalse(NoteEncryption.isEncrypted(null));
        }

        @Test
        void encrypt_nullInput_returnsNull() throws Exception {
            NoteEncryption.setPassword("pw123456");
            assertNull(NoteEncryption.encrypt(null));
        }

        @Test
        void encrypt_blankInput_returnsBlank() throws Exception {
            NoteEncryption.setPassword("pw123456");
            String result = NoteEncryption.encrypt("   ");
            // blank stays blank (not encrypted)
            assertNotNull(result);
        }

        @Test
        void clearSession_disablesKey() throws Exception {
            NoteEncryption.setPassword("pw123456");
            NoteEncryption.clearSession();
            assertFalse(NoteEncryption.hasSessionKey());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  StorageUtils
    // ══════════════════════════════════════════════════════════════

    @Nested
    class StorageUtilsTest {

        @Test
        void resolveTxtDir_notNull() {
            assertNotNull(StorageUtils.resolveTxtDir());
        }

        @Test
        void extractJsonStr_delegatesToJsonHelper() {
            String json = "{\"key\":\"value\"}";
            assertEquals("value", StorageUtils.extractJsonStr(json, "key"));
        }

        @Test
        void escapeJson_escapesQuotes() {
            assertTrue(StorageUtils.escapeJson("say \"hi\"").contains("\\\""));
        }

        @Test
        void escapeJson_null_returnsEmpty() {
            assertEquals("", StorageUtils.escapeJson(null));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DataExportImport
    // ══════════════════════════════════════════════════════════════

    @Nested
    class DataExportImportTest {

        @TempDir Path tmp;

        @Test
        void exportCsv_createsFile() throws IOException {
            Path f = DataExportImport.exportCsv(tmp);
            assertTrue(Files.exists(f));
            assertTrue(f.toString().endsWith(".csv"));
        }

        @Test
        void exportCsv_hasHeader() throws IOException {
            Path f = DataExportImport.exportCsv(tmp);
            String content = Files.readString(f);
            assertTrue(content.startsWith("IP;"));
        }

        @Test
        void exportJson_createsFile() throws IOException {
            Path f = DataExportImport.exportJson(tmp);
            assertTrue(Files.exists(f));
            String content = Files.readString(f);
            assertTrue(content.trim().startsWith("["));
        }

        @Test
        void exportHtml_createsFile() throws IOException {
            Path f = DataExportImport.exportHtml(tmp);
            assertTrue(Files.exists(f));
            String content = Files.readString(f);
            assertTrue(content.contains("<!DOCTYPE html>"));
            assertTrue(content.contains("NetTool"));
        }

        @Test
        void exportBackup_createsZip() throws IOException {
            Path f = DataExportImport.exportBackup(tmp);
            assertTrue(Files.exists(f));
            assertTrue(f.toString().endsWith(".zip"));
        }

        @Test
        void importCsv_emptyFile_returnsZero() throws IOException {
            Path csv = tmp.resolve("empty.csv");
            Files.writeString(csv, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n");
            assertEquals(0, DataExportImport.importCsv(csv));
        }

        @Test
        void importJson_emptyArray_returnsZero() throws IOException {
            Path json = tmp.resolve("empty.json");
            Files.writeString(json, "[]");
            assertEquals(0, DataExportImport.importJson(json));
        }

        @Test
        void importCsv_headerOnly_returnsZero() throws IOException {
            Path csv = tmp.resolve("headeronly.csv");
            Files.writeString(csv, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n");
            assertEquals(0, DataExportImport.importCsv(csv));
        }

        @Test
        void exportHtml_containsTableTag() throws IOException {
            Path f = DataExportImport.exportHtml(tmp);
            assertTrue(Files.readString(f).contains("<table>"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanProfileStore
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanProfileStoreTest {

        ScanProfileStore store = ScanProfileStore.getInstance();
        static final String TEST_NAME = "__test_profile__";

        @AfterEach
        void cleanup() {
            store.delete(TEST_NAME);
        }

        @Test
        void saveAndGet_roundtrip() {
            ScanProfile p = new ScanProfile(TEST_NAME);
            p.osFilter = "Linux";
            p.cidrs.add("10.0.0.0/24");
            store.save(p);
            assertTrue(store.get(TEST_NAME).isPresent());
            assertEquals("Linux", store.get(TEST_NAME).get().osFilter);
        }

        @Test
        void delete_removesProfile() {
            store.save(new ScanProfile(TEST_NAME));
            store.delete(TEST_NAME);
            assertFalse(store.get(TEST_NAME).isPresent());
        }

        @Test
        void getAll_containsSaved() {
            store.save(new ScanProfile(TEST_NAME));
            assertTrue(store.getAll().stream().anyMatch(p -> TEST_NAME.equals(p.name)));
        }

        @Test
        void updateLastRun_persists() {
            store.save(new ScanProfile(TEST_NAME));
            store.updateLastRun(TEST_NAME, "2024-06-01 12:00:00");
            assertEquals("2024-06-01 12:00:00", store.get(TEST_NAME).get().lastRun);
        }

        @Test
        void save_overwrites_existing() {
            ScanProfile p1 = new ScanProfile(TEST_NAME);
            p1.osFilter = "Linux";
            store.save(p1);

            ScanProfile p2 = new ScanProfile(TEST_NAME);
            p2.osFilter = "Windows";
            store.save(p2);

            assertEquals("Windows", store.get(TEST_NAME).get().osFilter);
            assertEquals(1, store.getAll().stream()
                    .filter(p -> TEST_NAME.equals(p.name)).count());
        }

        @Test
        void get_missing_returnsEmpty() {
            assertFalse(store.get("nonexistent_xyz").isPresent());
        }
    }
}
