package networktool_v3.filter;

import networktool_v3.model.HostResult;
import networktool_v3.model.ScanResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HostResultFilterTest {

    private List<HostResult> hosts;

    @BeforeEach
    void setUp() {
        hosts = List.of(
            new HostResult("192.168.1.1",  "router.local",   "Linux/Unix",  null),
            new HostResult("192.168.1.10", "win-pc [AA:BB]", "Windows",     null),
            new HostResult("192.168.1.20", "android-phone",  "Android",     null),
            new HostResult("192.168.1.30", "macbook.local",  "macOS",       null)
        );
    }

    @Test
    void filterByOs_windows() {
        var result = HostResultFilter.filter(hosts, "Windows", "");
        assertEquals(1, result.size());
        assertEquals("192.168.1.10", result.get(0).ip);
    }

    @Test
    void filterByOs_caseInsensitive() {
        var result = HostResultFilter.filter(hosts, "windows", "");
        assertEquals(1, result.size());
    }

    @Test
    void filterByHostname_partial() {
        var result = HostResultFilter.filter(hosts, "", "router");
        assertEquals(1, result.size());
        assertEquals("192.168.1.1", result.get(0).ip);
    }

    @Test
    void filterAll_emptyFilters() {
        var result = HostResultFilter.filter(hosts, "", "");
        assertEquals(4, result.size());
    }

    @Test
    void filterAll_alleFilter() {
        var result = HostResultFilter.filter(hosts, "alle", "alle");
        assertEquals(4, result.size());
    }

    @Test
    void filterCombined_osAndHostname() {
        var result = HostResultFilter.filter(hosts, "Linux", "router");
        assertEquals(1, result.size());
    }

    @Test
    void filterCombined_noMatch() {
        var result = HostResultFilter.filter(hosts, "Windows", "router");
        assertTrue(result.isEmpty());
    }

    @Test
    void buildLabel_noFilters() {
        assertEquals("Alle Geräte", HostResultFilter.buildLabel("", ""));
    }

    @Test
    void buildLabel_withOs() {
        String label = HostResultFilter.buildLabel("Windows", "");
        assertTrue(label.contains("Windows"));
    }

    @Test
    void buildLabel_withBoth() {
        String label = HostResultFilter.buildLabel("Linux", "router");
        assertTrue(label.contains("Linux") && label.contains("router"));
    }
}

class ScanFilterTest {

    private List<ScanResult> results;

    @BeforeEach
    void setUp() {
        Map<Integer,String> portsWeb  = Map.of(80, "HTTP", 443, "HTTPS");
        Map<Integer,String> portsSSH  = Map.of(22, "SSH");
        Map<Integer,String> portsBoth = Map.of(22, "SSH", 80, "HTTP");
        results = List.of(
            new ScanResult("10.0.0.1", "web-server",   portsWeb,  "Linux/Unix"),
            new ScanResult("10.0.0.2", "ssh-server",   portsSSH,  "Linux/Unix"),
            new ScanResult("10.0.0.3", "win-machine",  portsBoth, "Windows"),
            new ScanResult("10.0.0.4", "empty-host",   Map.of(),  "Android")
        );
    }

    @Test
    void filterByOS_linux() {
        var r = ScanFilter.filterByOS(results, "linux");
        assertEquals(2, r.size());
    }

    @Test
    void filterByOS_windows() {
        var r = ScanFilter.filterByOS(results, "Windows");
        assertEquals(1, r.size());
    }

    @Test
    void filterByPort_80() {
        var r = ScanFilter.filterByPort(results, 80);
        assertEquals(2, r.size());
    }

    @Test
    void filterByPort_22() {
        var r = ScanFilter.filterByPort(results, 22);
        assertEquals(2, r.size());
    }

    @Test
    void filterByHostnameRegex() {
        var r = ScanFilter.filterByHostnameRegex(results, ".*server.*");
        assertEquals(2, r.size());
    }

    @Test
    void filterByHostnameRegex_caseInsensitive() {
        var r = ScanFilter.filterByHostnameRegex(results, ".*SERVER.*");
        assertEquals(2, r.size());
    }

    @Test
    void filterCombined_linuxPort80() {
        var r = ScanFilter.filterCombined(results, "linux", 80);
        assertEquals(1, r.size());
        assertEquals("10.0.0.1", r.get(0).getIp());
    }

    @Test
    void filterCombined_noMatch() {
        var r = ScanFilter.filterCombined(results, "Android", 80);
        assertTrue(r.isEmpty());
    }
}
