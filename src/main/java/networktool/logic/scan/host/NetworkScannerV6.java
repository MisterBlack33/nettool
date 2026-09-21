package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.model.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * IPv6-Scan eines CIDR. Kandidaten sind die ersten {@link Ipv6HostRange#MAX_HOSTS}
 * Adressen des Netzes plus alle Nachbarn aus dem Neighbor-Cache innerhalb des CIDR
 * (SLAAC-Adressen sind per Durchzählen nicht auffindbar).
 */
public final class NetworkScannerV6 {

    private static final int MAX_THREADS = 32;
    private static final long SCAN_TIMEOUT_MINUTES = 5;

    private final Predicate<String> reachability;
    private final Supplier<Set<String>> neighbors;
    private final Function<String, ScanResult> describer;

    NetworkScannerV6(Predicate<String> reachability, Supplier<Set<String>> neighbors,
                     Function<String, ScanResult> describer) {
        this.reachability = reachability;
        this.neighbors = neighbors;
        this.describer = describer;
    }

    public static NetworkScannerV6 withDefaults() {
        return new NetworkScannerV6(Ipv6HostProbe::isAlive, Ipv6NeighborSource::read, Ipv6HostProbe::describe);
    }

    public List<ScanResult> scanCidr(String cidr) {
        List<String> candidates;
        try {
            candidates = candidates(cidr);
        } catch (IllegalArgumentException e) {
            DebugLogger.getInstance().log("WARN", "[NetworkScannerV6] " + e.getMessage());
            return List.of();
        }
        List<ScanResult> results = Collections.synchronizedList(new ArrayList<>());
        probeAll(candidates, results);
        List<ScanResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing(ScanResult::getIp));
        return List.copyOf(sorted);
    }

    private List<String> candidates(String cidr) {
        Set<String> all = new LinkedHashSet<>(Ipv6HostRange.expand(cidr));
        for (String neighbor : neighbors.get()) {
            if (Ipv6HostRange.contains(cidr, neighbor)) all.add(Ipv6HostRange.canonical(neighbor));
        }
        return List.copyOf(all);
    }

    private void probeAll(List<String> candidates, List<ScanResult> results) {
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(1, Math.min(MAX_THREADS, candidates.size())), r -> {
                    Thread thread = new Thread(r, "NetworkScannerV6");
                    thread.setDaemon(true);
                    return thread;
                });
        candidates.forEach(ip -> executor.submit(() -> probe(ip, results)));
        executor.shutdown();
        try {
            executor.awaitTermination(SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void probe(String ip, List<ScanResult> results) {
        try {
            if (reachability.test(ip)) results.add(describer.apply(ip));
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("FINE", "[NetworkScannerV6] Probe fehlgeschlagen (" + ip + "): " + e);
        }
    }
}
