package main.java.networktool.logic.analysis.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class SecurityFindingsExporter {

    private static final String HEADER =
            "IP;Kategorie;Stufe;Konfidenz;Quelle;Beobachtet;Beleg / Detail";

    private SecurityFindingsExporter() {}

    public static Path exportCsv(Path file, List<SecurityFinding> findings) throws IOException {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (SecurityFinding finding : findings) {
            csv.append(cell(finding.ip())).append(';')
                    .append(cell(finding.category().name())).append(';')
                    .append(cell(finding.severity().name())).append(';')
                    .append(cell(finding.confidence().name())).append(';')
                    .append(cell(finding.source().name())).append(';')
                    .append(cell(finding.observedAt().toString())).append(';')
                    .append(cell(finding.detail())).append("\r\n");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, csv, StandardCharsets.UTF_8);
        return file;
    }

    private static String cell(String value) {
        int firstMeaningful = 0;
        while (firstMeaningful < value.length()
                && (Character.isWhitespace(value.charAt(firstMeaningful))
                || Character.isISOControl(value.charAt(firstMeaningful)))) {
            firstMeaningful++;
        }
        if (firstMeaningful < value.length()
                && "=+-@".indexOf(value.charAt(firstMeaningful)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
