package main.java.networktool.storage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Backward-compatible wrapper. Delegates to {@link DataExporter} / {@link DataImporter}.
 */
public final class DataExportImport {

    private DataExportImport() {}

    public static Path exportCsv(Path outDir)      throws IOException { return DataExporter.exportCsv(outDir); }
    public static Path exportJson(Path outDir)     throws IOException { return DataExporter.exportJson(outDir); }
    public static Path exportHtml(Path outDir)     throws IOException { return DataExporter.exportHtml(outDir); }
    public static Path exportBackup(Path outDir)   throws IOException { return DataExporter.exportBackup(outDir); }
    public static int  importCsv(Path file)        throws IOException { return DataImporter.importCsv(file); }
    public static int  importJson(Path file)        throws IOException { return DataImporter.importJson(file); }
    public static int  restoreBackup(Path zipFile)  throws IOException { return DataImporter.restoreBackup(zipFile); }
}