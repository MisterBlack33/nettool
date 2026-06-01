package main.java.networktool_v3.storage;

import main.java.networktool.storage.DataExporter;
import main.java.networktool.storage.DataImporter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExportImportTest {

    @TempDir Path tmp;

    @Nested
    class DataExporterTest {

        @Test void exportCsv_createsFile() throws IOException    { assertTrue(Files.exists(DataExporter.exportCsv(tmp))); }
        @Test void exportCsv_hasHeader() throws IOException       { assertTrue(Files.readString(DataExporter.exportCsv(tmp)).startsWith("IP;")); }
        @Test void exportJson_validArray() throws IOException     { assertTrue(Files.readString(DataExporter.exportJson(tmp)).trim().startsWith("[")); }
        @Test void exportHtml_hasDoctype() throws IOException     { assertTrue(Files.readString(DataExporter.exportHtml(tmp)).contains("<!DOCTYPE html>")); }
        @Test void csv_semicolonEscaped()                        { assertFalse(DataExporter.csv("a;b").contains(";")); }
        @Test void csv_null_returnsEmpty()                       { assertEquals("", DataExporter.csv(null)); }
        @Test void csv_withComma_quoted()                        { assertTrue(DataExporter.csv("a,b").startsWith("\"")); }
        @Test void esc_quotesEscaped()                           { assertTrue(DataExporter.esc("say \"hi\"").contains("\\\"")); }
        @Test void esc_null_returnsEmpty()                       { assertEquals("", DataExporter.esc(null)); }
        @Test void esc_newlineEscaped()                          { assertTrue(DataExporter.esc("a\nb").contains("\\n")); }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void exportBackup_isZip() throws IOException {
            Path src = tmp.resolve("src"); Files.createDirectories(src);
            Path f = DataExporter.exportBackup(tmp, src);
            assertTrue(f.toString().endsWith(".zip") && Files.exists(f));
        }
    }

    @Nested
    class DataImporterTest {

        @Test void importCsv_headerOnly_zero() throws IOException {
            Path f = tmp.resolve("h.csv");
            Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n");
            assertEquals(0, DataImporter.importCsv(f));
        }

        @Test void importCsv_blankLines_ignored() throws IOException {
            Path f = tmp.resolve("blank.csv");
            Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n\n   \n");
            assertEquals(0, DataImporter.importCsv(f));
        }

        @Test void importJson_emptyArray_zero() throws IOException {
            Path f = tmp.resolve("empty.json"); Files.writeString(f, "[]");
            assertEquals(0, DataImporter.importJson(f));
        }

        @Test void restoreBackup_zipSlipBlocked() throws IOException {
            Path zip = tmp.resolve("evil.zip");
            try (var zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zip.toFile()))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("../../evil.txt"));
                zos.write("bad".getBytes()); zos.closeEntry();
            }
            assertDoesNotThrow(() -> DataImporter.restoreBackup(zip));
        }
    }
}