package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;
import main.java.networktool.storage.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NetworkStorePersistenceEdgeTest {
    @TempDir Path tmp;
    @Test void loadAll_noSavedDir_leavesMapsEmpty() {
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp.resolve("nonexistent"), networks, prefixes);
        assertTrue(networks.isEmpty()); assertTrue(prefixes.isEmpty());
    }
    @Test void saveNetwork_missingPrefix_defaultsToEmptyString() throws IOException {
        NetworkStorePersistence.saveNetwork(tmp, TestConstants.NET_STANDARD,
                List.of(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux")), null);
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp, networks, prefixes);
        assertEquals("", prefixes.get(TestConstants.NET_STANDARD));
    }
    @Test void saveAllFile_dedupesRepeatedIpAcrossNetworks() throws IOException {
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        networks.put("A", List.of(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux")));
        networks.put("B", List.of(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux")));
        NetworkStorePersistence.saveAllFile(tmp, networks);
        String json = Files.readString(NetworkStorePersistence.savedDir(tmp).resolve(NetworkStorePersistence.ALL_FILE));
        assertEquals(1, json.split("\"ip\":", -1).length - 1);
    }
    @Test void extractStr_and_esc_delegateToJsonCodec() {
        assertEquals("v", NetworkStorePersistence.extractStr("{\"k\":\"v\"}", "k"));
        assertEquals("a\\\"b", NetworkStorePersistence.esc("a\"b"));
    }
}
