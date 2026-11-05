package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityFindingsExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportCsv_writesMetadataAndEscapesCsvCells() throws Exception {
        Instant observedAt = Instant.parse("2026-10-29T17:00:00Z");
        SecurityFinding finding = new SecurityFinding("10.0.0.1",
                SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.WARN, "Banner says \"legacy; service\"\ncheck",
                SecurityFinding.Source.CVE_BANNER_MATCH,
                SecurityFinding.Confidence.LOW, observedAt);
        Path file = tempDir.resolve("reports").resolve("findings.csv");

        assertEquals(file, SecurityFindingsExporter.exportCsv(file, List.of(finding)));
        assertEquals(
                "IP;Kategorie;Stufe;Konfidenz;Quelle;Beobachtet;Beleg / Detail\r\n"
                        + "\"10.0.0.1\";\"KNOWN_VULNERABLE\";\"WARN\";\"LOW\";"
                        + "\"CVE_BANNER_MATCH\";\"2026-10-29T17:00:00Z\";"
                        + "\"Banner says \"\"legacy; service\"\"\ncheck\"\r\n",
                Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void exportCsv_prefixesFormulaLikeUntrustedDetails() throws Exception {
        SecurityFinding finding = new SecurityFinding("10.0.0.1",
                SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.WARN, "\t=HYPERLINK(\"https://example.invalid\")",
                SecurityFinding.Source.CVE_BANNER_MATCH,
                SecurityFinding.Confidence.LOW, Instant.parse("2026-10-29T17:00:00Z"));
        Path file = tempDir.resolve("formula.csv");

        SecurityFindingsExporter.exportCsv(file, List.of(finding));

        assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                .contains("\"'\t=HYPERLINK(\"\"https://example.invalid\"\")\""));
    }
}
