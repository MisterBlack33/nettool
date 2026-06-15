package networktool.misc;

import main.java.networktool.gui.GuiTheme;
import main.java.networktool.gui.security.NoteEncryption;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class MiscTest {

    // ══════════════════════════════════════════════════════════════
    //  GuiTheme
    // ══════════════════════════════════════════════════════════════

    @Nested
    class GuiThemeTest {

        @BeforeEach void resetToDark()  { if (!GuiTheme.isDark()) GuiTheme.toggleTheme(); }
        @AfterEach  void resetAfter()   { if (!GuiTheme.isDark()) GuiTheme.toggleTheme(); }

        @Test void isDark_initiallyTrue()               { assertTrue(GuiTheme.isDark()); }
        @Test void toggleTheme_switchesToLight()         { GuiTheme.toggleTheme(); assertFalse(GuiTheme.isDark()); }
        @Test void toggleTheme_twice_backToDark()        { GuiTheme.toggleTheme(); GuiTheme.toggleTheme(); assertTrue(GuiTheme.isDark()); }
        @Test void applyToStatics_updatesColors()        { GuiTheme.applyToStatics(); assertNotNull(GuiTheme.BG); assertNotNull(GuiTheme.FG); }
        @Test void osColor_windows()                     { assertEquals(GuiTheme.WIN_COL, GuiTheme.osColor("Windows")); }
        @Test void osColor_linux()                       { assertEquals(GuiTheme.LIN_COL, GuiTheme.osColor("Linux/Unix")); }
        @Test void osColor_android()                     { assertEquals(GuiTheme.AND_COL, GuiTheme.osColor("Android (Samsung)")); }
        @Test void osColor_raspberry()                   { assertEquals(GuiTheme.RPI_COL, GuiTheme.osColor("Raspberry Pi (Linux)")); }
        @Test void osColor_null_returnsFg()              { assertEquals(GuiTheme.FG, GuiTheme.osColor(null)); }
        @Test void osColor_unknown_returnsFgDim()         { assertEquals(GuiTheme.FG_DIM, GuiTheme.osColor("Unbekannt")); }
        @Test void osColor_router()                      { assertEquals(GuiTheme.NET_COL, GuiTheme.osColor("Router / Switch")); }
        @Test void osColor_drucker()                     { assertEquals(GuiTheme.PRN_COL, GuiTheme.osColor("Drucker (IPP/CUPS)")); }
        @Test void osColor_iot()                         { assertEquals(GuiTheme.IOT_COL, GuiTheme.osColor("IoT-Gerät (MQTT)")); }
        @Test void osColor_macos()                       { assertEquals(GuiTheme.APL_COL, GuiTheme.osColor("macOS")); }

        @Test void brighter_returnsLighterColor() {
            java.awt.Color base = new java.awt.Color(100, 100, 100);
            assertTrue(GuiTheme.brighter(base, 1.5f).getRed() >= base.getRed());
        }

        @Test void dynamicColors_darkMode()  { if (!GuiTheme.isDark()) GuiTheme.toggleTheme(); assertNotNull(GuiTheme.bg()); assertNotNull(GuiTheme.fg()); }
        @Test void dynamicColors_lightMode() { if (GuiTheme.isDark()) GuiTheme.toggleTheme(); assertNotNull(GuiTheme.bg()); assertNotNull(GuiTheme.fg()); }
        @Test void themeName_dark()          { if (!GuiTheme.isDark()) GuiTheme.toggleTheme(); assertTrue(GuiTheme.themeName().contains("Hell") || GuiTheme.themeName().contains("☀")); }
        @Test void fonts_notNull()           { assertNotNull(GuiTheme.MONO); assertNotNull(GuiTheme.MONO_S); assertNotNull(GuiTheme.BTN_F); }
    }

    // ══════════════════════════════════════════════════════════════
    //  NoteEncryption
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NoteEncryptionTest {

        @AfterEach void clearSession() { NoteEncryption.clearSession(); }

        @Test void noSession_encryptReturnsPlaintext()  { NoteEncryption.clearSession(); assertEquals("hello", NoteEncryption.encrypt("hello")); }
        @Test void setPassword_enablesSession() throws Exception { NoteEncryption.setPassword("test-pw-123"); assertTrue(NoteEncryption.hasSessionKey()); }

        @Test void encrypt_producesPrefix() throws Exception {
            NoteEncryption.setPassword("test-pw-123");
            assertTrue(NoteEncryption.encrypt("secret").startsWith(NoteEncryption.PREFIX));
        }

        @Test void encrypt_decrypt_roundtrip() throws Exception {
            String pw = "my-secure-pw-456";
            NoteEncryption.setPassword(pw);
            String plain = "super secret note äöü";
            assertEquals(plain, NoteEncryption.decrypt(NoteEncryption.encrypt(plain), pw));
        }

        @Test void decrypt_wrongPassword_returnsError() throws Exception {
            NoteEncryption.setPassword("correct-pw-789");
            assertEquals("[Falsches Passwort]", NoteEncryption.decrypt(NoteEncryption.encrypt("secret"), "wrong-pw-000"));
        }

        @Test void decrypt_notEncrypted_returnsInput()  { assertEquals("plain text", NoteEncryption.decrypt("plain text", "any-pw")); }
        @Test void isEncrypted_false_forPlain()          { assertFalse(NoteEncryption.isEncrypted("plain text")); }
        @Test void isEncrypted_null_false()              { assertFalse(NoteEncryption.isEncrypted(null)); }
        @Test void encrypt_nullInput_returnsNull() throws Exception { NoteEncryption.setPassword("pw123456"); assertNull(NoteEncryption.encrypt(null)); }
        @Test void clearSession_disablesKey() throws Exception { NoteEncryption.setPassword("pw123456"); NoteEncryption.clearSession(); assertFalse(NoteEncryption.hasSessionKey()); }
    }

    // ══════════════════════════════════════════════════════════════
    //  StorageUtils
    // ══════════════════════════════════════════════════════════════

    @Nested
    class StorageUtilsTest {

        @Test void resolveDataDir_notNull()              { assertNotNull(StorageUtils.resolveDataDir()); }
        @Test void extractJsonStr_delegatesToJsonHelper() { assertEquals("value", StorageUtils.extractJsonStr("{\"key\":\"value\"}", "key")); }
        @Test void escapeJson_escapesQuotes()             { assertTrue(StorageUtils.escapeJson("say \"hi\"").contains("\\\"")); }
        @Test void escapeJson_null_returnsEmpty()         { assertEquals("", StorageUtils.escapeJson(null)); }
    }

    // ══════════════════════════════════════════════════════════════
    //  DataExportImport
    // ══════════════════════════════════════════════════════════════

    @Nested
    class DataExportImportTest {

        @TempDir Path tmp;

        @Test void exportCsv_createsFile() throws IOException          { assertTrue(Files.exists(DataExportImport.exportCsv(tmp))); }
        @Test void exportCsv_hasHeader() throws IOException             { assertTrue(Files.readString(DataExportImport.exportCsv(tmp)).startsWith("IP;")); }
        @Test void exportJson_createsFile() throws IOException          { assertTrue(Files.readString(DataExportImport.exportJson(tmp)).trim().startsWith("[")); }
        @Test void exportHtml_createsFile() throws IOException          { assertTrue(Files.readString(DataExportImport.exportHtml(tmp)).contains("<!DOCTYPE html>")); }
        @Test void exportHtml_containsTableTag() throws IOException     { assertTrue(Files.readString(DataExportImport.exportHtml(tmp)).contains("<table>")); }
        @Test void importCsv_emptyFile_returnsZero() throws IOException {
            Path csv = tmp.resolve("empty.csv");
            Files.writeString(csv, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n");
            assertEquals(0, DataExportImport.importCsv(csv));
        }
        @Test void importJson_emptyArray_returnsZero() throws IOException {
            Path json = tmp.resolve("empty.json");
            Files.writeString(json, "[]");
            assertEquals(0, DataExportImport.importJson(json));
        }

        @Test void exportBackup_createsZip() throws IOException {
            Path src = tmp.resolve("src"); Files.createDirectories(src);
            assertTrue(DataExporter.exportBackup(tmp, src).toString().endsWith(".zip"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanProfileStore  (isolated to __junit__ prefix)
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanProfileStoreTest {

        ScanProfileStore store = ScanProfileStore.getInstance();
        final String N = TestConstants.PROFILE_STANDARD;  // "__junit__profile"

        @AfterEach void cleanup() { store.delete(N); }

        @Test void saveAndGet_roundtrip() {
            ScanProfile p = new ScanProfile(N);
            p.osFilter = "Linux";
            p.cidrs.add("10.0.0.0/24");
            store.save(p);
            assertTrue(store.get(N).isPresent());
            assertEquals("Linux", store.get(N).get().osFilter);
        }

        @Test void delete_removesProfile()     { store.save(new ScanProfile(N)); store.delete(N); assertFalse(store.get(N).isPresent()); }
        @Test void getAll_containsSaved()      { store.save(new ScanProfile(N)); assertTrue(store.getAll().stream().anyMatch(p -> N.equals(p.name))); }

        @Test void updateLastRun_persists() {
            store.save(new ScanProfile(N));
            store.updateLastRun(N, "2024-06-01 12:00:00");
            assertEquals("2024-06-01 12:00:00", store.get(N).get().lastRun);
        }

        @Test void save_overwrites_existing() {
            ScanProfile p1 = new ScanProfile(N); p1.osFilter = "Linux"; store.save(p1);
            ScanProfile p2 = new ScanProfile(N); p2.osFilter = "Windows"; store.save(p2);
            assertEquals("Windows", store.get(N).get().osFilter);
            assertEquals(1, store.getAll().stream().filter(p -> N.equals(p.name)).count());
        }

        @Test void get_missing_returnsEmpty() { assertFalse(store.get(TestConstants.TEST_PREFIX + "nonexistent_xyz").isPresent()); }
    }
}