package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.analysis.os.OsDetector;
import main.java.networktool.logic.analysis.probe.OuiDatabase;
import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Führt den parallelen Host-Scan aus und führt ARP-/Discovery-/ICMP-Treffer zusammen. Package-private. */
final class NetworkHostScanOrchestrator {

    private NetworkHostScanOrchestrator() {}

    private static final int THREAD_COUNT = Math.min(64,
            Math.max(20, Runtime.getRuntime().availableProcessors() * 4));

    static List<HostResult> scanIpList(List<String> ips, Map<String, String> knownMacs,
                                       Set<String> discovered) {
        System.out.println("Starte Scan: " + ips.size() + " Hosts, Threads: " + THREAD_COUNT);
        List<HostResult> found    = java.util.Collections.synchronizedList(new ArrayList<>());
        ScanProgress     progress = new ScanProgress(ips.size());
        ExecutorService  executor = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        for (String host : ips)
            executor.submit(() -> { scanHost(host, found, knownMacs, discovered); progress.step(); });

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        Set<String> foundIps = new HashSet<>();
        found.forEach(h -> foundIps.add(h.ip));

        knownMacs.forEach((ip, mac) -> {
            if (!foundIps.contains(ip)) {
                String hostname = NetworkHostnameResolver.resolveHostname(ip);
                String os = detectOsFull(ip, hostname, mac);
                found.add(new HostResult(ip, buildDisplay(hostname, mac), os));
                foundIps.add(ip);
            }
        });
        // Nur per mDNS/UPnP/Zweit-Ping gefundene Hosts (kein ARP-Eintrag, kein TCP/ICMP-Treffer)
        discovered.forEach(ip -> {
            if (!foundIps.contains(ip)) {
                String hostname = NetworkHostnameResolver.resolveHostname(ip);
                found.add(new HostResult(ip, hostname, detectOsFull(ip, hostname, null)));
                foundIps.add(ip);
            }
        });

        persistToHistory(found);
        System.out.println("Scan abgeschlossen: " + found.size() + " Gerät(e) gefunden.");
        return found;
    }

    private static void scanHost(String ip, List<HostResult> found, Map<String, String> knownMacs,
                                 Set<String> discovered) {
        try {
            boolean alive = HostAliveChecker.isAlive(ip)
                    || knownMacs.containsKey(ip)
                    || discovered.contains(ip);
            if (!alive) return;
            String mac      = knownMacs.getOrDefault(ip, NetworkHostArpResolver.readMacFromArp(ip));
            String hostname = NetworkHostnameResolver.resolveHostname(ip);
            String os       = detectOsFull(ip, hostname, mac);
            found.add(new HostResult(ip, buildDisplay(hostname, mac), os));
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostScanOrchestrator] Scan von " + ip + " fehlgeschlagen: " + e);
        }
    }

    private static String detectOsFast(String ip, String hostname, String mac) {
        String fromHn = OsDetector.detectFromHostname(hostname, ip);
        if (fromHn != null) return fromHn;
        if (mac != null && mac.length() >= 8) {
            String vendor = OuiDatabase.lookup(mac.substring(0, 8));
            if (vendor != null) return vendor;
        }
        return "Unbekannt";
    }

    private static String detectOsFull(String ip, String hostname, String mac) {
        String fast = detectOsFast(ip, hostname, mac);
        if (!"Unbekannt".equals(fast)) return fast;
        try {
            String full = OsDetector.detect(ip); // volle Pipeline: Banner/UDP/mDNS/Ports/TTL
            if (full != null && !"Unbekannt".equals(full)) return full;
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostScanOrchestrator] Volle OS-Erkennung fehlgeschlagen (" + ip + "): " + e);
        }
        return fast;
    }

    private static void persistToHistory(List<HostResult> found) {
        if (found.isEmpty()) return;
        List<ScanResult> sr = found.stream()
                .map(h -> new ScanResult(h.ip, h.hostname, h.ports, h.os))
                .toList();
        ScanHistory.getInstance().add("Lokaler Scan", sr);
    }

    private static String buildDisplay(String hostname, String mac) {
        return mac != null && !mac.isBlank()
                ? hostname + " [" + mac + "]"
                : hostname;
    }
}