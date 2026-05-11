package main.java.networktool_v3.logic.scan;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Prüft ob ein Host erreichbar ist.
 *
 * Strategie (parallel):
 *  1. ICMP isReachable()
 *  2. TCP-Connect auf bekannte Probe-Ports
 *
 * Alle Checks laufen gleichzeitig. Sobald einer antwortet,
 * wird true zurückgegeben. Gesamtdauer ≈ TIMEOUT_MS.
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int TIMEOUT_MS = 600;

    private static final List<Integer> PROBE_PORTS = Arrays.asList(
            80, 443, 22, 445, 3389, 53, 8080,
            139, 21, 25, 3306, 5985, 8443, 9100, 1883
    );

    public static boolean isAlive(String host) {
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            // ICMP + alle Ports parallel
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

            // Ersten positiven Treffer sofort zurückgeben
            return exec.invokeAny(tasks,
                    TIMEOUT_MS + 50L, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        } finally {
            exec.shutdownNow();
        }
    }
}
