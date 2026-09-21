package main.java.networktool.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BackupManagerTest {

    @TempDir Path tmp;
    Path src;

    @BeforeEach void setup() throws IOException {
        src = tmp.resolve("saves");
        Files.createDirectories(src.resolve("a"));
        Files.writeString(src.resolve("a/b.txt"), "B");
        Files.writeString(src.resolve("c.txt"), "C");
    }

    private Set<String> entries(Path zip) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipFile z = new ZipFile(zip.toFile())) {
            z.stream().map(ZipEntry::getName).forEach(names::add);
        }
        return names;
    }

    private long fileCount(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) { return s.count(); }
    }

    @Test void createBackup_containsRelativeFiles() throws IOException {
        Path zip = BackupManager.createBackup(src, tmp.resolve("out"));
        assertTrue(zip.getFileName().toString().startsWith(BackupManager.FILE_PREFIX));
        assertEquals(Set.of("a/b.txt", "c.txt"), entries(zip));
    }

    @Test void createBackup_excludesTargetInsideSource() throws IOException {
        Path inside = src.resolve("backups");
        BackupManager.createBackup(src, inside);
        Path second = BackupManager.createBackup(src, inside);
        assertEquals(Set.of("a/b.txt", "c.txt"), entries(second));
    }

    @Test void createBackup_missingSource_throws() {
        assertThrows(IOException.class, () -> BackupManager.createBackup(tmp.resolve("none"), tmp.resolve("o")));
    }

    @Test void restore_roundtrip_restoresContent() throws IOException {
        Path zip = BackupManager.createBackup(src, tmp.resolve("out"));
        Files.delete(src.resolve("c.txt"));
        Files.writeString(src.resolve("a/b.txt"), "changed");
        int n = BackupManager.restoreBackup(zip, src, tmp.resolve("safe"));
        assertEquals(2, n);
        assertEquals("C", Files.readString(src.resolve("c.txt")));
        assertEquals("B", Files.readString(src.resolve("a/b.txt")));
    }

    @Test void restore_createsSafetyBackupOfCurrentState() throws IOException {
        Path zip = BackupManager.createBackup(src, tmp.resolve("out"));
        Files.writeString(src.resolve("c.txt"), "current");
        Path safe = tmp.resolve("safe");
        BackupManager.restoreBackup(zip, src, safe);
        assertEquals(1, fileCount(safe));
    }

    @Test void restore_toNewDirectory_skipsSafetyBackup() throws IOException {
        Path zip = BackupManager.createBackup(src, tmp.resolve("out"));
        Path fresh = tmp.resolve("fresh");
        assertEquals(2, BackupManager.restoreBackup(zip, fresh, tmp.resolve("safe")));
        assertFalse(Files.exists(tmp.resolve("safe")));
        assertEquals("C", Files.readString(fresh.resolve("c.txt")));
    }

    @Test void restore_zipSlip_rejectedBeforeAnyWrite() throws IOException {
        Path evil = tmp.resolve("evil.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(evil))) {
            out.putNextEntry(new ZipEntry("ok.txt"));
            out.write('x');
            out.closeEntry();
            out.putNextEntry(new ZipEntry("../escaped.txt"));
            out.write('x');
            out.closeEntry();
        }
        assertThrows(IOException.class, () -> BackupManager.restoreBackup(evil, src, tmp.resolve("safe")));
        assertFalse(Files.exists(tmp.resolve("escaped.txt")));
        assertFalse(Files.exists(src.resolve("ok.txt")));
    }

    @Test void restore_missingZip_throws() {
        assertThrows(IOException.class,
                () -> BackupManager.restoreBackup(tmp.resolve("no.zip"), src, tmp.resolve("safe")));
    }

    @Test void restore_directoryEntries_ignored() throws IOException {
        Path zip = tmp.resolve("dirs.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("d/"));
            out.closeEntry();
        }
        assertEquals(0, BackupManager.restoreBackup(zip, tmp.resolve("t"), tmp.resolve("safe")));
    }
}
