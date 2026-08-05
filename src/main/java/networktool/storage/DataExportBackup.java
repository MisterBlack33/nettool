package main.java.networktool.storage;

import main.java.networktool.logging.DebugLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Erstellt ZIP-Backups (unverschlüsselt + AES-256-GCM-verschlüsselt) des
 * Datenverzeichnisses. Ausgelagert aus {@link DataExporter}
 * (nur Split, keine Logik-Änderung). Package-private — öffentliche Fassade
 * bleibt DataExporter.
 */
final class DataExportBackup {

    private DataExportBackup() {}

    /** Backup mit automatisch generiertem Dateinamen. */
    static Path exportBackup(Path outDir) throws IOException {
        return exportBackup(outDir, NetworkStorePersistence.resolveDataDir());
    }

    /** Backup mit automatisch generiertem Dateinamen + explizitem Quellverzeichnis. */
    static Path exportBackup(Path outDir, Path srcDir) throws IOException {
        return exportBackup(outDir, srcDir, "nettool_backup_" + DataExportFormatters.now() + ".zip");
    }

    /**
     * Backup mit explizitem Dateinamen (wird von AutoBackup genutzt,
     * damit Test-Backups das TEST_BACKUP_PREFIX tragen können).
     */
    static Path exportBackup(Path outDir, String filename) throws IOException {
        return exportBackup(outDir, NetworkStorePersistence.resolveDataDir(), filename);
    }

    static Path exportBackup(Path outDir, Path srcDir, String filename) throws IOException {
        Files.createDirectories(outDir);
        Path zipFile = outDir.resolve(filename);
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(zipFile.toFile()))) {
            if (Files.isDirectory(srcDir)) {
                try (var stream = Files.walk(srcDir)) {
                    stream.filter(Files::isRegularFile).forEach(p -> {
                        try {
                            zos.putNextEntry(new ZipEntry(
                                    srcDir.relativize(p).toString().replace('\\', '/')));
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            DebugLogger.getInstance().log("FINE", "[DataExportBackup] Datei konnte nicht ins Backup aufgenommen werden (" + p + "): " + e);
                        }
                    });
                }
            }
        }
        return zipFile;
    }

    /**
     * Erstellt ein AES-256-GCM-verschlüsseltes Backup (additiv zu exportBackup()).
     * Das unverschlüsselte ZIP wird nur temporär angelegt und danach gelöscht.
     */
    static Path exportEncryptedBackup(Path outDir, String password) throws Exception {
        Path plain = exportBackup(outDir);
        Path encrypted = outDir.resolve(plain.getFileName() + BackupCrypto.ENCRYPTED_SUFFIX);
        try {
            BackupCrypto.encryptFile(plain, encrypted, password);
        } finally {
            Files.deleteIfExists(plain);
        }
        return encrypted;
    }
}
