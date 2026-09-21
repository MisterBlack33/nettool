package main.java.networktool.logic.scan.schedule;

import main.java.networktool.logging.DebugLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/** Prüft Hosts parallel in einem eigenen Pool (blockierende I/O gehört nicht in den Common-Pool). */
final class OfflineAliveProbe {

    private static final int MAX_THREADS = 16;

    private OfflineAliveProbe() {}

    static Set<String> aliveOf(Set<String> ips, Predicate<String> alive) {
        if (ips.isEmpty()) return Set.of();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(MAX_THREADS, ips.size()), r -> {
            Thread t = new Thread(r, "OfflineProbe");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (String ip : ips) futures.add(pool.submit(() -> alive.test(ip) ? ip : null));
            return collect(futures);
        } finally {
            pool.shutdownNow();
        }
    }

    private static Set<String> collect(List<Future<String>> futures) {
        Set<String> result = new HashSet<>();
        for (Future<String> f : futures) {
            try {
                String ip = f.get();
                if (ip != null) result.add(ip);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                DebugLogger.getInstance().log("WARN", "[OfflineAliveProbe] " + e.getCause());
            }
        }
        return result;
    }
}
