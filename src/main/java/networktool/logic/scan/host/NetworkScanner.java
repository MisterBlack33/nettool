package main.java.networktool.logic.scan.host;

import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.CIDRUtils;

import java.util.*;
import java.util.concurrent.*;

/** Orchestriert CIDR-Scans. Einzel-IP-Logik siehe {@link NetworkScannerIpScan}. */
public final class NetworkScanner {

    private NetworkScanner() {}

    private static final int THREAD_COUNT  = Math.max(20, Runtime.getRuntime().availableProcessors() * 4);
    /** Netze bis zu dieser Größe werden direkt ohne PingSweep-Vorfilter gescannt. */
    private static final int DIRECT_LIMIT  = 254;

    public static volatile boolean testMode = false;

    public static List<ScanResult> scanCIDR(String cidr) {
        if (testMode) return Collections.emptyList();
        List<String> allIps = CIDRUtils.getAllIPs(cidr);
        List<String> ips    = allIps.size() <= DIRECT_LIMIT ? allIps : sweepFirst(allIps);

        List<ScanResult> results  = Collections.synchronizedList(new ArrayList<>());
        ScanProgress     progress  = new ScanProgress(ips.size());
        ExecutorService  executor  = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        System.out.println("Scanne " + ips.size() + " Hosts in " + cidr
                + " (Threads: " + THREAD_COUNT + ")...");

        for (String ip : ips) {
            executor.submit(() -> {
                try   { NetworkScannerIpScan.scanIp(ip, results); }
                finally { progress.step(); }
            });
        }

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        ScanHistory.getInstance().add(cidr, results);
        return results;
    }

    private static List<String> sweepFirst(List<String> allIps) {
        List<String> alive = PingSweep.sweep(allIps, null);
        System.out.println("  [PingSweep] " + alive.size() + "/" + allIps.size()
                + " Hosts erreichbar → voll scannen");
        return alive;
    }
}