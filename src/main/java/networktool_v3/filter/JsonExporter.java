package main.java.networktool_v3.filter;

import main.java.networktool_v3.model.ScanResult;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringJoiner;

/**
 * Exportiert eine Liste von {@link ScanResult}s als JSON-Datei.
 *
 * Kein externer JSON-Parser nötig – die einfache Struktur
 * wird direkt als String aufgebaut.
 */
public final class JsonExporter {

    private JsonExporter() {}

    /**
     * Speichert die Scan-Ergebnisse in eine JSON-Datei.
     *
     * @param results  zu exportierende Ergebnisse
     * @param filePath Zielpfad der JSON-Datei
     */
    public static void save(List<ScanResult> results, String filePath) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(toJson(results));
            System.out.println("Ergebnisse gespeichert in: " + filePath);
        } catch (IOException e) {
            System.err.println("JsonExporter: Fehler beim Speichern: " + e.getMessage());
        }
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private static String toJson(List<ScanResult> results) {
        StringJoiner entries = new StringJoiner(",\n", "[\n", "\n]");
        for (ScanResult r : results) {
            entries.add(toJsonObject(r));
        }
        return entries.toString();
    }

    private static String toJsonObject(ScanResult r) {
        return "  {\n"
             + "    \"ip\": \""       + escape(r.getIp())       + "\",\n"
             + "    \"hostname\": \"" + escape(r.getHostname())  + "\",\n"
             + "    \"os\": \""       + escape(r.getOsGuess())   + "\",\n"
             + "    \"ports\": "      + r.getOpenPorts().keySet() + "\n"
             + "  }";
    }

    /** Maskiert Sonderzeichen für JSON-Strings. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
