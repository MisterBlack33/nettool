package main.java.networktool.model;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ModelEdgeCaseTest {
    @Test void hostResult_emptyPortsMap_portsToStringEmpty() {
        HostResult h = new HostResult("1.1.1.1", "h", "Linux", null, new HashMap<>());
        assertEquals("", h.portsToString()); assertTrue(h.getPorts().isEmpty());
    }
    @Test void hostResult_fourArgConstructor_savedAtNull() {
        HostResult h = new HostResult("1.1.1.1", "h", "Win", "2024-01-01");
        assertEquals("2024-01-01", h.savedAt); assertEquals("", h.notes);
        assertTrue(h.getPorts().isEmpty());
    }
    @Test void hostResult_fiveArgConstructor_defaultsNotesEmpty() {
        HostResult h = new HostResult("1.1.1.1", "h", "Win", null, Map.of(80, "HTTP"));
        assertEquals("", h.notes); assertEquals("HTTP", h.getPorts().get(80));
    }
    @Test void scanResult_emptyHostname_allowed() {
        assertEquals("", new ScanResult("1.1.1.1", "", new HashMap<>(), "Win").getHostname());
    }
    @Test void scanProfile_lastRun_defaultsEmpty() { assertEquals("", new ScanProfile("p").lastRun); }
    @Test void scanProfile_ports_defaultsEmptyList() { assertTrue(new ScanProfile("p").ports.isEmpty()); }
    @Test void scanProfile_summary_withOnlyHnFilter_noOsFilter() {
        ScanProfile p = new ScanProfile("only-hn"); p.hnFilter = "server";
        assertTrue(p.summary().contains("HN:server")); assertFalse(p.summary().contains("OS:"));
    }
    @Test void scanProfile_category_defaultsEmpty() { assertEquals("", new ScanProfile("p").category); }
}
