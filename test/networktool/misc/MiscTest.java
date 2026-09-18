package networktool.misc;

import main.java.networktool.gui.security.NoteDecryptionException;
import main.java.networktool.storage.*;
import main.java.networktool.storage.export.DataExportImport;
import main.java.networktool.storage.export.DataExporter;
import main.java.networktool.storage.profile.ScanProfileStore;
import main.java.networktool.theme.GuiTheme;
import main.java.networktool.gui.security.NoteEncryption;
import main.java.networktool.model.ScanProfile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class MiscTest {

    //  NoteEncryption

    @Isolated
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

        @Test void decrypt_wrongPassword_throwsNoteDecryptionException() throws Exception {
            NoteEncryption.setPassword("correct-pw-789");
            String enc = NoteEncryption.encrypt("secret");
            assertThrows(NoteDecryptionException.class, () -> NoteEncryption.decrypt(enc, "wrong-pw-000"));
        }

        @Test void decrypt_notEncrypted_returnsInput()  { assertEquals("plain text", NoteEncryption.decrypt("plain text", "any-pw")); }
        @Test void isEncrypted_false_forPlain()          { assertFalse(NoteEncryption.isEncrypted("plain text")); }
        @Test void isEncrypted_null_false()              { assertFalse(NoteEncryption.isEncrypted(null)); }
        @Test void encrypt_nullInput_returnsNull() throws Exception { NoteEncryption.setPassword("pw123456"); assertNull(NoteEncryption.encrypt(null)); }
        @Test void clearSession_disablesKey() throws Exception { NoteEncryption.setPassword("pw123456"); NoteEncryption.clearSession(); assertFalse(NoteEncryption.hasSessionKey()); }
    }

    //  StorageLocationsResolver

    @Nested
    class StorageLocationsResolverTest {

        @Test void resolveDataDir_notNull()              { assertNotNull(StorageLocationsResolver.resolveDataDir()); }
        @Test void extractJsonStr_delegatesToJsonCodec() { assertEquals("value", StorageLocationsResolver.extractJsonStr("{\"key\":\"value\"}", "key")); }
        @Test void escapeJson_escapesQuotes()             { assertTrue(StorageLocationsResolver.escapeJson("say \"hi\"").contains("\\\"")); }
        @Test void escapeJson_null_returnsEmpty()         { assertEquals("", StorageLocationsResolver.escapeJson(null)); }
    }

    //  DataExportImport

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
    }

    //  ScanProfileStore  (isolated to __junit__ prefix)

    @Isolated
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
