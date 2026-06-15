package main.java.networktool.gui;

import main.java.networktool.logic.analysis.TracerouteRunner;
import main.java.networktool.logic.scan.RemoteNetScanner;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.NetworkStore;

import java.util.*;
import java.util.concurrent.*;

/**
 * Ermittelt Netzwerk-Zwischenknoten per Traceroute (max. 5 Hops).
 *
 * Ergebnis: Host-IP → direkter Upstream-Knoten.
 * Alle entdeckten Zwischenknoten werden in HOP_PARENT persistent gespeichert.
 *
 * Verbesserungen:
 *  - Zwischenknoten (nicht nur direkte Upstream) werden als Switch-Kandidaten erkannt
 *  - IPs die mehrfach als Hop-Parent auftauchen → automatisch als Switch markiert
 *  - Timeout-Hops werden übersprungen, aber die letzte bekannte IP davor gilt als Upstream
 */
final class MapHopDiscovery {

    private MapHopDiscovery() {}

    private static final int MAX_HOPS    = 5;
    private static final int MAX_THREADS = 20;
    private static final int TIMEOUT_SEC = 30;

    // IP → wie oft als Hop-Zwischenknoten gesehen
    private static final Map<String, Integer> hopFrequency = new ConcurrentHashMap<>();

    // Schwelle: ab dieser Häufigkeit wird ein Hop automatisch als Switch markiert
    private static final int SWITCH_PROMOTE_THRESHOLD = 3;

    // ── Public API ────────────────────────────────────────────────────────

    static Map<String, String> discover() {
        String gatewayIp = RemoteNetScanner.detectDefaultGateway();
        List<HostResult> hosts = NetworkStore.getInstance().getAllHosts();
        if (hosts.isEmpty()) return Collections.emptyMap();

        hopFrequency.clear();

        Map<String, String> hopParent = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), MAX_THREADS));

        for (HostResult host : hosts)
            executor.submit(() -> discoverHost(host.ip, gatewayIp, hopParent));

        executor.shutdown();
        try { executor.awaitTermination(TIMEOUT_SEC, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        promoteFrequentHopsToSwitches();
        return Collections.unmodifiableMap(hopParent);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static void discoverHost(String hostIp, String gatewayIp,
                                     Map<String, String> result) {
        try {
            List<TracerouteRunner.HopInfo> hops = TracerouteRunner.run(hostIp, MAX_HOPS);
            if (hops.isEmpty()) return;

            recordAllIntermediateHops(hops, hostIp, gatewayIp);

            String upstream = findUpstream(hops, hostIp, gatewayIp);
            if (upstream != null) result.put(hostIp, upstream);

        } catch (Exception ignored) {}
    }

    /**
     * Zählt wie oft eine IP als Zwischenknoten (nicht Ziel, nicht Gateway) auftaucht.
     * Häufige Zwischenknoten sind mit hoher Wahrscheinlichkeit Switches.
     */
    private static void recordAllIntermediateHops(List<TracerouteRunner.HopInfo> hops,
                                                  String targetIp, String gatewayIp) {
        for (TracerouteRunner.HopInfo hop : hops) {
            if (hop.timeout || hop.ip == null || hop.ip.isBlank()) continue;
            if (hop.ip.equals(targetIp) || hop.ip.equals(gatewayIp)) continue;
            hopFrequency.merge(hop.ip, 1, Integer::sum);
        }
    }

    /**
     * Gibt letzten Nicht-Timeout-Hop vor dem Ziel zurück.
     * Überspringt Gateway (direkte Verbindung → kein Zwischenknoten).
     * Berücksichtigt auch Timeout-Sequenzen: letzte bekannte IP vor Timeout-Block.
     */
    static String findUpstream(List<TracerouteRunner.HopInfo> hops,
                               String targetIp, String gatewayIp) {
        if (hops.size() < 2) return null;

        // Rückwärts iterieren: letzter Knoten vor dem Ziel der kein Gateway ist
        for (int i = hops.size() - 1; i >= 0; i--) {
            TracerouteRunner.HopInfo hop = hops.get(i);
            if (hop.timeout || hop.ip == null || hop.ip.isBlank()) continue;
            if (hop.ip.equals(targetIp))   continue;
            if (hop.ip.equals(gatewayIp))  continue;
            // Nur wenn wirklich ein Zwischenknoten vorkommt (hop-Nummer < letzter Hop)
            if (hop.number < hops.size())  return hop.ip;
        }
        return null;
    }

    /**
     * Markiert IPs die häufig als Zwischenknoten auftreten als Switch in MapSwitchStore.
     * Verhindert, dass derselbe Switch für jede Route einzeln erkannt werden muss.
     */
    private static void promoteFrequentHopsToSwitches() {
        hopFrequency.forEach((ip, count) -> {
            if (count >= SWITCH_PROMOTE_THRESHOLD && !MapSwitchStore.contains(ip))
                MapSwitchStore.add(ip);
        });
    }
}