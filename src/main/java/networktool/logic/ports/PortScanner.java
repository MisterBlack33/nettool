package main.java.networktool.logic.ports;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PortScanner {

    private PortScanner() {}

    public static int TIMEOUT_LAN  = 1_500;
    public static int TIMEOUT_FAST = 800;
    private static final int RETRIES     = 1;

    public static final List<Integer> DEFAULT_PORTS = List.of(
            21, 22, 23, 25,
            80, 443, 8080, 8443, 8888, 3000,
            135, 139, 445, 3389, 5985, 5986,
            1433, 1521, 3306, 5432, 5984, 6379, 27017,
            53, 67, 161, 162,
            515, 631, 9100,
            548, 5353,
            1883, 8883,
            110, 143, 465, 587, 993, 995,
            9090, 9200, 6443
    );

    public static final List<Integer> FAST_PORTS = List.of(
            22, 80, 443, 445, 3389, 8080,
            21, 23, 25, 53, 135, 139, 631, 1883, 3306
    );

    private static volatile List<Integer> activePorts = DEFAULT_PORTS;

    private static final int MAX_SCAN_THREADS = Math.min(32, Runtime.getRuntime().availableProcessors() * 4);

    public static void setActivePorts(List<Integer> ports) {
        activePorts = (ports == null || ports.isEmpty())
                ? DEFAULT_PORTS
                : Collections.unmodifiableList(new ArrayList<>(ports));
        System.out.println("[PortScanner] Port-Liste: " + activePorts.size() + " Ports");
    }

    public static List<Integer> getActivePorts() { return activePorts; }

    public static Map<Integer, String> scanParallel(String host, int timeoutMs)
            throws InterruptedException {
        int timeout = timeoutMs > 0 ? timeoutMs : TIMEOUT_LAN;
        return doScan(host, activePorts, timeout, true);
    }

    public static Map<Integer, String> scanSimple(String host, int timeoutMs) {
        int timeout = timeoutMs > 0 ? timeoutMs : TIMEOUT_FAST;
        List<Integer> ports = (activePorts == DEFAULT_PORTS) ? FAST_PORTS : activePorts;
        try {
            return doScan(host, ports, timeout, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConcurrentHashMap<>();
        }
    }

    public static boolean isOpen(String host, int port, int timeoutMs) {
        return probePort(host, port, timeoutMs > 0 ? timeoutMs : TIMEOUT_LAN) != PortState.CLOSED;
    }

    public static boolean isOpen(String host, int port) {
        return isOpen(host, port, TIMEOUT_LAN);
    }

    private static Map<Integer, String> doScan(String host, List<Integer> ports,
                                               int timeout, boolean grabBanner)
            throws InterruptedException {
        Map<Integer, String> results = new ConcurrentHashMap<>();
        int threads = Math.min(ports.size(), MAX_SCAN_THREADS);
        ExecutorService exec = Executors.newFixedThreadPool(threads,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        AtomicInteger done = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>(ports.size());

        for (int port : ports) {
            final int p = port;
            futures.add(exec.submit(() -> {
                try {
                    PortState state = probePortWithRetry(host, p, timeout);
                    if (state != PortState.CLOSED) {
                        String banner = grabBanner
                                ? BannerGrabber.grab(host, p, timeout)
                                : "offen";
                        results.put(p, banner);
                    }
                } finally {
                    done.incrementAndGet();
                }
            }));
        }

        exec.shutdown();
        if (!exec.awaitTermination(timeout + 500L, TimeUnit.MILLISECONDS)) {
            exec.shutdownNow();
        }
        return results;
    }

    private static PortState probePortWithRetry(String host, int port, int timeout) {
        PortState first = probePort(host, port, timeout);
        if (first != PortState.CLOSED) return first;
        try { Thread.sleep(50); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PortState.CLOSED;
        }
        return probePort(host, port, timeout);
    }

    static PortState probePort(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            return PortState.OPEN;
        } catch (ConnectException | SocketTimeoutException e) {
            return psFallback(host, port);
        } catch (Exception e) {
            return psFallback(host, port);
        }
    }

    // Windows-Fallback: Firewalls blocken teils rohe Sockets, aber nicht Test-NetConnection
    private static PortState psFallback(String host, int port) {
        return main.java.networktool.logic.windows.PsPortScanResolver.isOpen(host, port)
                ? PortState.OPEN : PortState.CLOSED;
    }

    enum PortState { OPEN, REFUSED, CLOSED }
}