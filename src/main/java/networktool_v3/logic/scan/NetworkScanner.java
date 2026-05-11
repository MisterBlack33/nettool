package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.logic.ports.PortScanner;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.util.CIDRUtils;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scannt alle Hosts in einem CIDR-Bereich (z.B. 192.168.1.0/24).
 *
 * Änderungen gegenüber der Vorgängerversion:
 *  - REACH_TIMEOUT: 500ms → 800ms  (zuverlässigere Erreichbarkeitsprüfung)
 *  - PORT_TIMEOUT:  200ms → PortScanner.TIMEOUT_FAST (600ms)
 *    → scanSimple() läuft trotzdem parallel, Gesamtdauer bleibt ~600ms pro Host
 *  - THREAD_COUNT: 50 → 80 (mehr parallele Hosts bei /16-Scans)
 *  - Hostname-Auflösung mit Timeout (kein unlimitiertes DNS-Warten mehr)
 */
public final class NetworkScanner {

    private NetworkScanner() {}

    private static final int THREAD_COUNT  = 80;
    private static final int REACH_TIMEOUT = 800;

    public static List<ScanResult> scanCIDR(String cidr) {
        List<String> allIps = CIDRUtils.getAllIPs(cidr);
        List<String> ips    = allIps.size() > 254
                ? PingSweep.sweep(allIps, null)
                : allIps;
        if (allIps.size() > 254)
            System.out.println("  [PingSweep] " + ips.size() + "/" + allIps.size()
                    + " Hosts erreichbar → voll scannen");

        List<ScanResult> results = Collections.synchronizedList(new ArrayList<>());
        ScanProgress     progress = new ScanProgress(ips.size());
        ExecutorService  executor = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        System.out.println("Scanne " + ips.size() + " Hosts in " + cidr + " ...");

        for (String ip : ips) {
            executor.submit(() -> {
                try   { scanIp(ip, results); }
                finally { progress.step(); }
            });
        }

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        ScanHistory.getInstance().add(cidr, results);
        return results;
    }

    // ── Private Hilfsmethoden ─────────────────────────────────────────────

    private static void scanIp(String ip, List<ScanResult> results) {
        try {
            if (!InetAddress.getByName(ip).isReachable(REACH_TIMEOUT)) return;

            // Hostname mit Timeout auflösen (max 1s, kein unlimitiertes DNS-Warten)
            String hostname = resolveHostname(ip);

            // Port-Scan: nutzt TIMEOUT_FAST (600ms) und FAST_PORTS parallel
            Map<Integer, String> ports = PortScanner.scanSimple(ip, 0);

            String os = OsDetector.detect(ip);
            results.add(new ScanResult(ip, hostname, ports, os));
        } catch (Exception ignored) {}
    }

    /**
     * Löst den Hostnamen mit Timeout auf.
     * Gibt die IP zurück wenn DNS nicht antwortet (statt endlos zu warten).
     */
    private static String resolveHostname(String ip) {
        String[] result = {ip};
        Thread t = new Thread(() -> {
            try {
                String name = InetAddress.getByName(ip).getCanonicalHostName();
                if (name != null && !name.equals(ip)) result[0] = name;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(1_000); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }
}