package main.java.networktool.gui.components.actions;

import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GuiBackupActionsTest {

    @TempDir Path tmp;
    private final GuiOutputPanel output = new GuiOutputPanel();

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @Test void runBackupThenRestore_roundtrip() throws IOException {
        Path src = tmp.resolve("saves");
        Files.createDirectories(src);
        Files.writeString(src.resolve("f.txt"), "data");
        Path bk = tmp.resolve("bk");
        GuiBackupActions.runBackup(src, bk, output);
        Path zip;
        try (Stream<Path> s = Files.list(bk)) { zip = s.findFirst().orElseThrow(); }
        Files.delete(src.resolve("f.txt"));
        GuiBackupActions.runRestore(zip, src, tmp.resolve("safe"), output);
        assertEquals("data", Files.readString(src.resolve("f.txt")));
    }

    @Test void runRestore_missingZip_throws() {
        assertThrows(IOException.class, () -> GuiBackupActions.runRestore(
                tmp.resolve("no.zip"), tmp, tmp.resolve("safe"), output));
    }
}
