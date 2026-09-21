package main.java.networktool.logic.scan.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OfflineTrackerStoreTest {

    @TempDir Path tmp;

    @Test void saveAndLoad_roundtrip() throws IOException {
        Path f = tmp.resolve("sub/state.tsv");
        OfflineTrackerStore.save(f, Map.of("1.1.1.1", 10L, "2.2.2.2", 20L));
        assertEquals(Map.of("1.1.1.1", 10L, "2.2.2.2", 20L), OfflineTrackerStore.load(f));
    }

    @Test void load_missingFile_empty() {
        assertTrue(OfflineTrackerStore.load(tmp.resolve("none.tsv")).isEmpty());
    }

    @Test void load_skipsCorruptLines() throws IOException {
        Path f = tmp.resolve("bad.tsv");
        Files.writeString(f, "1.1.1.1\t5\ngarbage\n2.2.2.2\tabc\n3.3.3.3\t7\n");
        assertEquals(Map.of("1.1.1.1", 5L, "3.3.3.3", 7L), OfflineTrackerStore.load(f));
    }

    @Test void load_unreadable_returnsEmpty() throws IOException {
        Path dirAsFile = Files.createDirectory(tmp.resolve("dir"));
        assertTrue(OfflineTrackerStore.load(dirAsFile).isEmpty());
    }
}
