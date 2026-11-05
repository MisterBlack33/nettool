package main.java.networktool.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageLocationsTest {

    @TempDir
    Path tempDir;

    @Test
    void migrateLegacyDataCopiesFilesAndPreservesExistingDestination() throws IOException {
        Path legacyRoot = tempDir.resolve("legacy");
        Path dataRoot = tempDir.resolve("data");
        Files.createDirectories(legacyRoot.resolve("networkdata"));
        Files.createDirectories(dataRoot.resolve("networkdata"));
        Files.writeString(legacyRoot.resolve("networkdata/hosts.json"), "legacy");
        Files.writeString(legacyRoot.resolve("networkdata/settings.json"), "new legacy file");
        Files.writeString(dataRoot.resolve("networkdata/hosts.json"), "current");

        StorageLocations.migrateLegacyData(legacyRoot, dataRoot);

        assertEquals("current", Files.readString(dataRoot.resolve("networkdata/hosts.json")));
        assertEquals("new legacy file", Files.readString(dataRoot.resolve("networkdata/settings.json")));
        assertTrue(Files.exists(legacyRoot.resolve("networkdata/hosts.json")));
    }

    @Test
    void migrateLegacyDataDoesNotCopyIntoLegacyTree() throws IOException {
        Path legacyRoot = tempDir.resolve("legacy");
        Files.createDirectories(legacyRoot);
        Files.writeString(legacyRoot.resolve("state.json"), "state");

        StorageLocations.migrateLegacyData(legacyRoot, legacyRoot.resolve("nested-data"));

        assertTrue(Files.exists(legacyRoot.resolve("state.json")));
        assertTrue(Files.notExists(legacyRoot.resolve("nested-data/state.json")));
    }
}
