package main.java.networktool.logic.scan.host;

import main.java.networktool.model.HostResult;
import main.java.networktool.util.CIDRUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestriert den lokalen Netzwerk-Scan.
 * Hostname-Auflösung: {@link NetworkHostnameResolver}. ARP-Cache/-Parsing: {@link NetworkHostArpResolver}.
 * Scan-Ausführung + Ergebnis-Zusammenführung: {@link NetworkHostScanOrchestrator}.
 */
public final class NetworkHostScanner {

    private NetworkHostScanner() {}

    /** Scannt /24-Präfixe — nutzt ARP-Cache + ICMP. */
    public static List<HostResult> scan(List<String> subnets) {
        List<String> allIps = expandSubnets(subnets);
        // Vor dem ARP-Read: zusätzliche Discovery, damit stille WLAN-Geräte
        // im ARP-Cache erscheinen bzw. separat als erreichbar zählen.
        Set<String> discovered = NetworkDiscoverySweep.discover(allIps);
        HostAliveChecker.warmCache();
        Map<String, String> arpHosts = NetworkHostArpResolver.readArpCache(subnets);
        List<String> ips = mergeIps(allIps, union(arpHosts.keySet(), discovered));
        return NetworkHostScanOrchestrator.scanIpList(ips, arpHosts, discovered);
    }

    public static List<HostResult> scanCidr(String cidr) {
        HostAliveChecker.warmCache();
        List<String> ips = CIDRUtils.getAllIPs(cidr);
        Set<String> discovered = NetworkDiscoverySweep.discover(ips);
        return NetworkHostScanOrchestrator.scanIpList(ips, java.util.Collections.emptyMap(), discovered);
    }

    /** Package-private Testzugriff auf die ARP-Auflösung, siehe NetworkHostScannerPackageTest. */
    static String readMacFromArp(String ip) {
        return NetworkHostArpResolver.readMacFromArp(ip);
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> merged = new LinkedHashSet<>(a);
        merged.addAll(b);
        return merged;
    }

    private static List<String> expandSubnets(List<String> subnets) {
        List<String> ips = new ArrayList<>(subnets.size() * 254);
        for (String subnet : subnets)
            for (int i = 1; i < 255; i++)
                ips.add(subnet + "." + i);
        return ips;
    }

    /** Vereinigt ICMP-Scan-IPs und ARP-IPs ohne Duplikate. */
    private static List<String> mergeIps(List<String> scanIps, Set<String> arpIps) {
        Set<String> merged = new LinkedHashSet<>(scanIps);
        merged.addAll(arpIps);
        return new ArrayList<>(merged);
    }
}