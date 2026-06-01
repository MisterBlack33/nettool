package main.java.networktool.gui;

import main.java.networktool.logic.analysis.TracerouteRunner;
import main.java.networktool.logic.scan.RemoteNetScanner;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import java.util.*;
import java.util.concurrent.*;

/**
 * Ermittelt Netzwerk-Zwischenknoten per Traceroute (max. 4 Hops).
 * Ergebnis: Host-IP → direkter Upstream-Knoten (Switch oder Gateway).
 */
final class MapHopDiscovery {

    private MapHopDiscovery() {}

    private static final int MAX_HOPS    = 4;
    private static final int MAX_THREADS = 20;
    private static final int TIMEOUT_SEC = 30;

    /**
     * Führt parallelen Traceroute für alle gespeicherten Hosts durch.
     * Gibt Map zurück: Host-IP → IP des direkten Upstream-Knotens.
     * Hosts die direkt am Gateway hängen werden nicht eingetragen.
     */
    static Map<String, String> discover() {
        String gatewayIp = RemoteNetScanner.detectDefaultGateway();
        List<HostResult> hosts =
                NetworkStore.getInstance().getAllHosts();

        if (hosts.isEmpty()) return Collections.emptyMap();

        Map<String, String> hopParent = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(hosts.size(), MAX_THREADS));

        for (HostResult host : hosts) {
            executor.submit(() -> discoverHost(host.ip, gatewayIp, hopParent));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Collections.unmodifiableMap(hopParent);
    }

    private static void discoverHost(String hostIp, String gatewayIp,
                                     Map<String, String> result) {
        try {
            List<TracerouteRunner.HopInfo> hops = TracerouteRunner.run(hostIp, MAX_HOPS);
            String upstream = findUpstream(hops, hostIp, gatewayIp);
            if (upstream != null) result.put(hostIp, upstream);
        } catch (Exception ignored) {}
    }

    /**
     * Gibt den letzten Nicht-Timeout-Hop vor dem Ziel zurück.
     * Null wenn kein Zwischenknoten vorhanden oder direkt am Gateway.
     */
    static String findUpstream(List<TracerouteRunner.HopInfo> hops,
                               String targetIp, String gatewayIp) {
        if (hops.size() < 2) return null;
        for (int i = hops.size() - 1; i >= 0; i--) {
            TracerouteRunner.HopInfo hop = hops.get(i);
            if (hop.timeout || hop.ip == null || hop.ip.isEmpty()) continue;
            if (hop.ip.equals(targetIp) || hop.ip.equals(gatewayIp)) continue;
            return hop.ip;
        }
        return null;
    }
}