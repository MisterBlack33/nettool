package main.java.networktool.storage.export;

import main.java.networktool.storage.TestConstants;
import main.java.networktool.storage.network.NetworkStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workstream B: Regressionstests für {@link DataImporter} bei unvollständigen,
 * fehlerhaften oder veralteten Import-Dateien — bisher nur der Glücksfall
 * (vollständige, korrekt formatierte Zeilen) war abgedeckt.
 */
class DataImporterRegressionTest {

    private static final String CAT = TestConstants.TEST_PREFIX + "ws_b_import";

    @AfterEach
    void cleanup() {
        NetworkStore.getInstance().deleteNetwork(CAT);
    }

    // ── CSV: nur IP vorhanden, restliche Spalten fehlen ──────────────────

    @Test
    void importCsv_onlyIpColumn_hostnameFallsBackToIp(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("minimal.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n"
                + "10.5.5.5\n"); // nur IP, kein Trenner danach
        int count = DataImporter.importCsv(f);
        assertEquals(1, count);
        assertTrue(NetworkStore.getInstance().getAllHostsInternal().stream()
                .anyMatch(h -> "10.5.5.5".equals(h.ip)));
    }

    // ── CSV: völlig unstrukturierte Zeile wird übersprungen, nicht gezählt ─

    @Test
    void importCsv_garbageLine_skippedWithoutThrowing(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("garbage.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n"
                + "###not-a-valid-row###\n"
                + ";;;;;;\n"); // erste Spalte leer → blank, muss übersprungen werden
        assertEquals(0, DataImporter.importCsv(f));
    }

    // ── CSV: Notiz-Feld mit Semikolon verschiebt Folgespalten (Datenformat-Grenze) ─

    @Test
    void importCsv_semicolonInsideNoteField_stillProducesValidHost(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("semicolon.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n"
                + "10.6.6.6;host6;Linux;2024-01-01;;a;b;" + CAT + "\n");
        // split(";",7) begrenzt auf 7 Felder — die IP muss trotz zusätzlicher
        // Semikola im Rest der Zeile korrekt erkannt und gespeichert werden.
        int count = DataImporter.importCsv(f);
        assertEquals(1, count);
        assertTrue(NetworkStore.getInstance().getAllHostsInternal().stream()
                .anyMatch(h -> "10.6.6.6".equals(h.ip)));
    }

    // ── JSON: Datenmüll nach dem schließenden Array wird ignoriert ───────

    @Test
    void importJson_trailingGarbageAfterArray_ignored(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("trailing.json");
        Files.writeString(f, "[{\"ip\":\"10.7.7.7\",\"hostname\":\"h7\",\"os\":\"Linux\","
                + "\"category\":\"" + CAT + "\"}]\n### unrelated trailer content ###");
        assertEquals(1, DataImporter.importJson(f));
    }

    // ── JSON: fehlendes "hostname"-Feld fällt auf die IP zurück ──────────

    @Test
    void importJson_missingHostname_fallsBackToIp(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("no-hostname.json");
        Files.writeString(f, "[{\"ip\":\"10.8.8.8\",\"os\":\"Win\",\"category\":\"" + CAT + "\"}]");
        assertEquals(1, DataImporter.importJson(f));
        assertTrue(NetworkStore.getInstance().getAllHostsInternal().stream()
                .anyMatch(h -> "10.8.8.8".equals(h.ip) && "10.8.8.8".equals(h.hostname)));
    }

    // ── CSV: fehlende Kategorie-Spalte fällt auf "Import" zurück ─────────

    @Test
    void importCsv_missingCategoryColumn_defaultsToImport(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("no-cat.csv");
        Files.writeString(f, "IP;Hostname;OS;Datum;Ports;Notiz;Kategorie\n"
                + "10.9.9.9;host9;Linux\n"); // nur 3 Spalten
        assertEquals(1, DataImporter.importCsv(f));
        assertTrue(NetworkStore.getInstance().getNetworkNames().contains("Import")
                || NetworkStore.getInstance().getAllNetworkNames().contains("Import"));
        NetworkStore.getInstance().remove("10.9.9.9", "Import");
    }
}
