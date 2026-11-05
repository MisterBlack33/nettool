package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityFindingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_preservesEvidenceAndResolvedStatus() throws Exception {
        SecurityFindingsStore store = new SecurityFindingsStore(tempDir.resolve("history.json"));
        SecurityFinding finding = new SecurityFinding("10.0.0.4",
                SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.WARN,
                "quote \" slash \\ line\nreturn\rtab\t",
                SecurityFinding.Source.TLS_CERT_INSPECTOR,
                SecurityFinding.Confidence.CONFIRMED,
                Instant.parse("2026-10-29T12:00:00Z"),
                SecurityFinding.Status.RESOLVED);

        store.save(List.of(finding));

        assertEquals(List.of(finding), store.load());
    }

    @Test
    void load_missingFile_returnsEmptyHistory() throws Exception {
        assertTrue(new SecurityFindingsStore(tempDir.resolve("missing.json")).load().isEmpty());
    }

    @Test
    void load_malformedHistoryReportsAnError() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{\"notFindings\": []}");

        assertThrows(java.io.IOException.class, () -> new SecurityFindingsStore(file).load());
    }

    @Test
    void load_unsupportedVersionReportsAnError() throws Exception {
        Path file = tempDir.resolve("future.json");
        Files.writeString(file, "{\"version\": 2, \"findings\": []}");

        assertThrows(java.io.IOException.class, () -> new SecurityFindingsStore(file).load());
    }
}
