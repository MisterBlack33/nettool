package main.java.networktool.logic.scan.host;

import main.java.networktool.logic.scan.schedule.ScanHistory;
import main.java.networktool.logic.ScanOutcome;
import main.java.networktool.logging.DebugLogger;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.CIDRUtils;
import main.java.networktool.util.IpValidator;

import java.util.*;
import java.util.concurrent.*;

/** Orchestriert CIDR-Scans. Einzel-IP-Logik siehe {@link NetworkScannerIpScan}. */
public final class NetworkScanner {

    private NetworkScanner() {}

    private static final int THREAD_COUNT  = Math.max(20, Runtime.getRuntime().availableProcessors() * 4);
    /** Netze bis zu dieser Größe werden direkt ohne PingSweep-Vorfilter gescannt. */
    private static final int DIRECT_LIMIT  = 254;
    public static final long MAX_SCAN_HOSTS = 65_534;

    public static volatile boolean testMode = false;

    public static List<ScanResult> scanCIDR(String cidr) {
        return scanCIDROutcome(cidr).orElse(Collections.emptyList());
    }

    public static ScanOutcome<List<ScanResult>> scanCIDROutcome(String cidr) {
        return scanCIDROutcome(cidr, null);
    }

    public static ScanOutcome<List<ScanResult>> scanCIDROutcome(String cidr, List<Integer> ports) {
        if (testMode) return ScanOutcome.success(Collections.emptyList());
        String validationError = validateCidr(cidr);
        if (validationError != null) return ScanOutcome.failure(validationError);
        if (ports != null && (ports.isEmpty() || ports.size() > 1_024
                || ports.stream().anyMatch(port -> port == null || port < 1 || port > 65_535)))
            return ScanOutcome.failure("Ungültige Portliste (erlaubt sind 1 bis 1.024 Ports im Bereich 1-65535).");
        if (Thread.currentThread().isInterrupted()) return ScanOutcome.failure("unterbrochen: 0 Hosts verarbeitet");
        List<String> allIps = CIDRUtils.getAllIPs(cidr);
        List<String> ips    = allIps.size() <= DIRECT_LIMIT ? allIps : sweepFirst(allIps);
        if (Thread.currentThread().isInterrupted())
            return ScanOutcome.failure("unterbrochen: " + ips.size() + " Hosts zur Prüfung vorbereitet");

        List<ScanResult> results  = Collections.synchronizedList(new ArrayList<>());
        ScanProgress     progress  = new ScanProgress(ips.size());
        ExecutorService  executor  = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        System.out.println("Scanne " + ips.size() + " Hosts in " + cidr
                + " (Threads: " + THREAD_COUNT + ")...");

        for (String ip : ips) {
            if (Thread.currentThread().isInterrupted()) {
                executor.shutdownNow();
                return ScanOutcome.failure("unterbrochen: " + progress.completed() + "/" + ips.size()
                        + " Host(s) verarbeitet");
            }
            executor.submit(() -> {
                try   { NetworkScannerIpScan.scanIp(ip, results, ports); }
                finally { progress.step(); }
            });
        }

        executor.shutdown();
        boolean complete = true;
        try {
            complete = executor.awaitTermination(5, TimeUnit.MINUTES);
            if (!complete) {
                executor.shutdownNow();
                DebugLogger.getInstance().log("WARN",
                        "[NetworkScanner] Unvollständiger Scan: "
                                + progress.completed() + "/" + ips.size() + " Host(s) verarbeitet.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            DebugLogger.getInstance().log("WARN",
                    "[NetworkScanner] Scan unterbrochen: " + cidr);
            return ScanOutcome.failure("unterbrochen: " + progress.completed() + "/" + ips.size()
                    + " Host(s) verarbeitet");
        }

        if (!complete) {
            return ScanOutcome.failure("unvollständig: " + progress.completed() + "/" + ips.size()
                    + " Host(s) verarbeitet");
        }
        ScanHistory.getInstance().add(cidr, results);
        return ScanOutcome.success(results);
    }

    public static long estimatedHostCount(String cidr) {
        if (!IpValidator.isValidCidr(cidr)) throw new IllegalArgumentException("Ungültiges IPv4-CIDR.");
        int prefix = Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
        return prefix >= 31 ? 0 : (1L << (32 - prefix)) - 2;
    }

    public static String validateCidr(String cidr) {
        if (!IpValidator.isValidCidr(cidr)) return "Ungültiges IPv4-CIDR.";
        long hostCount = estimatedHostCount(cidr);
        if (hostCount > MAX_SCAN_HOSTS)
            return "Scanbereich zu groß (" + hostCount + " Hosts). Maximal "
                    + MAX_SCAN_HOSTS + " Hosts pro CIDR-Scan.";
        return null;
    }

    private static List<String> sweepFirst(List<String> allIps) {
        List<String> alive = PingSweep.sweep(allIps, null);
        System.out.println("  [PingSweep] " + alive.size() + "/" + allIps.size()
                + " Hosts erreichbar → voll scannen");
        return alive;
    }
}