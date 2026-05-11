package networktool_v3.model;

import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class HostResultTest {

    @Test
    void fullConstructorSetsAllFields() {
        Map<Integer,String> ports = Map.of(80, "HTTP", 22, "SSH");
        HostResult h = new HostResult("192.168.1.1", "host.local", "Linux/Unix",
                "2026-01-01 10:00:00", ports, "Test-Notiz");
        assertEquals("192.168.1.1",         h.ip);
        assertEquals("host.local",           h.hostname);
        assertEquals("Linux/Unix",           h.os);
        assertEquals("2026-01-01 10:00:00",  h.savedAt);
        assertEquals("Test-Notiz",           h.notes);
        assertTrue(h.ports.containsKey(80));
        assertTrue(h.ports.containsKey(22));
    }

    @Test
    void nullPortsBecomesEmptyMap() {
        HostResult h = new HostResult("1.2.3.4", "host", "Windows", null, null, "");
        assertNotNull(h.ports);
        assertTrue(h.ports.isEmpty());
    }

    @Test
    void nullNotesBecomesEmptyString() {
        HostResult h = new HostResult("1.2.3.4", "host", "Windows");
        assertEquals("", h.notes);
    }

    @Test
    void portsToString_nonEmpty() {
        Map<Integer,String> ports = new TreeMap<>(Map.of(22, "SSH", 80, "HTTP"));
        HostResult h = new HostResult("1.2.3.4", "x", "Linux", null, ports, "");
        String s = h.portsToString();
        assertTrue(s.contains("22"));
        assertTrue(s.contains("80"));
    }

    @Test
    void portsToString_empty() {
        HostResult h = new HostResult("1.2.3.4", "x", "Linux");
        assertEquals("", h.portsToString());
    }

    @Test
    void getPortsIsUnmodifiable() {
        HostResult h = new HostResult("1.2.3.4", "x", "Linux", null,
                Map.of(80, "HTTP"), "");
        assertThrows(UnsupportedOperationException.class,
                () -> h.getPorts().put(443, "HTTPS"));
    }

    @Test
    void threeArgConstructorDefaults() {
        HostResult h = new HostResult("1.2.3.4", "x", "Linux");
        assertNull(h.savedAt);
        assertNotNull(h.ports);
        assertEquals("", h.notes);
    }
}

class ScanResultTest {

    @Test
    void gettersReturnCorrectValues() {
        Map<Integer,String> ports = Map.of(443, "HTTPS");
        ScanResult r = new ScanResult("10.0.0.1", "server.local", ports, "Linux/Unix");
        assertEquals("10.0.0.1",    r.getIp());
        assertEquals("server.local", r.getHostname());
        assertEquals("Linux/Unix",   r.getOsGuess());
        assertTrue(r.getOpenPorts().containsKey(443));
    }

    @Test
    void emptyPortsMap() {
        ScanResult r = new ScanResult("1.2.3.4", "h", Map.of(), "Windows");
        assertTrue(r.getOpenPorts().isEmpty());
    }
}
