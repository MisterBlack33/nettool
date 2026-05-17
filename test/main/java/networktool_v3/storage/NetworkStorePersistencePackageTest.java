package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NetworkStorePersistence (package-private).
 * Must be in package main.java.networktool_v3.storage.
 */
class NetworkStorePersistencePackageTest {

    @TempDir Path tmp;

    @Test
    void saveNetwork_createsFile() throws IOException {
        List<HostResult> hosts = List.of(
                new HostResult("10.0.0.1", "host1", "Linux", "2024-01-01", null, "note"));
        NetworkStorePersistence.saveNetwork(tmp, "home", hosts, "10.0.0.");
        Path file = NetworkStorePersistence.savedDir(tmp).resolve("home.json");
        assertTrue(Files.exists(file));
    }

    @Test
    void saveAndLoad_roundtrip() throws IOException {
        Map<Integer, String> ports = Map.of(22, "SSH", 80, "HTTP");
        HostResult h = new HostResult("192.168.1.1", "router", "Linux", "2024-06-01", ports, "test note");
        NetworkStorePersistence.saveNetwork(tmp, "net1", List.of(h), "192.168.1.");

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String> prefixes = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp, networks, prefixes);

        assertTrue(networks.containsKey("net1"));
        assertEquals(1, networks.get("net1").size());
        assertEquals("192.168.1.1", networks.get("net1").get(0).ip);
        assertEquals("192.168.1.", prefixes.get("net1"));
    }

    @Test
    void saveAllFile_createsAllJson() throws IOException {
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        networks.put("test", List.of(new HostResult("1.2.3.4", "h", "Win", null)));
        NetworkStorePersistence.saveAllFile(tmp, networks);
        assertTrue(Files.exists(NetworkStorePersistence.savedDir(tmp)
                .resolve(NetworkStorePersistence.ALL_FILE)));
    }

    @Test
    void ntfyTopics_saveAndLoad() {
        NetworkStorePersistence.saveNtfyTopic(tmp, "my-topic");
        NetworkStorePersistence.saveNtfyTopic(tmp, "other-topic");
        List<String> topics = NetworkStorePersistence.loadNtfyTopics(tmp);
        assertTrue(topics.contains("my-topic"));
        assertTrue(topics.contains("other-topic"));
    }

    @Test
    void ntfyTopics_noDuplicates() {
        NetworkStorePersistence.saveNtfyTopic(tmp, "dup");
        NetworkStorePersistence.saveNtfyTopic(tmp, "dup");
        List<String> topics = NetworkStorePersistence.loadNtfyTopics(tmp);
        assertEquals(1, topics.stream().filter("dup"::equals).count());
    }

    @Test
    void ntfyTopics_emptyWhenNoFile() {
        assertTrue(NetworkStorePersistence.loadNtfyTopics(tmp).isEmpty());
    }

    @Test
    void parsePorts_valid() {
        Map<Integer, String> p = NetworkStorePersistence.parsePorts("22:SSH,80:HTTP");
        assertEquals("SSH", p.get(22));
        assertEquals("HTTP", p.get(80));
    }

    @Test
    void parsePorts_empty_returnsEmpty() {
        assertTrue(NetworkStorePersistence.parsePorts("").isEmpty());
        assertTrue(NetworkStorePersistence.parsePorts(null).isEmpty());
    }

    @Test
    void loadLegacyFile_parsesCorrectly() throws IOException {
        Path file = tmp.resolve("legacy.txt");
        Files.writeString(file,
                "IP-PRÄFIX:10.0.\n10.0.0.1;server;Linux;2024-01-01;22:SSH;note1\n",
                StandardCharsets.UTF_8);
        Map<String, List<HostResult>> nets = new LinkedHashMap<>();
        Map<String, String> pfx = new LinkedHashMap<>();
        NetworkStorePersistence.loadLegacyFile(file, "legacy", nets, pfx);
        assertEquals("10.0.", pfx.get("legacy"));
        assertEquals("10.0.0.1", nets.get("legacy").get(0).ip);
    }
}
