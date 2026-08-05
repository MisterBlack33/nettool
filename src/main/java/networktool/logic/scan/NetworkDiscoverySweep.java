package main.java.networktool.logic.scan;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.analysis.MdnsDiscovery;
import main.java.networktool.logic.analysis.UpnpDiscovery;
import main.java.networktool.logic.TimeoutConfig;

import java.util.*;
import java.util.concurrent.*;

/**
 * Zusätzliche Erkennungsquellen für den lokalen Netzscan.
 *
 * ICMP/TCP allein übersieht viele WLAN-/IoT-Geräte (Energiesparmodus,
 * Firewall). Diese Klasse ergänzt daher:
 *  - PingSweep       → zweiter ICMP-Versuch, triggert ARP-Auflösung
 *  - mDNS/Bonjour     → Handys, Drucker, Chromecasts
 *  - SSDP/UPnP        → Smart-TVs, Router, Konsolen
 */
final class NetworkDiscoverySweep {


    private NetworkDiscoverySweep() {}

    /** Liefert die Vereinigungsmenge aller in {@code candidateIps} gefundenen IPs. */
    static Set<String> discover(List<String> candidateIps) {
        if (candidateIps.isEmpty()) return Set.of();

        Set<String> found = ConcurrentHashMap.newKeySet();
        ExecutorService exec = Executors.newFixedThreadPool(3,
                r -> { Thread t = new Thread(r, "DiscoverySweep"); t.setDaemon(true); return t; });

        exec.submit(() -> found.addAll(PingSweep.sweep(candidateIps, null)));
        exec.submit(() -> collect(found, () -> MdnsDiscovery.discover().stream().map(MdnsDiscovery.ServiceRecord::ip)));
        exec.submit(() -> collect(found, () -> UpnpDiscovery.discover().stream().map(UpnpDiscovery.Device::ip)));

        exec.shutdown();
        try { exec.awaitTermination(TimeoutConfig.DISCOVERY_SWEEP_SEC, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Set<String> allowed = new HashSet<>(candidateIps);
        found.retainAll(allowed);
        return found;
    }

    private static void collect(Set<String> found, java.util.function.Supplier<java.util.stream.Stream<String>> src) {
        try { src.get().forEach(found::add); }
        catch (Exception e) { DebugLogger.getInstance().log("FINE", "[NetworkDiscoverySweep] Discovery-Quelle fehlgeschlagen: " + e); }
    }
}