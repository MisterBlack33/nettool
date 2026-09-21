package main.java.networktool.logic.scan.host;

import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class NetworkScannerV6Test {

    private static final String NET = "2001:db8::/126";
    private static final String HOST_1 = "2001:db8:0:0:0:0:0:1";
    private static final String HOST_2 = "2001:db8:0:0:0:0:0:2";

    private static NetworkScannerV6 scanner(Predicate<String> alive, Set<String> neighbors) {
        return new NetworkScannerV6(alive, () -> neighbors,
                ip -> new ScanResult(ip, "h-" + ip, Map.of(), "Linux/Unix"));
    }

    @Test void scan_returnsOnlyReachableHosts() {
        List<ScanResult> results = scanner(ip -> ip.equals(HOST_2), Set.of()).scanCidr(NET);
        assertEquals(1, results.size());
        assertEquals(HOST_2, results.get(0).getIp());
    }

    @Test void scan_resultCarriesDescriberData() {
        ScanResult r = scanner(ip -> true, Set.of()).scanCidr(NET).get(0);
        assertEquals("Linux/Unix", r.getOsGuess());
        assertTrue(r.getOpenPorts().isEmpty());
    }

    @Test void scan_includesNeighborsInsideCidr() {
        List<ScanResult> results = scanner(ip -> true, Set.of("2001:db8::abcd")).scanCidr("2001:db8::/64");
        assertTrue(results.stream().anyMatch(r -> r.getIp().equals("2001:db8:0:0:0:0:0:abcd")));
    }

    @Test void scan_ignoresNeighborsOutsideCidr() {
        List<ScanResult> results = scanner(ip -> true, Set.of("2001:db9::1")).scanCidr(NET);
        assertEquals(3, results.size());
    }

    @Test void scan_neighborAlreadyInRange_notDuplicated() {
        assertEquals(3, scanner(ip -> true, Set.of("2001:db8::2")).scanCidr(NET).size());
    }

    @Test void scan_invalidCidr_returnsEmpty() {
        assertTrue(scanner(ip -> true, Set.of()).scanCidr("not-a-cidr").isEmpty());
    }

    @Test void scan_resultsSortedByIp() {
        List<String> ips = scanner(ip -> true, Set.of()).scanCidr(NET).stream().map(ScanResult::getIp).toList();
        assertEquals(ips.stream().sorted().toList(), ips);
    }

    @Test void scan_failingProbe_doesNotAbortOthers() {
        NetworkScannerV6 scanner = new NetworkScannerV6(ip -> true, Set::of, ip -> {
            if (ip.equals(HOST_1)) throw new IllegalStateException("boom");
            return new ScanResult(ip, "h", Map.of(), "x");
        });
        assertEquals(2, scanner.scanCidr(NET).size());
    }

    @Test void scan_resultIsImmutable() {
        List<ScanResult> results = scanner(ip -> true, Set.of()).scanCidr(NET);
        assertThrows(UnsupportedOperationException.class, () -> results.add(null));
    }

    @Test void withDefaults_createsScanner() { assertNotNull(NetworkScannerV6.withDefaults()); }
}
