package main.java.networktool.logic.scan;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Prüft ob ein Host erreichbar ist.
 *
 * Reihenfolge:
 *  1. ARP-Cache lesen  (kein Root nötig, sehr schnell im LAN)
 *  2. ICMP isReachable (klappt ohne Root meist nur auf loopback)
 *  3. TCP-Probe auf bekannte Ports
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final int ICMP_TIMEOUT = 800;
    private static final int TCP_TIMEOUT  = 600;
    private static final int MAX_THREADS  =
            Math.min(48, Runtime.getRuntime().availableProcessors() * 4);

    private static final ExecutorService POOL = new ThreadPoolExecutor(
            8, MAX_THREADS,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            r -> { Thread t = new Thread(r, "AliveChecker"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy());

    // Häufige LAN-Ports: Windows, Linux, macOS, Router, Drucker, IoT
    private static final List<Integer> PROBE_PORTS = List.of(
            80, 443, 22, 445, 139, 135, 3389,
            8080, 8443, 21, 23, 25, 53,
            548, 5000, 5353,
            515, 631, 9100,
            1883, 8883,
            110, 143, 587,
            9090, 9200, 3306, 5432, 6379,
            554, 8554, 1900
    );

    private static final Pattern MAC_PAT =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PAT  =
            Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    public static boolean isAlive(String host) {
        // 1. ARP-Cache — im LAN fastest path, kein Root nötig
        if (isInArpCache(host)) return true;

        // 2. ICMP + TCP parallel
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

        long deadline = System.currentTimeMillis() + TCP_TIMEOUT + 200L;
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

    /** Prüft ob die IP bereits im ARP-Cache steht (= war kürzlich im LAN aktiv). */
    private static boolean isInArpCache(String host) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            Process p = Runtime.getRuntime().exec(win ? "arp -a" : "arp -a -n");
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher ipM  = IP_PAT.matcher(line);
                    Matcher macM = MAC_PAT.matcher(line);
                    if (ipM.find() && macM.find() && ipM.group().equals(host))
                        return true;
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return false;
    }
}