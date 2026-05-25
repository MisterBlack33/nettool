package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkStorePersistencePackageTest {

    @TempDir Path tmp;

    @Test
    void saveNetwork_createsFile() throws IOException {
        List<HostResult> hosts = List.of(
                new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux", "2024-01-01", null, "note"));
        NetworkStorePersistence.saveNetwork(tmp, TestConstants.NET_STANDARD, hosts, TestConstants.PREFIX_88);
        Path file = NetworkStorePersistence.savedDir(tmp).resolve(TestConstants.NET_STANDARD + ".json");
        assertTrue(Files.exists(file));
    }

    @Test
    void saveAndLoad_roundtrip() throws IOException {
        Map<Integer, String> ports = Map.of(22, "SSH", 80, "HTTP");
        HostResult h = new HostResult(TestConstants.IP_1, "router", "Linux", "2024-06-01", ports, "test note");
        NetworkStorePersistence.saveNetwork(tmp, TestConstants.NET_STANDARD, List.of(h), TestConstants.PREFIX_88);

        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        Map<String, String>           prefixes  = new LinkedHashMap<>();
        NetworkStorePersistence.loadAll(tmp, networks, prefixes);

        assertTrue(networks.containsKey(TestConstants.NET_STANDARD));
        assertEquals(1, networks.get(TestConstants.NET_STANDARD).size());
        assertEquals(TestConstants.IP_1, networks.get(TestConstants.NET_STANDARD).get(0).ip);
        assertEquals(TestConstants.PREFIX_88, prefixes.get(TestConstants.NET_STANDARD));
    }

    @Test
    void saveAllFile_createsAllJson() throws IOException {
        Map<String, List<HostResult>> networks = new LinkedHashMap<>();
        networks.put(TestConstants.NET_STANDARD,
                List.of(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Win", null)));
        NetworkStorePersistence.saveAllFile(tmp, networks);
        assertTrue(Files.exists(NetworkStorePersistence.savedDir(tmp)
                .resolve(NetworkStorePersistence.ALL_FILE)));
    }

    @Test void ntfyTopics_saveAndLoad() {
        NetworkStorePersistence.saveNtfyTopic(tmp, "my-topic");
        NetworkStorePersistence.saveNtfyTopic(tmp, "other-topic");
        List<String> topics = NetworkStorePersistence.loadNtfyTopics(tmp);
        assertTrue(topics.contains("my-topic"));
        assertTrue(topics.contains("other-topic"));
    }

    @Test void ntfyTopics_noDuplicates() {
        NetworkStorePersistence.saveNtfyTopic(tmp, "dup");
        NetworkStorePersistence.saveNtfyTopic(tmp, "dup");
        assertEquals(1, NetworkStorePersistence.loadNtfyTopics(tmp).stream()
                .filter("dup"::equals).count());
    }

    @Test void ntfyTopics_emptyWhenNoFile() {
        assertTrue(NetworkStorePersistence.loadNtfyTopics(tmp).isEmpty());
    }

    @Test void parsePorts_valid() {
        Map<Integer, String> p = NetworkStorePersistence.parsePorts("22:SSH,80:HTTP");
        assertEquals("SSH", p.get(22));
        assertEquals("HTTP", p.get(80));
    }

    @Test void parsePorts_empty_returnsEmpty() {
        assertTrue(NetworkStorePersistence.parsePorts("").isEmpty());
        assertTrue(NetworkStorePersistence.parsePorts(null).isEmpty());
    }

    @Test
    void loadLegacyFile_parsesCorrectly() throws IOException {
        Path file = tmp.resolve("legacy.txt");
        Files.writeString(file,
                "IP-PRÄFIX:" + TestConstants.PREFIX_88 + "\n"
                        + TestConstants.IP_1 + ";" + TestConstants.HOST_1 + ";Linux;2024-01-01;22:SSH;note1\n",
                StandardCharsets.UTF_8);
        Map<String, List<HostResult>> nets = new LinkedHashMap<>();
        Map<String, String>           pfx  = new LinkedHashMap<>();
        NetworkStorePersistence.loadLegacyFile(file, TestConstants.NET_STANDARD, nets, pfx);
        assertEquals(TestConstants.PREFIX_88, pfx.get(TestConstants.NET_STANDARD));
        assertEquals(TestConstants.IP_1, nets.get(TestConstants.NET_STANDARD).get(0).ip);
    }
}