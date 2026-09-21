package main.java.networktool.logic.analysis.security;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.ports.BannerGrabber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Prüft Banner offener Ports jedes Scan-Treffers gegen {@link CveLookup}.
 * Der Scan liefert nur "offen" als Banner, daher werden Banner hier gezielt
 * nachgeladen — nur solange der Hook aktiviert ist, und asynchron, damit
 * der Scan selbst nicht langsamer wird.
 */
public final class ScanSecurityHook {

    private static final int WORKER_THREADS = 2;
    private static final int QUEUE_CAPACITY = 1024;

    private static final class Holder {
        static final ScanSecurityHook INSTANCE = new ScanSecurityHook(
                SecurityFindingsCollector.getInstance(), BannerGrabber::grab, newWorkerPool());
    }
    public static ScanSecurityHook getInstance() { return Holder.INSTANCE; }

    private final FindingReporter reporter;
    private final BiFunction<String, Integer, String> bannerSource;
    private final Executor executor;
    private volatile boolean enabled;

    /** Synchrone Variante für deterministische Tests. */
    ScanSecurityHook(SecurityFindingsCollector sink, BiFunction<String, Integer, String> bannerSource) {
        this(sink, bannerSource, Runnable::run);
    }

    ScanSecurityHook(SecurityFindingsCollector sink, BiFunction<String, Integer, String> bannerSource,
                     Executor executor) {
        this.reporter     = new FindingReporter(sink);
        this.bannerSource = bannerSource;
        this.executor     = executor;
    }

    public boolean isEnabled()              { return enabled; }
    public void    setEnabled(boolean on)   { enabled = on; }

    public void onHost(String ip, Map<Integer, String> openPorts) {
        if (!enabled || openPorts == null) return;
        List<Integer> ports = List.copyOf(openPorts.keySet());
        executor.execute(() -> checkPorts(ip, ports));
    }

    private void checkPorts(String ip, List<Integer> ports) {
        try {
            for (Integer port : ports)
                CveLookup.classify(ip, bannerSource.apply(ip, port)).ifPresent(reporter::report);
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[ScanSecurityHook] " + ip + ": " + e);
        }
    }

    private static Executor newWorkerPool() {
        return new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY), r -> {
                    Thread t = new Thread(r, "ScanSecurityHook");
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.DiscardPolicy());
    }
}
