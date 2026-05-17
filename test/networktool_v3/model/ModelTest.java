package networktool_v3.model;

import main.java.networktool_v3.model.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HostResult, ScanResult, ScanProfile.
 */
class ModelTest {

    // ══════════════════════════════════════════════════════════════
    //  HostResult
    // ══════════════════════════════════════════════════════════════

    @Nested
    class HostResultTest {

        @Test
        void constructor_full() {
            Map<Integer, String> ports = Map.of(22, "SSH");
            HostResult h = new HostResult("1.2.3.4", "host", "Linux", "2024-01", ports, "note");
            assertEquals("1.2.3.4", h.ip);
            assertEquals("host", h.hostname);
            assertEquals("Linux", h.os);
            assertEquals("2024-01", h.savedAt);
            assertEquals("SSH", h.getPorts().get(22));
            assertEquals("note", h.notes);
        }

        @Test
        void constructor_nullPorts_emptyMap() {
            HostResult h = new HostResult("1.1.1.1", "h", "Win", null, null, null);
            assertNotNull(h.getPorts());
            assertTrue(h.getPorts().isEmpty());
        }

        @Test
        void constructor_nullNotes_emptyString() {
            HostResult h = new HostResult("1.1.1.1", "h", "Win", null, null, null);
            assertEquals("", h.notes);
        }

        @Test
        void getPorts_isUnmodifiable() {
            HostResult h = new HostResult("1.1.1.1", "h", "Win");
            assertThrows(UnsupportedOperationException.class,
                    () -> h.getPorts().put(80, "HTTP"));
        }

        @Test
        void portsToString_withPorts() {
            Map<Integer, String> ports = new TreeMap<>();
            ports.put(22, "SSH");
            ports.put(80, "HTTP");
            HostResult h = new HostResult("1.1.1.1", "h", "Linux", null, ports);
            String s = h.portsToString();
            assertTrue(s.contains("22"));
            assertTrue(s.contains("80"));
        }

        @Test
        void portsToString_empty() {
            HostResult h = new HostResult("1.1.1.1", "h", "Win");
            assertEquals("", h.portsToString());
        }

        @Test
        void threeParamConstructor() {
            HostResult h = new HostResult("5.5.5.5", "myhost", "macOS");
            assertEquals("5.5.5.5", h.ip);
            assertNull(h.savedAt);
        }

        @Test
        void portsCopied_notShared() {
            Map<Integer, String> ports = new TreeMap<>();
            ports.put(443, "HTTPS");
            HostResult h = new HostResult("1.1.1.1", "h", "Win", null, ports);
            ports.put(8080, "HTTP-Alt");
            // original map change should not affect HostResult
            assertFalse(h.getPorts().containsKey(8080));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanResult
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanResultTest {

        @Test
        void getters_returnCorrectValues() {
            Map<Integer, String> ports = Map.of(80, "HTTP");
            ScanResult r = new ScanResult("10.0.0.1", "server", ports, "Linux");
            assertEquals("10.0.0.1", r.getIp());
            assertEquals("server", r.getHostname());
            assertEquals("Linux", r.getOsGuess());
            assertEquals("HTTP", r.getOpenPorts().get(80));
        }

        @Test
        void emptyPorts() {
            ScanResult r = new ScanResult("1.1.1.1", "h", new HashMap<>(), "Win");
            assertTrue(r.getOpenPorts().isEmpty());
        }

        @Test
        void nullHostname() {
            ScanResult r = new ScanResult("1.1.1.1", null, new HashMap<>(), "");
            assertNull(r.getHostname());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanProfile
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanProfileTest {

        @Test
        void defaults() {
            ScanProfile p = new ScanProfile("test");
            assertEquals("test", p.name);
            assertTrue(p.cidrs.isEmpty());
            assertEquals("", p.osFilter);
            assertFalse(p.autoSave);
        }

        @Test
        void summary_local() {
            ScanProfile p = new ScanProfile("home");
            String s = p.summary();
            assertTrue(s.contains("home"));
            assertTrue(s.contains("lokal"));
        }

        @Test
        void summary_withCidr() {
            ScanProfile p = new ScanProfile("office");
            p.cidrs.add("192.168.2.0/24");
            assertTrue(p.summary().contains("192.168.2.0/24"));
        }

        @Test
        void summary_withFilters() {
            ScanProfile p = new ScanProfile("filter-test");
            p.osFilter = "Linux";
            p.hnFilter = "server";
            String s = p.summary();
            assertTrue(s.contains("Linux"));
            assertTrue(s.contains("server"));
        }
    }
}
