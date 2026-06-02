package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.OsDetector;
import main.java.networktool.logic.ports.PortScanner;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.CIDRUtils;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

public final class NetworkScanner {

    private NetworkScanner() {}

    // Adaptive thread count based on CPU cores
    private static final int THREAD_COUNT  = Math.max(20, Runtime.getRuntime().availableProcessors() * 4);
    private static final int REACH_TIMEOUT = 1000;

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

        System.out.println("Scanne " + ips.size() + " Hosts in " + cidr
                + " (Threads: " + THREAD_COUNT + ")...");

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

    private static void scanIp(String ip, List<ScanResult> results) {
        try {
            if (!InetAddress.getByName(ip).isReachable(REACH_TIMEOUT)) return;

            String hostname = resolveHostname(ip);
            Map<Integer, String> ports = PortScanner.scanSimple(ip, 0);
            String os = OsDetector.detect(ip);
            results.add(new ScanResult(ip, hostname, ports, os));
        } catch (Exception ignored) {}
    }

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
        try { t.join(800); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }
}