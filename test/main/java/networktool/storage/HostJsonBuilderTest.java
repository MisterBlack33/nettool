package main.java.networktool.storage;

import main.java.networktool.model.HostResult;
import main.java.networktool.networktool_v3.storage.HostJsonBuilder;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HostJsonBuilder (package-private).
 * All operations are self-contained: no real NetworkStore interaction.
 */
class HostJsonBuilderTest {

    @Test void buildNetworkJson_containsIp() {
        HostResult h = new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux", "2024-01-01",
                Map.of(22, "SSH"), "note");
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, TestConstants.PREFIX_88, List.of(h));
        assertTrue(json.contains(TestConstants.IP_1));
        assertTrue(json.contains(TestConstants.NET_STANDARD));
    }

    @Test void buildNetworkJson_emptyHosts() {
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", List.of());
        assertTrue(json.contains("\"hosts\""));
        assertTrue(json.contains("[]") || json.contains("[\n  ]") || json.contains("[\n]"));
    }

    @Test void buildNetworkJson_multipleHosts() {
        List<HostResult> hosts = List.of(
                new HostResult(TestConstants.IP_1, TestConstants.HOST_1, "Linux"),
                new HostResult(TestConstants.IP_2, TestConstants.HOST_2, "Windows")
        );
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", hosts);
        assertTrue(json.contains(TestConstants.IP_1));
        assertTrue(json.contains(TestConstants.IP_2));
    }

    @Test void buildNetworkJson_specialCharsInNotes_escaped() {
        HostResult h = new HostResult(TestConstants.IP_3, TestConstants.HOST_3, "Lin", "2024",
                new TreeMap<>(), "note with \"quotes\" and\nnewline");
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", List.of(h));
        assertFalse(json.contains("\"quotes\""));
    }

    @Test void parseHost_roundtrip() {
        HostResult original = new HostResult(TestConstants.IP_4, TestConstants.HOST_4, "Win", "2024-06-01",
                Map.of(3389, "RDP"), "my note");
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", List.of(original));

        // Extract the host object from JSON
        int start = json.indexOf('{', json.indexOf("\"hosts\""));
        int end   = json.lastIndexOf('}', json.lastIndexOf('}') - 1) + 1;
        String hostObj = json.substring(start, end);

        HostResult parsed = HostJsonBuilder.parseHost(hostObj);
        assertNotNull(parsed);
        assertEquals(TestConstants.IP_4, parsed.ip);
        assertEquals(TestConstants.HOST_4, parsed.hostname);
        assertEquals("Win", parsed.os);
        assertTrue(parsed.getPorts().containsKey(3389));
        assertEquals("my note", parsed.notes);
    }

    @Test void parseHost_nullIp_returnsNull() {
        assertNull(HostJsonBuilder.parseHost("{\"hostname\":\"h\",\"os\":\"Win\"}"));
    }

    @Test void parseHost_missingOptionalFields_defaults() {
        String obj = "{\"ip\":\"" + TestConstants.IP_5 + "\"}";
        HostResult h = HostJsonBuilder.parseHost(obj);
        assertNotNull(h);
        assertEquals(TestConstants.IP_5, h.ip);
        assertEquals(TestConstants.IP_5, h.hostname);
        assertEquals("", h.os);
        assertEquals("", h.notes);
    }

    @Test void serPortsJson_empty()          { assertEquals("{}", HostJsonBuilder.serPortsJson(new HashMap<>())); }
    @Test void serPortsJson_null_returnsEmptyBraces() { assertEquals("{}", HostJsonBuilder.serPortsJson(null)); }

    @Test void serPortsJson_withPorts() {
        Map<Integer, String> ports = new TreeMap<>();
        ports.put(80, "HTTP");
        ports.put(443, "HTTPS");
        String result = HostJsonBuilder.serPortsJson(ports);
        assertTrue(result.contains("80"));
        assertTrue(result.contains("HTTP"));
        assertTrue(result.contains("443"));
    }
}