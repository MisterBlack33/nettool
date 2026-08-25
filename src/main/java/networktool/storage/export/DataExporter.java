package main.java.networktool.storage.export;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.backup.DataExportBackup;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Export gespeicherter Hosts in CSV / JSON / HTML / ZIP.
 *
 * Öffentliche Fassade — Implementierung aufgeteilt in:
 *  {@link DataExportFormatters} (CSV/JSON/HTML) und
 *  {@link DataExportBackup} (ZIP / verschlüsseltes Backup).
 * Reiner Struktur-Split, keine Logik-Änderung.
 */
public final class DataExporter {

    private DataExporter() {}

    public static Path exportCsv(Path outDir) throws IOException {
        return DataExportFormatters.exportCsv(outDir);
    }

    public static Path exportJson(Path outDir) throws IOException {
        return DataExportFormatters.exportJson(outDir);
    }

    public static Path exportHtml(Path outDir) throws IOException {
        return DataExportFormatters.exportHtml(outDir);
    }

    /** Backup mit automatisch generiertem Dateinamen. */
    public static Path exportBackup(Path outDir) throws IOException {
        return DataExportBackup.exportBackup(outDir);
    }

    /** Backup mit automatisch generiertem Dateinamen + explizitem Quellverzeichnis. */
    public static Path exportBackup(Path outDir, Path srcDir) throws IOException {
        return DataExportBackup.exportBackup(outDir, srcDir);
    }

    /**
     * Backup mit explizitem Dateinamen (wird von AutoBackup genutzt,
     * damit Test-Backups das TEST_BACKUP_PREFIX tragen können).
     */
    public static Path exportBackup(Path outDir, String filename) throws IOException {
        return DataExportBackup.exportBackup(outDir, filename);
    }

    public static Path exportBackup(Path outDir, Path srcDir, String filename) throws IOException {
        return DataExportBackup.exportBackup(outDir, srcDir, filename);
    }

    /**
     * Erstellt ein AES-256-GCM-verschlüsseltes Backup (additiv zu exportBackup()).
     * Das unverschlüsselte ZIP wird nur temporär angelegt und danach gelöscht.
     */
    public static Path exportEncryptedBackup(Path outDir, String password) throws Exception {
        return DataExportBackup.exportEncryptedBackup(outDir, password);
    }

    // ── Hilfsmethoden (Testzugriff über Package-Sichtbarkeit) ──────────────

    @FunctionalInterface
    interface HostConsumer { void accept(HostResult h, String category); }

    static void forEachHost(HostConsumer consumer) {
        DataExportFormatters.forEachHost(consumer::accept);
    }

    static String csv(String s) { return DataExportFormatters.csv(s); }

    static String esc(String s) { return DataExportFormatters.esc(s); }
}
