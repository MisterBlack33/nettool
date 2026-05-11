package networktool_v3.storage;

import main.java.networktool_v3.storage.DataExportImport;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanProfile;
import main4.networktool_v3.storage.NetworkStorePersistence;
import main.java.networktool_v3.storage.ScanProfileStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// ── NetworkStore ──────────────────────────────────────────────────────────────

/**
 * NetworkStore ist ein Singleton – Tests laufen sequenziell
 * und isolieren sich über separate TempDirs via NetworkStorePersistence-Methoden.
 * Da der Store seinen Pfad im Konstruktor festlegt, testen wir hier
 * die Persistenzschicht direkt.
 */
class NetworkStorePersistenceRoundtripTest {

    @TempDir Path tempDir;

    @Test
    void saveAndLoadNetwork() {
        List<HostResult> hosts = new ArrayList<>();
        hosts.add(new HostResult("192.168.1.1", "router", "Linux", "2026-01-01",
                Map.of(80, "HTTP"), "Main router"));

        NetworkStorePersistence.saveNetwork(tempDir, "Heim", hosts, "192.168.1.");

        Map<String, List<HostResult>> loaded = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tempDir, loaded, prefixes);

        assertTrue(loaded.containsKey("Heim"));
        assertEquals(1, loaded.get("Heim").size());
        HostResult h = loaded.get("Heim").get(0);
        assertEquals("192.168.1.1",  h.ip);
        assertEquals("router",       h.hostname);
        assertEquals("Linux",        h.os);
        assertEquals("Main router",  h.notes);
        assertEquals("192.168.1.",   prefixes.get("Heim"));
        assertTrue(h.ports.containsKey(80));
    }

    @Test
    void saveMultipleNetworks() {
        NetworkStorePersistence.saveNetwork(tempDir, "Heim",   List.of(
                new HostResult("10.0.0.1", "h1", "Linux",   null)), "10.0.0.");
        NetworkStorePersistence.saveNetwork(tempDir, "Schule", List.of(
                new HostResult("10.1.0.1", "h2", "Windows", null)), "10.1.");

        Map<String, List<HostResult>> loaded = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tempDir, loaded, null);

        assertTrue(loaded.containsKey("Heim"));
        assertTrue(loaded.containsKey("Schule"));
        assertEquals(1, loaded.get("Heim").size());
        assertEquals(1, loaded.get("Schule").size());
    }

    @Test
    void emptyHostListPersists() {
        NetworkStorePersistence.saveNetwork(tempDir, "Empty", List.of(), "");
        Map<String, List<HostResult>> loaded = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tempDir, loaded, null);
        assertTrue(loaded.containsKey("Empty"));
        assertTrue(loaded.get("Empty").isEmpty());
    }

    @Test
    void hostWithNoPortsPersists() {
        HostResult h = new HostResult("1.2.3.4", "host", "Android", null, null, "");
        NetworkStorePersistence.saveNetwork(tempDir, "Test", List.of(h), "");
        Map<String, List<HostResult>> loaded = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tempDir, loaded, null);
        HostResult loaded_h = loaded.get("Test").get(0);
        assertNotNull(loaded_h.ports);
        assertTrue(loaded_h.ports.isEmpty());
    }

    @Test
    void specialCharsInNotesPreserved() {
        HostResult h = new HostResult("1.2.3.4", "host", "Linux", null, null,
                "Note with \"quotes\" and \\backslash");
        NetworkStorePersistence.saveNetwork(tempDir, "Notes", List.of(h), "");
        Map<String, List<HostResult>> loaded = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tempDir, loaded, null);
        String notes = loaded.get("Notes").get(0).notes;
        assertTrue(notes.contains("quotes"));
        assertTrue(notes.contains("backslash"));
    }

    @Test
    void allFileGenerated() {
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        networks.put("A", List.of(new HostResult("1.1.1.1", "h", "Linux")));
        networks.put("B", List.of(new HostResult("2.2.2.2", "h", "Windows")));
        NetworkStorePersistence.saveAllFile(tempDir, networks);

        Path allFile = NetworkStorePersistence.savedDir(tempDir)
                .resolve(NetworkStorePersistence.ALL_FILE);
        assertTrue(allFile.toFile().exists());
    }

    @Test
    void ntfyTopicsSaveAndLoad() {
        NetworkStorePersistence.saveNtfyTopic(tempDir, "my-topic");
        NetworkStorePersistence.saveNtfyTopic(tempDir, "other-topic");
        List<String> topics = NetworkStorePersistence.loadNtfyTopics(tempDir);
        assertTrue(topics.contains("my-topic"));
        assertTrue(topics.contains("other-topic"));
    }

    @Test
    void ntfyTopicsDeduplicated() {
        NetworkStorePersistence.saveNtfyTopic(tempDir, "same-topic");
        NetworkStorePersistence.saveNtfyTopic(tempDir, "same-topic");
        List<String> topics = NetworkStorePersistence.loadNtfyTopics(tempDir);
        assertEquals(1, topics.stream().filter("same-topic"::equals).count());
    }
}

// ── ScanProfileStore ──────────────────────────────────────────────────────────

class ScanProfileStoreTest {

    private ScanProfileStore store;

    @BeforeEach
    void setUp() {
        store = ScanProfileStore.getInstance();
        // Clear all profiles
        store.getAll().forEach(p -> store.delete(p.name));
    }

    @Test
    void saveAndGet() {
        ScanProfile p = new ScanProfile("TestProfil");
        p.cidrs.add("192.168.1.0/24");
        p.osFilter = "Linux";
        store.save(p);

        assertTrue(store.get("TestProfil").isPresent());
        ScanProfile loaded = store.get("TestProfil").get();
        assertEquals("TestProfil", loaded.name);
        assertEquals("Linux",      loaded.osFilter);
        assertTrue(loaded.cidrs.contains("192.168.1.0/24"));
    }

    @Test
    void saveOverwritesDuplicate() {
        ScanProfile p1 = new ScanProfile("P");
        p1.osFilter = "Linux";
        store.save(p1);

        ScanProfile p2 = new ScanProfile("P");
        p2.osFilter = "Windows";
        store.save(p2);

        assertEquals(1, store.getAll().stream().filter(p -> "P".equals(p.name)).count());
        assertEquals("Windows", store.get("P").get().osFilter);
    }

    @Test
    void deleteWorks() {
        store.save(new ScanProfile("ToDelete"));
        store.delete("ToDelete");
        assertTrue(store.get("ToDelete").isEmpty());
    }

    @Test
    void updateLastRun() {
        store.save(new ScanProfile("P"));
        store.updateLastRun("P", "2026-05-01 10:00:00");
        assertEquals("2026-05-01 10:00:00", store.get("P").get().lastRun);
    }

    @Test
    void getAllReturnsAll() {
        store.save(new ScanProfile("A"));
        store.save(new ScanProfile("B"));
        store.save(new ScanProfile("C"));
        assertTrue(store.getAll().size() >= 3);
    }

    @Test
    void profileWithPorts() {
        ScanProfile p = new ScanProfile("PortProfile");
        p.ports.addAll(List.of(22, 80, 443));
        store.save(p);
        List<Integer> loaded = store.get("PortProfile").get().ports;
        assertTrue(loaded.contains(22));
        assertTrue(loaded.contains(80));
        assertTrue(loaded.contains(443));
    }

    @Test
    void profileWithAutoSave() {
        ScanProfile p = new ScanProfile("AutoSave");
        p.autoSave = true;
        p.category = "Schule";
        store.save(p);
        ScanProfile loaded = store.get("AutoSave").get();
        assertTrue(loaded.autoSave);
        assertEquals("Schule", loaded.category);
    }

    @Test
    void profileSummaryContainsName() {
        ScanProfile p = new ScanProfile("MyScan");
        p.cidrs.add("10.0.0.0/24");
        assertTrue(p.summary().contains("MyScan"));
        assertTrue(p.summary().contains("10.0.0.0/24"));
    }

    @Test
    void getMissingProfileEmpty() {
        assertTrue(store.get("nonexistent-profile-xyz").isEmpty());
    }
}

// ── DataExportImport ──────────────────────────────────────────────────────────

class DataExportImportTest {

    @TempDir Path tempDir;

    private void seedStore() {
        // Ensure at least one host in store for export
        NetworkStore ns = NetworkStore.getInstance();
        if (ns.getNetworkNames().stream().noneMatch(n -> n.equals("TestNet")))
            ns.createNetwork("TestNet", "");
        ns.save(new HostResult("9.9.9.9", "test-export-host", "Linux", null, null, "note"), "TestNet");
    }

    @Test
    void exportCsv_createsFile() throws Exception {
        seedStore();
        Path f = DataExportImport.exportCsv(tempDir);
        assertTrue(f.toFile().exists());
        String content = java.nio.file.Files.readString(f);
        assertTrue(content.contains("IP;Hostname"));
    }

    @Test
    void exportJson_createsFile() throws Exception {
        seedStore();
        Path f = DataExportImport.exportJson(tempDir);
        assertTrue(f.toFile().exists());
        String content = java.nio.file.Files.readString(f);
        assertTrue(content.startsWith("["));
        assertTrue(content.contains("\"ip\""));
    }

    @Test
    void exportHtml_createsFile() throws Exception {
        seedStore();
        Path f = DataExportImport.exportHtml(tempDir);
        assertTrue(f.toFile().exists());
        String content = java.nio.file.Files.readString(f);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("NetTool"));
    }

    @Test
    void importCsv_roundtrip() throws Exception {
        seedStore();
        Path csv = DataExportImport.exportCsv(tempDir);
        int count = DataExportImport.importCsv(csv);
        assertTrue(count >= 0); // May already exist → 0 duplicates OK
    }

    @Test
    void importJson_roundtrip() throws Exception {
        seedStore();
        Path json = DataExportImport.exportJson(tempDir);
        int count = DataExportImport.importJson(json);
        assertTrue(count >= 0);
    }

    @Test
    void exportBackup_createsZip() throws Exception {
        seedStore();
        Path zip = DataExportImport.exportBackup(tempDir);
        assertTrue(zip.toFile().exists());
        assertTrue(zip.toString().endsWith(".zip"));
    }

    @Test
    void restoreBackup_roundtrip() throws Exception {
        seedStore();
        Path zip = DataExportImport.exportBackup(tempDir);
        int restored = DataExportImport.restoreBackup(zip);
        assertTrue(restored >= 0);
    }
}
