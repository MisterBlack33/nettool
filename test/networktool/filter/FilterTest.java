package networktool.filter;

import networktool.filter.HostResultFilter;
import networktool.filter.ScanFilter;
import networktool.model.HostResult;
import networktool.model.ScanResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HostResultFilter and ScanFilter.
 */
class FilterTest {

    // ══════════════════════════════════════════════════════════════
    //  HostResultFilter
    // ══════════════════════════════════════════════════════════════

    @Nested
    class HostResultFilterTest {

        List<HostResult> hosts = List.of(
                new HostResult("1.1.1.1", "desktop-win [AA:BB:CC]", "Windows", null),
                new HostResult("1.1.1.2", "raspberrypi", "Raspberry Pi (Linux)", null),
                new HostResult("1.1.1.3", "1.1.1.3", "Android", null),
                new HostResult("1.1.1.4", "macbook-pro", "macOS", null),
                new HostResult("1.1.1.5", "linuxserver", "Linux/Unix", null)
        );

        @Test
        void noFilter_returnsAll() {
            assertEquals(5, HostResultFilter.filter(hosts, null, null).size());
        }

        @Test
        void osFilter_windows() {
            List<HostResult> res = HostResultFilter.filter(hosts, "windows", null);
            assertEquals(1, res.size());
            assertEquals("1.1.1.1", res.get(0).ip);
        }

        @Test
        void osFilter_linux_matchesMultiple() {
            List<HostResult> res = HostResultFilter.filter(hosts, "linux", null);
            assertEquals(2, res.size());
        }

        @Test
        void osFilter_caseInsensitive() {
            List<HostResult> res = HostResultFilter.filter(hosts, "ANDROID", null);
            assertEquals(1, res.size());
        }

        @Test
        void hostnameFilter_matchesPartial() {
            List<HostResult> res = HostResultFilter.filter(hosts, null, "server");
            assertEquals(1, res.size());
            assertEquals("1.1.1.5", res.get(0).ip);
        }

        @Test
        void hostnameFilter_excludesRawIp() {
            // host with hostname == ip should not match anything
            List<HostResult> res = HostResultFilter.filter(hosts, null, "1.1.1.3");
            assertTrue(res.isEmpty());
        }

        @Test
        void combined_filter() {
            List<HostResult> res = HostResultFilter.filter(hosts, "linux", "raspberry");
            assertEquals(1, res.size());
        }

        @Test
        void emptyList() {
            assertTrue(HostResultFilter.filter(Collections.emptyList(), "win", null).isEmpty());
        }

        @Test
        void buildLabel_noFilters() {
            assertEquals("Alle Geräte", HostResultFilter.buildLabel(null, null));
        }

        @Test
        void buildLabel_osOnly() {
            String label = HostResultFilter.buildLabel("Linux", null);
            assertTrue(label.contains("Linux"));
        }

        @Test
        void buildLabel_both() {
            String label = HostResultFilter.buildLabel("Win", "server");
            assertTrue(label.contains("Win"));
            assertTrue(label.contains("server"));
        }

        @Test
        void hostnameFilter_stripsMacPart() {
            // desktop-win [AA:BB:CC] - hostname has MAC in brackets
            List<HostResult> res = HostResultFilter.filter(hosts, null, "desktop");
            assertEquals(1, res.size());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ScanFilter
    // ══════════════════════════════════════════════════════════════

    @Nested
    class ScanFilterTest {

        List<ScanResult> results = buildResults();

        private List<ScanResult> buildResults() {
            Map<Integer, String> winPorts = Map.of(445, "SMB", 3389, "RDP");
            Map<Integer, String> linPorts = Map.of(22, "SSH", 80, "HTTP");
            Map<Integer, String> empty = new HashMap<>();
            return List.of(
                    new ScanResult("10.0.0.1", "win-server", winPorts, "Windows"),
                    new ScanResult("10.0.0.2", "linux-box", linPorts, "Linux/Unix"),
                    new ScanResult("10.0.0.3", "android-phone", empty, "Android"),
                    new ScanResult("10.0.0.4", "web-server", linPorts, "Web-Server (nginx)"),
                    new ScanResult("10.0.0.5", "macbook", Map.of(548, "AFP"), "macOS")
            );
        }

        @Test
        void filterByOS_windows() {
            List<ScanResult> r = ScanFilter.filterByOS(results, "windows");
            assertEquals(1, r.size());
            assertEquals("10.0.0.1", r.get(0).getIp());
        }

        @Test
        void filterByOS_caseInsensitive() {
            assertEquals(1, ScanFilter.filterByOS(results, "ANDROID").size());
        }

        @Test
        void filterByOS_noMatch() {
            assertTrue(ScanFilter.filterByOS(results, "RouterOS").isEmpty());
        }

        @Test
        void filterByPort_ssh() {
            List<ScanResult> r = ScanFilter.filterByPort(results, 22);
            assertEquals(2, r.size());
        }

        @Test
        void filterByPort_notPresent() {
            assertTrue(ScanFilter.filterByPort(results, 9999).isEmpty());
        }

        @Test
        void filterByHostnameRegex_simple() {
            List<ScanResult> r = ScanFilter.filterByHostnameRegex(results, "server");
            assertEquals(2, r.size());
        }

        @Test
        void filterByHostnameRegex_caseInsensitive() {
            List<ScanResult> r = ScanFilter.filterByHostnameRegex(results, "LINUX");
            assertEquals(1, r.size());
        }

        @Test
        void filterByHostnameRegex_noMatch() {
            assertTrue(ScanFilter.filterByHostnameRegex(results, "zzzzzz").isEmpty());
        }

        @Test
        void filterCombined_linuxSsh() {
            List<ScanResult> r = ScanFilter.filterCombined(results, "linux", 22);
            assertEquals(1, r.size());
            assertEquals("10.0.0.2", r.get(0).getIp());
        }

        @Test
        void filterCombined_noMatch() {
            assertTrue(ScanFilter.filterCombined(results, "windows", 22).isEmpty());
        }
    }
}
