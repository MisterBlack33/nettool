package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkRegistryTest {

    @Nested
    class RegistryTest {

        NetworkRegistry reg;

        @BeforeEach
        void setup() {
            reg = new NetworkRegistry();
        }

        @Test
        void create_success() {
            assertTrue(reg.create("Home", "192.168.1."));
            assertTrue(reg.contains("Home"));
        }

        @Test
        void create_duplicate_rejected() {
            reg.create("Home", "");
            assertFalse(reg.create("Home", ""));
        }

        @Test
        void create_allCategory_rejected() {
            assertFalse(reg.create(NetworkRegistry.ALL_CATEGORY, ""));
        }

        @Test
        void create_blank_rejected() {
            assertFalse(reg.create("  ", ""));
        }

        @Test
        void names_includesAllCategory() {
            reg.create("Work", "10.0.");
            assertTrue(reg.names().contains(NetworkRegistry.ALL_CATEGORY));
            assertTrue(reg.names().contains("Work"));
        }

        @Test
        void prefix_returnsCorrect() {
            reg.create("Net", "172.16.");
            assertEquals("172.16.", reg.prefix("Net"));
        }

        @Test
        void prefix_unknown_returnsEmpty() {
            assertEquals("", reg.prefix("unknown"));
        }

        @Test
        void ipMatches_allCategory_always() {
            assertTrue(reg.ipMatches("1.2.3.4", NetworkRegistry.ALL_CATEGORY));
        }

        @Test
        void ipMatches_withPrefix_true() {
            reg.create("Net", "10.0.");
            assertTrue(reg.ipMatches("10.0.5.1", "Net"));
        }

        @Test
        void ipMatches_withPrefix_false() {
            reg.create("Net", "10.0.");
            assertFalse(reg.ipMatches("192.168.1.1", "Net"));
        }

        @Test
        void ipMatches_emptyPrefix_alwaysTrue() {
            reg.create("Any", "");
            assertTrue(reg.ipMatches("1.2.3.4", "Any"));
        }

        @Test
        void matchingNetworks_returnsAll() {
            reg.create("A", "10.");
            reg.create("B", "10.");
            reg.create("C", "192.");
            List<String> matches = reg.matchingNetworks("10.0.0.1");
            assertTrue(matches.contains("A"));
            assertTrue(matches.contains("B"));
            assertFalse(matches.contains("C"));
        }

        @Test
        void ensureDefault_addsStandardWhenEmpty() {
            reg.ensureDefault();
            assertTrue(reg.contains(NetworkRegistry.DEFAULT_CAT));
        }

        @Test
        void ensureDefault_doesNotAddWhenNotEmpty() {
            reg.create("X", "");
            reg.ensureDefault();
            assertFalse(reg.contains(NetworkRegistry.DEFAULT_CAT));
        }

        @Test
        void networks_isLive() {
            reg.create("Live", "");
            reg.networks().get("Live").add(new HostResult("1.1.1.1", "h", "Lin"));
            assertEquals(1, reg.networks().get("Live").size());
        }
    }

    @Nested
    class LegacyTest {

        @TempDir Path tmp;

        @Test
        void parsePorts_valid() {
            Map<Integer, String> p = NetworkStoreLegacy.parsePorts("22:SSH,80:HTTP");
            assertEquals("SSH",  p.get(22));
            assertEquals("HTTP", p.get(80));
        }

        @Test
        void parsePorts_noLabel_defaultsToOffen() {
            assertEquals("offen", NetworkStoreLegacy.parsePorts("443").get(443));
        }

        @Test
        void parsePorts_empty_returnsEmpty() {
            assertTrue(NetworkStoreLegacy.parsePorts("").isEmpty());
            assertTrue(NetworkStoreLegacy.parsePorts(null).isEmpty());
        }

        @Test
        void parsePorts_malformed_ignored() {
            var m = NetworkStoreLegacy.parsePorts("abc:def,22:SSH");
            assertFalse(m.containsKey(0));
            assertTrue(m.containsKey(22));
        }

        @Test
        void loadFile_parsesIpAndHostname() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, "192.168.1.1;server;Linux;2024-01-01;22:SSH;note\n",
                    StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, "test", nets, null);
            assertEquals(1, nets.get("test").size());
            assertEquals("192.168.1.1", nets.get("test").get(0).ip);
        }

        @Test
        void loadFile_parsesPrefix() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, "IP-PRÄFIX:10.0.\n10.0.0.1;h;Win\n",
                    StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            Map<String, String>           pfx  = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, "test", nets, pfx);
            assertEquals("10.0.", pfx.get("test"));
        }

        @Test
        void loadFile_skipsBlankAndComment() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, "\n# comment\n\n", StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, "test", nets, null);
            assertTrue(nets.get("test").isEmpty());
        }
    }

    @Nested
    class NtfyTest {

        @TempDir Path tmp;

        @Test
        void saveAndLoad_topic() {
            NetworkStoreNtfy.saveTopic(tmp, "my-topic");
            assertTrue(NetworkStoreNtfy.loadTopics(tmp).contains("my-topic"));
        }

        @Test
        void noDuplicates() {
            NetworkStoreNtfy.saveTopic(tmp, "dup");
            NetworkStoreNtfy.saveTopic(tmp, "dup");
            assertEquals(1, NetworkStoreNtfy.loadTopics(tmp).stream()
                    .filter("dup"::equals).count());
        }

        @Test
        void empty_whenNoFile() {
            assertTrue(NetworkStoreNtfy.loadTopics(tmp).isEmpty());
        }

        @Test
        void blank_ignored() {
            NetworkStoreNtfy.saveTopic(tmp, "  ");
            assertTrue(NetworkStoreNtfy.loadTopics(tmp).isEmpty());
        }

        @Test
        void sorted_alphabetically() {
            NetworkStoreNtfy.saveTopic(tmp, "zzz");
            NetworkStoreNtfy.saveTopic(tmp, "aaa");
            List<String> topics = NetworkStoreNtfy.loadTopics(tmp);
            assertEquals("aaa", topics.get(0));
        }
    }
}