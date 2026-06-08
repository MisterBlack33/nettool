package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Prüft Erreichbarkeit via ICMP + erweitertem TCP-Port-Set.
 *
 * Fix: Ports erweitert (inkl. typische Heimnetz-Geräte wie Drucker,
 * Router, Kameras, Smart-TVs). ICMP-Timeout verkürzt für schnelleren
 * Scan. Gibt true zurück sobald einer der Checks antwortet.
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int ICMP_TIMEOUT = 600;
    private static final int TCP_TIMEOUT  = 500;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            Math.min(128, Runtime.getRuntime().availableProcessors() * 16),
            r -> { Thread t = new Thread(r, "AliveChecker"); t.setDaemon(true); return t; });

    /** Breiter Port-Satz: deckt Windows, Linux, macOS, Router, Drucker,
     *  Smart-Home, NAS, Kameras und typische Heimnetz-Geräte ab. */
    private static final List<Integer> PROBE_PORTS = List.of(
            80, 443, 22, 445, 3389, 8080,   // Standard
            21, 23, 25, 53, 110, 143,        // FTP/Telnet/Mail/DNS
            8443, 8888, 9090, 9200,          // Web-Alt
            139, 135, 5985,                  // Windows
            548, 5000, 5353,                 // macOS/Bonjour
            515, 631, 9100,                  // Drucker
            1883, 8883,                      // MQTT/IoT
            554, 8554,                       // RTSP/Kameras
            7, 9, 13, 17,                    // Echo/Discard – oft auf Routern
            1900, 5000                       // UPnP/SSDP
    );

    public static boolean isAlive(String host) {
        List<Future<Boolean>> futures = new ArrayList<>(PROBE_PORTS.size() + 1);

        // ICMP
        futures.add(POOL.submit(() -> {
            try { return InetAddress.getByName(host).isReachable(ICMP_TIMEOUT); }
            catch (Exception e) { return false; }
        }));

        // TCP-Probes
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