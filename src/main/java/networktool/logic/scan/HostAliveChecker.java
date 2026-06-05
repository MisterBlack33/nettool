package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Prüft Erreichbarkeit via ICMP + TCP-Port-Probes.
 *
 * Fehler-Fix: vorher wurde pro isAlive()-Aufruf ein newCachedThreadPool()
 * erzeugt und nie geschlossen → bei 254 parallelen Scans: 254×8 = 2032
 * Threads, die nie terminieren → Thread-Starvation, keine Geräte gefunden.
 * Jetzt: ein geteilter statischer FixedThreadPool.
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int TIMEOUT_MS = 800;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            Math.min(64, Runtime.getRuntime().availableProcessors() * 8),
            r -> { Thread t = new Thread(r, "AliveChecker"); t.setDaemon(true); return t; });

    private static final List<Integer> PROBE_PORTS = List.of(80, 443, 22, 445, 3389, 8080);

    public static boolean isAlive(String host) {
        List<Future<Boolean>> futures = new ArrayList<>(PROBE_PORTS.size() + 1);

        futures.add(POOL.submit(() -> {
            try { return InetAddress.getByName(host).isReachable(TIMEOUT_MS); }
            catch (Exception e) { return false; }
        }));

        for (int port : PROBE_PORTS) {
            final int p = port;
            futures.add(POOL.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), TIMEOUT_MS);
                    return true;
                } catch (Exception e) { return false; }
            }));
        }

        long deadline = System.currentTimeMillis() + TIMEOUT_MS + 50L;
        for (Future<Boolean> f : futures) {
            long rem = deadline - System.currentTimeMillis();
            if (rem <= 0) break;
            try {
                if (Boolean.TRUE.equals(f.get(rem, TimeUnit.MILLISECONDS))) {
                    futures.forEach(x -> x.cancel(true));
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}