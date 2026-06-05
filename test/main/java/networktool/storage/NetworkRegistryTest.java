package main.java.networktool.storage;

import main.java.networktool.model.HostResult;
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

        @BeforeEach void setup() { reg = new NetworkRegistry(); }

        @Test void create_success()             { assertTrue(reg.create(TestConstants.NET_STANDARD, TestConstants.PREFIX_88)); assertTrue(reg.contains(TestConstants.NET_STANDARD)); }
        @Test void create_duplicate_rejected()  { reg.create(TestConstants.NET_STANDARD, ""); assertFalse(reg.create(TestConstants.NET_STANDARD, "")); }
        @Test void create_allCategory_rejected(){ assertFalse(reg.create(NetworkRegistry.ALL_CATEGORY, "")); }
        @Test void create_blank_rejected()      { assertFalse(reg.create("  ", "")); }

        @Test void names_includesAllCategory() {
            reg.create(TestConstants.NET_STANDARD, TestConstants.PREFIX_99);
            assertTrue(reg.names().contains(NetworkRegistry.ALL_CATEGORY));
            assertTrue(reg.names().contains(TestConstants.NET_STANDARD));
        }

        @Test void prefix_returnsCorrect() {
            reg.create(TestConstants.NET_STANDARD, TestConstants.PREFIX_88);
            assertEquals(TestConstants.PREFIX_88, reg.prefix(TestConstants.NET_STANDARD));
        }

        @Test void prefix_unknown_returnsEmpty()      { assertEquals("", reg.prefix("unknown")); }
        @Test void ipMatches_allCategory_always()     { assertTrue(reg.ipMatches("1.2.3.4", NetworkRegistry.ALL_CATEGORY)); }

        @Test void ipMatches_withPrefix_true() {
            reg.create(TestConstants.NET_STANDARD, TestConstants.PREFIX_88);
            assertTrue(reg.ipMatches(TestConstants.IP_1, TestConstants.NET_STANDARD));
        }

        @Test void ipMatches_withPrefix_false() {
            reg.create(TestConstants.NET_STANDARD, TestConstants.PREFIX_88);
            assertFalse(reg.ipMatches("192.168.1.1", TestConstants.NET_STANDARD));
        }

        @Test void ipMatches_emptyPrefix_alwaysTrue() {
            reg.create(TestConstants.NET_EXT, "");
            assertTrue(reg.ipMatches("1.2.3.4", TestConstants.NET_EXT));
        }

        @Test void matchingNetworks_returnsAll() {
            reg.create(TestConstants.NET_STANDARD, "10.");
            reg.create(TestConstants.NET_EXT,      "10.");
            reg.create(TestConstants.NET_FIX,      "192.");
            List<String> matches = reg.matchingNetworks("10.0.0.1");
            assertTrue(matches.contains(TestConstants.NET_STANDARD));
            assertTrue(matches.contains(TestConstants.NET_EXT));
            assertFalse(matches.contains(TestConstants.NET_FIX));
        }

        @Test void ensureDefault_addsStandardWhenEmpty() { reg.ensureDefault(); assertTrue(reg.contains(NetworkRegistry.DEFAULT_CAT)); }

        @Test void ensureDefault_doesNotAddWhenNotEmpty() {
            reg.create(TestConstants.NET_STANDARD, "");
            reg.ensureDefault();
            assertFalse(reg.contains(NetworkRegistry.DEFAULT_CAT));
        }

        @Test void networks_isLive() {
            reg.create(TestConstants.NET_STANDARD, "");
            reg.networks().get(TestConstants.NET_STANDARD).add(new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Lin"));
            assertEquals(1, reg.networks().get(TestConstants.NET_STANDARD).size());
        }
    }

    @Nested
    class LegacyTest {

        @TempDir Path tmp;

        @Test void parsePorts_valid() {
            Map<Integer, String> p = NetworkStoreLegacy.parsePorts("22:SSH,80:HTTP");
            assertEquals("SSH", p.get(22)); assertEquals("HTTP", p.get(80));
        }

        @Test void parsePorts_noLabel_defaultsToOffen() { assertEquals("offen", NetworkStoreLegacy.parsePorts("443").get(443)); }
        @Test void parsePorts_empty_returnsEmpty()      { assertTrue(NetworkStoreLegacy.parsePorts("").isEmpty()); assertTrue(NetworkStoreLegacy.parsePorts(null).isEmpty()); }

        @Test void parsePorts_malformed_ignored() {
            var m = NetworkStoreLegacy.parsePorts("abc:def,22:SSH");
            assertFalse(m.containsKey(0)); assertTrue(m.containsKey(22));
        }

        @Test void loadFile_parsesIpAndHostname() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, TestConstants.IP_1 + ";" + TestConstants.HOST_1 + ";Linux;2024-01-01;22:SSH;note\n", StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, TestConstants.NET_STANDARD, nets, null);
            assertEquals(1, nets.get(TestConstants.NET_STANDARD).size());
            assertEquals(TestConstants.IP_1, nets.get(TestConstants.NET_STANDARD).get(0).ip);
        }

        @Test void loadFile_parsesPrefix() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, "IP-PRÄFIX:" + TestConstants.PREFIX_88 + "\n" + TestConstants.IP_1 + ";" + TestConstants.HOST_1 + ";Win\n", StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            Map<String, String>           pfx  = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, TestConstants.NET_STANDARD, nets, pfx);
            assertEquals(TestConstants.PREFIX_88, pfx.get(TestConstants.NET_STANDARD));
        }

        @Test void loadFile_skipsBlankAndComment() throws Exception {
            Path file = tmp.resolve("hosts.txt");
            Files.writeString(file, "\n# comment\n\n", StandardCharsets.UTF_8);
            Map<String, List<HostResult>> nets = new LinkedHashMap<>();
            NetworkStoreLegacy.loadFile(file, TestConstants.NET_STANDARD, nets, null);
            assertTrue(nets.get(TestConstants.NET_STANDARD).isEmpty());
        }
    }

    @Nested
    class NtfyTest {

        @TempDir Path tmp;

        @Test void saveAndLoad_topic()     { NetworkStoreNtfy.saveTopic(tmp, "my-topic"); assertTrue(NetworkStoreNtfy.loadTopics(tmp).contains("my-topic")); }
        @Test void noDuplicates()          { NetworkStoreNtfy.saveTopic(tmp, "dup"); NetworkStoreNtfy.saveTopic(tmp, "dup"); assertEquals(1, NetworkStoreNtfy.loadTopics(tmp).stream().filter("dup"::equals).count()); }
        @Test void empty_whenNoFile()      { assertTrue(NetworkStoreNtfy.loadTopics(tmp).isEmpty()); }
        @Test void blank_ignored()         { NetworkStoreNtfy.saveTopic(tmp, "  "); assertTrue(NetworkStoreNtfy.loadTopics(tmp).isEmpty()); }

        @Test void sorted_alphabetically() {
            NetworkStoreNtfy.saveTopic(tmp, "zzz");
            NetworkStoreNtfy.saveTopic(tmp, "aaa");
            assertEquals("aaa", NetworkStoreNtfy.loadTopics(tmp).get(0));
        }
    }
}