package main.java.networktool_v3.storage;

import main.java.networktool_v3.model.HostResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HostJsonBuilder (package-private class).
 * Must be in package main.java.networktool_v3.storage for access.
 */
class HostJsonBuilderTest {

    @Test
    void buildNetworkJson_containsIp() {
        HostResult h = new HostResult("1.2.3.4", "host", "Linux", "2024-01-01",
                Map.of(22, "SSH"), "note");
        String json = HostJsonBuilder.buildNetworkJson("mynet", "1.2.3.", List.of(h));
        assertTrue(json.contains("1.2.3.4"));
        assertTrue(json.contains("mynet"));
    }

    @Test
    void buildNetworkJson_emptyHosts() {
        String json = HostJsonBuilder.buildNetworkJson("empty", "", List.of());
        assertTrue(json.contains("\"hosts\""));
        assertTrue(json.contains("[]") || json.contains("[\n  ]") || json.contains("[\n]"));
    }

    @Test
    void buildNetworkJson_multipleHosts() {
        List<HostResult> hosts = List.of(
                new HostResult("1.1.1.1", "h1", "Linux"),
                new HostResult("1.1.1.2", "h2", "Windows")
        );
        String json = HostJsonBuilder.buildNetworkJson("multi", "", hosts);
        assertTrue(json.contains("1.1.1.1"));
        assertTrue(json.contains("1.1.1.2"));
    }

    @Test
    void buildNetworkJson_specialCharsInNotes_escaped() {
        HostResult h = new HostResult("1.2.3.4", "h", "Lin", "2024",
                new TreeMap<>(), "note with \"quotes\" and\nnewline");
        String json = HostJsonBuilder.buildNetworkJson("net", "", List.of(h));
        assertFalse(json.contains("\"quotes\""));
    }

    @Test
    void parseHost_roundtrip() {
        HostResult original = new HostResult("5.6.7.8", "server", "Win", "2024-06-01",
                Map.of(3389, "RDP"), "my note");
        String json = HostJsonBuilder.buildNetworkJson("net", "", List.of(original));

        int start = json.indexOf('{', json.indexOf("\"hosts\""));
        int end   = json.lastIndexOf('}', json.lastIndexOf('}') - 1) + 1;
        String hostObj = json.substring(start, end);

        HostResult parsed = HostJsonBuilder.parseHost(hostObj);
        assertNotNull(parsed);
        assertEquals("5.6.7.8", parsed.ip);
        assertEquals("server", parsed.hostname);
        assertEquals("Win", parsed.os);
        assertTrue(parsed.getPorts().containsKey(3389));
        assertEquals("my note", parsed.notes);
    }

    @Test
    void parseHost_nullIp_returnsNull() {
        assertNull(HostJsonBuilder.parseHost("{\"hostname\":\"h\",\"os\":\"Win\"}"));
    }

    @Test
    void parseHost_missingOptionalFields_defaults() {
        String obj = "{\"ip\":\"9.9.9.9\"}";
        HostResult h = HostJsonBuilder.parseHost(obj);
        assertNotNull(h);
        assertEquals("9.9.9.9", h.ip);
        assertEquals("9.9.9.9", h.hostname);
        assertEquals("", h.os);
        assertEquals("", h.notes);
    }

    @Test
    void serPortsJson_empty() {
        assertEquals("{}", HostJsonBuilder.serPortsJson(new HashMap<>()));
    }

    @Test
    void serPortsJson_null_returnsEmptyBraces() {
        assertEquals("{}", HostJsonBuilder.serPortsJson(null));
    }

    @Test
    void serPortsJson_withPorts() {
        Map<Integer, String> ports = new TreeMap<>();
        ports.put(80, "HTTP");
        ports.put(443, "HTTPS");
        String result = HostJsonBuilder.serPortsJson(ports);
        assertTrue(result.contains("80"));
        assertTrue(result.contains("HTTP"));
        assertTrue(result.contains("443"));
    }
}
