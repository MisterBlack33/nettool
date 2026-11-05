package main.java.networktool.logic.scan.schedule;

import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScanHistoryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_preservesLatestComparisonsAndPortEvidence() throws Exception {
        ScanHistoryStore store = new ScanHistoryStore(tempDir.resolve("history.bin"));
        ScanHistory.Entry entry = new ScanHistory.Entry("192.168.1.0/24",
                List.of(new ScanResult("192.168.1.5", "printer", Map.of(443, "HTTPS"), "Linux")),
                "2026-11-05 12:00:00");

        store.save(List.of(entry));

        ScanHistory.Entry loaded = store.load().get(0);
        assertEquals(entry.label, loaded.label);
        assertEquals(entry.timestamp, loaded.timestamp);
        assertEquals(1, loaded.results.size());
        assertEquals("192.168.1.5", loaded.results.get(0).getIp());
        assertEquals(Map.of(443, "HTTPS"), loaded.results.get(0).getOpenPorts());
    }

    @Test
    void load_rejectsCorruptedHeader() throws Exception {
        Path file = tempDir.resolve("bad.bin");
        java.nio.file.Files.write(file, new byte[]{0, 0, 0, 0});

        assertThrows(java.io.IOException.class, () -> new ScanHistoryStore(file).load());
    }
}
