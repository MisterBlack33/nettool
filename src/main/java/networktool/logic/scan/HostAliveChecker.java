// src/main/java/networktool/logic/scan/HostAliveChecker.java
package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int ICMP_TIMEOUT = 600;
    private static final int TCP_TIMEOUT  = 500;
    private static final int MAX_THREADS  =
            Math.min(32, Runtime.getRuntime().availableProcessors() * 2);

    // Bounded pool statt unbegrenzt wachsendem static POOL
    private static final ExecutorService POOL = new ThreadPoolExecutor(
            4, MAX_THREADS,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            r -> { Thread t = new Thread(r, "AliveChecker"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static final List<Integer> PROBE_PORTS = List.of(
            80, 443, 22, 445, 3389, 8080,
            21, 23, 25, 53, 110, 143,
            8443, 8888, 9090, 9200,
            139, 135, 5985,
            548, 5000, 5353,
            515, 631, 9100,
            1883, 8883,
            554, 8554
    );

    public static boolean isAlive(String host) {
        List<Future<Boolean>> futures = new ArrayList<>(PROBE_PORTS.size() + 1);

        futures.add(POOL.submit(() -> {
            try { return InetAddress.getByName(host).isReachable(ICMP_TIMEOUT); }
            catch (Exception e) { return false; }
        }));

        for (int port : PROBE_PORTS) {
            final int p = port;
            futures.add(POOL.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), TCP_TIMEOUT);
                    return true;
                } catch (Exception e) { return false; }
            }));
        }

        long deadline = System.currentTimeMillis() + TCP_TIMEOUT + 100L;
        try {
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
        } finally {
            futures.forEach(x -> x.cancel(true));
        }
        return false;
    }
}