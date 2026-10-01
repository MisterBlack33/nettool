package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.TimeoutConfig;
import main.java.networktool.logic.ScanOutcome;
import main.java.networktool.logic.scan.schedule.ScanRateLimiter;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;

/**
 * Prüft ob ein Host erreichbar ist.
 *
 * Strategie:
 *  1. ARP-Cache (einmal gecacht pro Scan-Lauf)
 *  2. ICMP isReachable
 *  3. TCP-Probe auf häufige Ports (bricht bei erstem Treffer ab)
 *
 * Jeder neue Verbindungsversuch (ICMP + jede TCP-Probe) wird vor dem Connect
 * über {@link ScanRateLimiter} gedrosselt, um Ping-/Port-Sweep-Erkennung durch
 * IDS/Firewalls zu vermeiden. Siehe {@link #setRateLimit} für Testkonfiguration.
 */
public final class HostAliveChecker {

    private HostAliveChecker() {}

    static void setTestTimeouts(int icmp, int tcp) {
        TimeoutConfig.ICMP_REACHABLE_MS = icmp;
        TimeoutConfig.TCP_PROBE_MS      = tcp;
    }

    private static volatile ScanRateLimiter rateLimiter = ScanRateLimiter.getInstance();

    /** Ersetzt den Rate-Limiter (z.B. für Tests eine hohe Rate, um Timeouts nicht zu sprengen). */
    static void setRateLimit(double ratePerSecond, double burst) {
        rateLimiter = new ScanRateLimiter(ratePerSecond, burst);
    }

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
            548, 631, 9100, 1883,
            62078, 5353, 7000   // iOS lockdownd, mDNS, AirPlay
    );

    // ARP-Cache: einmal pro Scan geladen, verhindert 254x "arp -a"
    private static volatile Set<String> cachedArpIps  = Collections.emptySet();
    private static volatile long        arpCacheTime  = 0;
    private static final long ARP_CACHE_TTL_MS = 10_000;

    public static boolean isAlive(String host) {
        return probe(host).orElse(false);
    }

    public static ScanOutcome<Boolean> probe(String host) {
        if (host == null || host.isBlank()) {
            DebugLogger.getInstance().log("WARN", "[HostAliveChecker] Host fehlt");
            return ScanOutcome.failure("host fehlt");
        }
        if (isInArpCache(host)) return ScanOutcome.success(true);

        // ICMP + Ports parallel, brich bei erstem Treffer ab
        CompletionService<Boolean> cs =
                new ExecutorCompletionService<>(POOL);

        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicReference<Throwable> probeFailure = new AtomicReference<>();

        futures.add(cs.submit(() -> {
            rateLimiter.acquire();
            try { return InetAddress.getByName(host).isReachable(TimeoutConfig.ICMP_REACHABLE_MS); }
            catch (IOException | SecurityException e) { return failedProbe(e, probeFailure); }
        }));

        for (int port : PROBE_PORTS) {
            final int p = port;
            futures.add(cs.submit(() -> {
                rateLimiter.acquire();
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, p), TimeoutConfig.TCP_PROBE_MS);
                    return true;
                } catch (IOException | SecurityException e) { return failedProbe(e, probeFailure); }
            }));
        }

        long deadline = System.currentTimeMillis() + TimeoutConfig.TCP_PROBE_MS + 300L;
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            DebugLogger.getInstance().log("FINE",
                    "[HostAliveChecker] Probe unterbrochen");
            return ScanOutcome.failure("unterbrochen");
        } catch (ExecutionException e) {
            probeFailure.compareAndSet(null, e.getCause() == null ? e : e.getCause());
        } catch (RuntimeException e) {
            return failure(host, e);
        } finally {
            futures.forEach(f -> f.cancel(true));
        }
        if (alive) return ScanOutcome.success(true);
        Throwable failure = probeFailure.get();
        if (failure != null) {
            return failure(host, failure);
        }
        if (checked < futures.size()) {
            return failure(host, new SocketTimeoutException("probe deadline"));
        }
        return ScanOutcome.success(alive);
    }

    private static ScanOutcome<Boolean> failure(String host, Throwable error) {
        ScanErrorClassifier.Kind kind = ScanErrorClassifier.classify(error);
        String level = switch (kind) {
            case TIMEOUT, HOST_OFFLINE, CONNECTION_REFUSED -> "FINE";
            default -> "WARN";
        };
        String description = ScanErrorClassifier.describe(host, error);
        DebugLogger.getInstance().log(level, "[HostAliveChecker] " + description);
        return ScanOutcome.failure(description);
    }

    private static boolean failedProbe(Throwable error, AtomicReference<Throwable> firstFailure) {
        ScanErrorClassifier.Kind kind = ScanErrorClassifier.classify(error);
        if (kind != ScanErrorClassifier.Kind.HOST_OFFLINE
                && kind != ScanErrorClassifier.Kind.CONNECTION_REFUSED) {
            firstFailure.compareAndSet(null, error);
        }
        return false;
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
        } catch (IOException | SecurityException e) {
            DebugLogger.getInstance().log("FINE",
                    "[HostAliveChecker] " + ScanErrorClassifier.describe("arp-cache", e));
        }
        cachedArpIps = Collections.unmodifiableSet(ips);
        arpCacheTime = System.currentTimeMillis();
    }
}