package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Prüft ob ein Host erreichbar ist.
 *
 * Strategie:
 *  1. ARP-Cache (einmal gecacht pro Scan-Lauf)
 *  2. ICMP isReachable
 *  3. TCP-Probe auf häufige Ports (bricht bei erstem Treffer ab)
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    private static final Logger LOGGER = Logger.getLogger(HostAliveChecker.class.getName());
    private static final int ICMP_TIMEOUT = 500;
    private static final int TCP_TIMEOUT  = 400;
    private static final int MAX_THREADS  =
            Math.min(64, Runtime.getRuntime().availableProcessors() * 8);

    private static final ExecutorService POOL = new ThreadPoolExecutor(
            16, MAX_THREADS,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            r -> { Thread t = new Thread(r, "AliveChecker"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.DiscardPolicy());

    private static final List<Integer> PROBE_PORTS = List.of(
            80, 443, 22, 445, 3389, 8080,
            135, 139, 21, 23, 53,
            548, 631, 9100, 1883
    );

    // ARP-Cache: einmal pro Scan geladen, verhindert 254x "arp -a"
    private static volatile Set<String> cachedArpIps  = Collections.emptySet();
    private static volatile long        arpCacheTime  = 0;
    private static final long ARP_CACHE_TTL_MS = 10_000;

    public static boolean isAlive(String host) {
        if (isInArpCache(host)) return true;

        // ICMP + Ports parallel, brich bei erstem Treffer ab
        CompletionService<Boolean> cs =
                new ExecutorCompletionService<>(POOL);

        List<Future<Boolean>> futures = new ArrayList<>();

        futures.add(cs.submit(() -> {
            try { return InetAddress.getByName(host).isReachable(ICMP_TIMEOUT); }
            catch (Exception e) { return false; }
        }));

        for (int port : PROBE_PORTS) {
            final int p = port;
            futures.add(cs.submit(() -> {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), TCP_TIMEOUT);
                    return true;
                } catch (Exception e) { return false; }
            }));
        }

        long deadline = System.currentTimeMillis() + TCP_TIMEOUT + 300L;
        boolean alive = false;
        int checked = 0;

        try {
            while (checked < futures.size()) {
                long rem = deadline - System.currentTimeMillis();
                if (rem <= 0) break;
                Future<Boolean> f = cs.poll(rem, TimeUnit.MILLISECONDS);
                if (f == null) break;
                checked++;
                if (Boolean.TRUE.equals(f.get())) { alive = true; break; }
            }
        } catch (Exception ignored) {
        } finally {
            futures.forEach(f -> f.cancel(true));
        }
        return alive;
    }

    /**
     * Lädt den ARP-Cache einmal und hält ihn TTL-lang.
     * Verhindert 254x "arp -a"-Prozesse pro Scan.
     */
    public static void warmCache() {
        loadArpCache();
    }

    private static boolean isInArpCache(String host) {
        if (System.currentTimeMillis() - arpCacheTime > ARP_CACHE_TTL_MS)
            loadArpCache();
        return cachedArpIps.contains(host);
    }

    private static final Pattern MAC_PAT =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PAT  =
            Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    private static synchronized void loadArpCache() {
        if (System.currentTimeMillis() - arpCacheTime < ARP_CACHE_TTL_MS) return;
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        Set<String> ips = new HashSet<>();
        try {
            Process p = Runtime.getRuntime().exec(win ? "arp -a" : "arp -a -n");
            try (var br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher ipM  = IP_PAT.matcher(line);
                    Matcher macM = MAC_PAT.matcher(line);
                    if (ipM.find() && macM.find()) ips.add(ipM.group());
                }
            }
            p.destroy();
        } catch (java.io.IOException ioException) {
            LOGGER.log(Level.FINE, "IO error reading ARP cache", ioException);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unexpected error loading ARP cache", exception);
        }
        cachedArpIps = Collections.unmodifiableSet(ips);
        arpCacheTime = System.currentTimeMillis();
    }
}