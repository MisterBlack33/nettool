package main.java.networktool.storage.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Schreibt Export-Dateien ohne bestehende zu überschreiben (Zähler-Suffix bei Namenskollision). */
public final class ExportFiles {

    private static final int MAX_ATTEMPTS = 1000;
    private static final String DEFAULT_DIR_NAME = "NetTool-Export";

    private ExportFiles() {}

    public static Path defaultDir() {
        return Path.of(System.getProperty("user.home"), DEFAULT_DIR_NAME);
    }

    public static Path writeUnique(Path dir, String extension, byte[] content) throws IOException {
        Files.createDirectories(dir);
        String base = "export_" + DataExportFormatters.now();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Path file = dir.resolve(base + (i == 0 ? "" : "_" + i) + "." + extension);
            try {
                Files.write(file, content, StandardOpenOption.CREATE_NEW);
                return file;
            } catch (FileAlreadyExistsException e) {
                continue;
            }
        }
        throw new IOException("Kein freier Dateiname in " + dir);
    }

    public static Path writeUnique(Path dir, String extension, String content) throws IOException {
        return writeUnique(dir, extension, content.getBytes(StandardCharsets.UTF_8));
    }
}
