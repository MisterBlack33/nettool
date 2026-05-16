package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.util.CIDRUtils;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

/** Parallel reachability probing for remote networks. Package-private. */
final class RemoteNetProbe {

    private RemoteNetProbe() {}

    static final int PROBE_COUNT   = 8;
    static final int REACH_TIMEOUT = 1200;

    static RemoteNetScanner.ReachResult parallelProbe(String cidr) {
        List<String> allIps;
        try { allIps = CIDRUtils.getAllIPs(RemoteNetScanner.normalizeCidr(cidr)); }
        catch (Exception e) { return new RemoteNetScanner.ReachResult(false, 0, 0); }
        if (allIps.isEmpty()) return new RemoteNetScanner.ReachResult(false, 0, 0);

        List<String> probes = selectProbes(allIps);
        List<Future<Long>> futures = submitProbes(probes);

        int responded = 0;
        long totalMs  = 0;
        for (Future<Long> f : futures) {
            try {
                long ms = f.get(100, TimeUnit.MILLISECONDS);
                if (ms >= 0) { responded++; totalMs += ms; }
            } catch (Exception ignored) {}
        }
        return new RemoteNetScanner.ReachResult(
                responded > 0, responded, responded > 0 ? totalMs / responded : 0);
    }

    static Map<String, RemoteNetScanner.ReachResult> parallelProbeAll(List<String> cidrs) {
        Map<String, RemoteNetScanner.ReachResult> result = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(Math.min(cidrs.size(), 8));
        cidrs.forEach(cidr -> exec.submit(() -> result.put(cidr, parallelProbe(cidr))));
        exec.shutdown();
        try { exec.awaitTermination(REACH_TIMEOUT * 2L + 1000, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        cidrs.forEach(c -> result.putIfAbsent(c, new RemoteNetScanner.ReachResult(false, 0, 0)));
        return result;
    }

    private static List<String> selectProbes(List<String> allIps) {
        int step = Math.max(1, allIps.size() / PROBE_COUNT);
        List<String> probes = new ArrayList<>();
        for (int i = 0; i < PROBE_COUNT && i * step < allIps.size(); i++)
            probes.add(allIps.get(i * step));
        // Always probe .1 (likely gateway)
        String base = allIps.get(0);
        String dot1 = base.substring(0, base.lastIndexOf('.') + 1) + "1";
        if (!probes.contains(dot1)) probes.set(0, dot1);
        return probes;
    }

    private static List<Future<Long>> submitProbes(List<String> probes) {
        ExecutorService exec = Executors.newFixedThreadPool(probes.size());
        List<Future<Long>> futures = probes.stream()
                .map(ip -> exec.submit(() -> {
                    long t = System.currentTimeMillis();
                    try { return InetAddress.getByName(ip).isReachable(REACH_TIMEOUT)
                            ? System.currentTimeMillis() - t : -1L; }
                    catch (Exception e) { return -1L; }
                }))
                .toList();
        exec.shutdown();
        try { exec.awaitTermination(REACH_TIMEOUT + 500L, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return futures;
    }
}