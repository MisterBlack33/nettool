package main.java.networktool.storage.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportFilesTest {

    @TempDir Path tmp;

    @Test void writeUnique_string_writesUtf8() throws IOException {
        Path f = ExportFiles.writeUnique(tmp, "txt", "äö");
        assertEquals("äö", Files.readString(f));
        assertTrue(f.getFileName().toString().endsWith(".txt"));
    }

    @Test void writeUnique_manyCalls_allDistinct() throws IOException {
        Path a = ExportFiles.writeUnique(tmp, "bin", new byte[]{1});
        Path b = ExportFiles.writeUnique(tmp, "bin", new byte[]{2});
        Path c = ExportFiles.writeUnique(tmp, "bin", new byte[]{3});
        assertEquals(3, java.util.Set.of(a, b, c).size());
    }

    @Test void writeUnique_createsMissingDirectory() throws IOException {
        Path f = ExportFiles.writeUnique(tmp.resolve("a/b"), "txt", "x");
        assertTrue(Files.exists(f));
    }

    @Test void defaultDir_endsWithExportFolder() {
        assertEquals("NetTool-Export", ExportFiles.defaultDir().getFileName().toString());
    }
}
