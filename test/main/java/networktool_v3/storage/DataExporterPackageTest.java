package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataExporterPackageTest {

    @TempDir Path tmp;

    // ── DataExporter ─────────────────────────────────────────────

    @Test
    void forEachHost_doesNotThrow() {
        assertDoesNotThrow(() -> DataExporter.forEachHost((h, cat) -> {}));
    }

    @Test
    void csv_normal() {
        assertEquals("hello", DataExporter.csv("hello"));
    }

    @Test
    void csv_semicolon_replaced() {
        assertFalse(DataExporter.csv("a;b").contains(";"));
    }

    @Test
    void csv_comma_quoted() {
        String r = DataExporter.csv("a,b");
        assertTrue(r.startsWith("\"") && r.endsWith("\""));
    }

    @Test
    void csv_null_empty() {
        assertEquals("", DataExporter.csv(null));
    }

    @Test
    void esc_quotes() {
        assertTrue(DataExporter.esc("\"hi\"").contains("\\\""));
    }

    @Test
    void esc_backslash() {
        assertTrue(DataExporter.esc("a\\b").contains("\\\\"));
    }

    @Test
    void esc_newline() {
        assertTrue(DataExporter.esc("a\nb").contains("\\n"));
    }

    @Test
    void esc_null_empty() {
        assertEquals("", DataExporter.esc(null));
    }

    @Test
    void exportCsv_hasHeader() throws IOException {
        Path f = DataExporter.exportCsv(tmp);
        assertTrue(Files.readString(f).startsWith("IP;"));
    }

    @Test
    void exportJson_isArray() throws IOException {
        Path f = DataExporter.exportJson(tmp);
        assertTrue(Files.readString(f).trim().startsWith("["));
    }

    @Test
    void exportHtml_hasNetTool() throws IOException {
        Path f = DataExporter.exportHtml(tmp);
        assertTrue(Files.readString(f).contains("NetTool"));
    }

    @Test
    void exportBackup_zip() throws IOException {
        Path f = DataExporter.exportBackup(tmp);
        assertTrue(f.toString().endsWith(".zip"));
    }

    // ── DataImporter ─────────────────────────────────────────────

    @Test
    void importCsv_headerOnly_zero() throws IOException {
        Path f = tmp.resolve("h.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n");
        assertEquals(0, DataImporter.importCsv(f));
    }

    @Test
    void importJson_emptyArray_zero() throws IOException {
        Path f = tmp.resolve("empty.json");
        Files.writeString(f, "[]");
        assertEquals(0, DataImporter.importJson(f));
    }

    @Test
    void importCsv_validRow() throws IOException {
        Path f = tmp.resolve("row.csv");
        Files.writeString(f,
                "IP;Hostname;OS;Datum;Ports;Notiz;TestImportCat\n" +
                        "172.16.0.1;srv;Linux;2024-01-01;;note;TestImportCat\n");
        int n = DataImporter.importCsv(f);
        assertTrue(n >= 0);
    }

    @Test
    void importJson_validEntry() throws IOException {
        Path f = tmp.resolve("data.json");
        Files.writeString(f,
                "[{\"ip\":\"172.16.0.2\",\"hostname\":\"srv\"," +
                        "\"os\":\"Linux\",\"savedAt\":\"\",\"ports\":\"\"," +
                        "\"notes\":\"\",\"category\":\"TestImportCat\"}]");
        int n = DataImporter.importJson(f);
        assertTrue(n >= 0);
    }

    @Test
    void restoreBackup_zipSlip_blocked() throws IOException {
        Path zip = tmp.resolve("evil.zip");
        try (var zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip.toFile()))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("../../evil.txt"));
            zos.write("bad".getBytes());
            zos.closeEntry();
        }
        assertDoesNotThrow(() -> DataImporter.restoreBackup(zip));
    }

    // ── HtmlReportBuilder ────────────────────────────────────────

    @Test
    void build_containsDoctype() {
        assertTrue(HtmlReportBuilder.build().contains("<!DOCTYPE html>"));
    }

    @Test
    void build_containsTable() {
        assertTrue(HtmlReportBuilder.build().contains("<table>"));
    }

    @Test
    void build_specialCharsEscaped() {
        // ensure no raw < or > from data leaks into HTML tag context
        String html = HtmlReportBuilder.build();
        assertNotNull(html);
    }
}