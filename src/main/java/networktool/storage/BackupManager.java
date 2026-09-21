package main.java.networktool.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Sichert/stellt das saves/-Verzeichnis als ZIP. Vor jeder Wiederherstellung wird
 * der aktuelle Stand gesichert, damit bestehende Daten nie ungesichert überschrieben werden.
 */
public final class BackupManager {

    static final String FILE_PREFIX = "nettool_backup_";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private BackupManager() {}

    public static Path createBackup(Path sourceDir, Path targetDir) throws IOException {
        Path src  = sourceDir.toAbsolutePath().normalize();
        Path skip = targetDir.toAbsolutePath().normalize();
        Files.createDirectories(skip);
        Path zip = skip.resolve(FILE_PREFIX + LocalDateTime.now().format(FMT) + ".zip");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(src)) {
            files = walk.filter(Files::isRegularFile).filter(p -> !p.normalize().startsWith(skip)).toList();
        }
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Path file : files) addEntry(out, src, file);
        }
        return zip;
    }

    /** @return Anzahl wiederhergestellter Dateien. Neustart der Anwendung danach erforderlich. */
    public static int restoreBackup(Path zip, Path targetDir, Path safetyDir) throws IOException {
        Path root = targetDir.toAbsolutePath().normalize();
        validateEntries(zip, root);
        if (Files.isDirectory(root)) createBackup(root, safetyDir);
        return extract(zip, root);
    }

    private static void addEntry(ZipOutputStream out, Path root, Path file) throws IOException {
        String name = root.relativize(file).toString().replace('\\', '/');
        out.putNextEntry(new ZipEntry(name));
        Files.copy(file, (OutputStream) out);
        out.closeEntry();
    }

    private static void validateEntries(Path zip, Path root) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) resolveSafe(root, entry.getName());
        }
    }

    private static int extract(Path zip, Path root) throws IOException {
        int count = 0;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                Path dest = resolveSafe(root, entry.getName());
                Files.createDirectories(dest.getParent());
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        return count;
    }

    /** Schutz gegen Zip-Slip ("../" oder absolute Pfade). */
    private static Path resolveSafe(Path root, String name) throws IOException {
        Path dest = root.resolve(name).normalize();
        if (!dest.startsWith(root)) throw new IOException("Unzulässiger Pfad im Backup: " + name);
        return dest;
    }
}
