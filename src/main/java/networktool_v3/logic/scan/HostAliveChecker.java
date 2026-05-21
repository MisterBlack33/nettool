package main.java.networktool_v3.logic.scan;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int TIMEOUT_MS = 600;

    private static final List<Integer> PROBE_PORTS = Arrays.asList(
            80, 443, 22, 445, 3389, 53, 8080,
            139, 21, 25, 3306, 5985, 8443, 9100, 1883
    );

    public static boolean isAlive(String host) {
        ExecutorService exec = Executors.newCachedThreadPool();
        CompletionService<Boolean> cs = new ExecutorCompletionService<>(exec);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            tasks.add(() -> {
                try { return InetAddress.getByName(host).isReachable(TIMEOUT_MS); }
                catch (Exception e) { return false; }
            });
            for (int port : PROBE_PORTS) {
                final int p = port;
                tasks.add(() -> {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(host, p), TIMEOUT_MS);
                        return true;
                    } catch (Exception e) { return false; }
                });
            }
            int total = tasks.size();
            tasks.forEach(cs::submit);
            long deadline = System.currentTimeMillis() + TIMEOUT_MS + 50L;
            for (int i = 0; i < total; i++) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                try {
                    Future<Boolean> f = cs.poll(remaining, TimeUnit.MILLISECONDS);
                    if (f != null && Boolean.TRUE.equals(f.get())) return true;
                } catch (Exception ignored) {}
            }
            return false;
        } finally {
            exec.shutdownNow();
        }
    }
}