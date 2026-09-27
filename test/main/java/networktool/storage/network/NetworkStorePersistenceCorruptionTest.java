package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workstream B: Verhalten von {@link NetworkStorePersistence#loadAll} bei
 * beschädigten oder unvollständigen Netzwerk-Dateien. Ein einzelnes defektes
 * ".json"-File darf weder den Ladevorgang abbrechen noch andere, gültige
 * Netzwerke im selben Verzeichnis verhindern.
 */
class NetworkStorePersistenceCorruptionTest {

    private Path savedDir(Path dataDir) throws Exception {
        Path dir = NetworkStorePersistence.savedDir(dataDir);
        Files.createDirectories(dir);
        return dir;
    }

    private void write(Path dir, String name, String content) throws Exception {
        Files.writeString(dir.resolve(name + ".json"), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ── Fehlendes "hosts"-Array ───────────────────────────────────────────

    @Test
    void loadAll_missingHostsArray_leavesNetworkEmptyWithoutThrowing(@TempDir Path tmp) throws Exception {
        Path dir = savedDir(tmp);
        write(dir, "NoHosts", "{\"schemaVersion\":1,\"network\":\"NoHosts\",\"prefix\":\"\"}");

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        assertDoesNotThrow(() -> NetworkStorePersistence.loadAll(tmp, networks, prefixes));
        assertTrue(networks.getOrDefault("NoHosts", List.of()).isEmpty());
    }

    // ── Trunkiertes / unvollständiges JSON ────────────────────────────────

    @Test
    void loadAll_truncatedJson_doesNotThrow_otherNetworksStillLoad(@TempDir Path tmp) throws Exception {
        Path dir = savedDir(tmp);
        write(dir, "Broken", "{\"schemaVersion\":1,\"network\":\"Broken\",\"hosts\":[{\"ip\":\"1.1.1.1\"");
        write(dir, "Fine", HostJsonBuilder.buildNetworkJson("Fine", "",
                List.of(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux"))));

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        assertDoesNotThrow(() -> NetworkStorePersistence.loadAll(tmp, networks, prefixes));
        assertTrue(networks.getOrDefault("Fine", List.of()).stream()
                .anyMatch(h -> TestConstants.IP_1.equals(h.ip)));
    }

    // ── Host-Objekt ohne "ip"-Feld wird übersprungen, Rest bleibt gültig ──

    @Test
    void loadAll_hostWithoutIp_skippedButSiblingsLoad(@TempDir Path tmp) throws Exception {
        Path dir = savedDir(tmp);
        String json = "{\"schemaVersion\":1,\"network\":\"Mixed\",\"prefix\":\"\",\"hosts\":[\n"
                + "  {\"hostname\":\"no-ip-host\",\"os\":\"Linux\"},\n"
                + "  {\"ip\":\"" + TestConstants.IP_2 + "\",\"hostname\":\"" + TestConstants.HOST_2
                + "\",\"os\":\"Win\"}\n]}";
        write(dir, "Mixed", json);

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp, networks, prefixes);

        List<HostResult> hosts = networks.getOrDefault("Mixed", List.of());
        assertEquals(1, hosts.size());
        assertEquals(TestConstants.IP_2, hosts.get(0).ip);
    }

    // ── Leeres Verzeichnis liefert leere, aber initialisierte Maps ───────

    @Test
    void loadAll_emptySavedDir_noEntriesNoException(@TempDir Path tmp) throws Exception {
        savedDir(tmp); // Verzeichnis existiert, aber ohne Dateien
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        assertDoesNotThrow(() -> NetworkStorePersistence.loadAll(tmp, networks, prefixes));
        assertTrue(networks.isEmpty());
        assertTrue(prefixes.isEmpty());
    }

    // ── all.json wird beim Laden ignoriert (nur generierte Übersicht) ────

    @Test
    void loadAll_ignoresGeneratedAllFile(@TempDir Path tmp) throws Exception {
        Path dir = savedDir(tmp);
        write(dir, "all", "{\"generated\":true,\"entries\":[{\"ip\":\"9.9.9.9\",\"category\":\"X\"}]}");

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp, networks, prefixes);
        assertFalse(networks.containsKey("all"),
                "all.json ist eine generierte Übersicht und kein eigenes Netzwerk");
    }
}
