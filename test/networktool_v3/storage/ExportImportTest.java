package main.java.networktool_v3.storage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class ExportImportTest {

    @TempDir Path tmp;

    @Nested
    class DataExporterTest {

        @Test void exportCsv_createsFile() throws IOException {
            Path f = DataExporter.exportCsv(tmp);
            assertTrue(Files.exists(f));
        }

        @Test void exportCsv_hasHeader() throws IOException {
            Path f = DataExporter.exportCsv(tmp);
            assertTrue(Files.readString(f).startsWith("IP;"));
        }

        @Test void exportJson_validArray() throws IOException {
            Path f = DataExporter.exportJson(tmp);
            assertTrue(Files.readString(f).trim().startsWith("["));
        }

        @Test void exportHtml_hasDoctype() throws IOException {
            Path f = DataExporter.exportHtml(tmp);
            assertTrue(Files.readString(f).contains("<!DOCTYPE html>"));
        }

        @Test void exportBackup_isZip() throws IOException {
            Path f = DataExporter.exportBackup(tmp);
            assertTrue(f.toString().endsWith(".zip"));
        }

        @Test void csv_semicolonEscaped() {
            String result = DataExporter.csv("a;b");
            assertFalse(result.contains(";"));
        }

        @Test void csv_null_returnsEmpty() {
            assertEquals("", DataExporter.csv(null));
        }

        @Test void csv_withComma_quoted() {
            String result = DataExporter.csv("a,b");
            assertTrue(result.startsWith("\""));
        }

        @Test void esc_quotesEscaped() {
            assertTrue(DataExporter.esc("say \"hi\"").contains("\\\""));
        }

        @Test void esc_null_returnsEmpty() {
            assertEquals("", DataExporter.esc(null));
        }

        @Test void esc_newlineEscaped() {
            assertTrue(DataExporter.esc("a\nb").contains("\\n"));
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
            Path f = tmp.resolve("empty.json");
            Files.writeString(f, "[]");
            assertEquals(0, DataImporter.importJson(f));
        }

        @Test void restoreBackup_zipSlipBlocked() throws IOException {
            Path zip = tmp.resolve("evil.zip");
            try (var zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(zip.toFile()))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("../../evil.txt"));
                zos.write("bad".getBytes());
                zos.closeEntry();
            }
            assertDoesNotThrow(() -> DataImporter.restoreBackup(zip));
        }
    }
}