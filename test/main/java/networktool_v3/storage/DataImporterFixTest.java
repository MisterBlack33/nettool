package main.java.networktool_v3.storage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für DataImporter-Fix: eigene extractJsonStr() durch JsonHelper ersetzt. */
class DataImporterFixTest {

    @TempDir Path tmp;

    @Test
    void noOwnExtractJsonStr_method() {
        // Die eigene Kopie soll entfernt sein — JsonHelper übernimmt
        boolean found = false;
        for (Method m : DataImporter.class.getDeclaredMethods()) {
            if (m.getName().equals("extractJsonStr")) { found = true; break; }
        }
        assertFalse(found, "eigene extractJsonStr() sollte entfernt sein");
    }

    @Test
    void importJson_validEntry_returnsOne() throws Exception {
        Path f = tmp.resolve("data.json");
        Files.writeString(f,
                "[{\"ip\":\"172.30.0.1\",\"hostname\":\"srv\"," +
                        "\"os\":\"Linux\",\"savedAt\":\"\",\"ports\":\"\"," +
                        "\"notes\":\"\",\"category\":\"FixTestCat\"}]");
        assertTrue(DataImporter.importJson(f) >= 0);
    }

    @Test
    void importJson_emptyArray_returnsZero() throws Exception {
        Path f = tmp.resolve("empty.json");
        Files.writeString(f, "[]");
        assertEquals(0, DataImporter.importJson(f));
    }

    @Test
    void importCsv_validRow_noException() throws Exception {
        Path f = tmp.resolve("row.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;FixTestCat\n" +
                "172.30.0.2;srv;Linux;2024-01-01;;note;FixTestCat\n");
        assertDoesNotThrow(() -> DataImporter.importCsv(f));
    }

    @Test
    void restoreBackup_zipSlip_blocked() throws Exception {
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