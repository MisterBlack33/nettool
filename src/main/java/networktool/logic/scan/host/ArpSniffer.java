package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Passive Host-Erkennung: beobachtet die ARP-Nachbartabelle und meldet neue
 * Hosts sowie MAC-Wechsel, ohne selbst zu scannen. Der Startzustand wird still
 * als Baseline übernommen, damit bekannte Geräte keine Meldungsflut auslösen.
 */
public final class ArpSniffer {

    public static final int POLL_INTERVAL_SEC = 5;

    private static final class Holder {
        static final ArpSniffer INSTANCE = new ArpSniffer(ArpNeighborSource::read);
    }

    public static ArpSniffer getInstance() { return Holder.INSTANCE; }

    private final Supplier<Map<String, String>> source;
    private final Map<String, String> known = new ConcurrentHashMap<>();
    private volatile Consumer<ArpSighting> listener;
    private ScheduledExecutorService scheduler;

    ArpSniffer(Supplier<Map<String, String>> source) {
        this.source = source;
    }

    public synchronized void start(Consumer<ArpSighting> onSighting) {
        if (isActive()) return;
        listener = onSighting;
        seedBaseline();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ArpSniffer");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::poll, POLL_INTERVAL_SEC, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (!isActive()) return;
        scheduler.shutdownNow();
        scheduler = null;
        listener = null;
    }

    public synchronized boolean isActive() { return scheduler != null; }

    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(Map.copyOf(known));
    }

    void seedBaseline() {
        known.clear();
        readSource().forEach(known::put);
    }

    void poll() {
        readSource().forEach((ip, mac) -> {
            String previous = known.put(ip, mac);
            if (!mac.equals(previous)) notifyListener(new ArpSighting(ip, mac, previous));
        });
    }

    private Map<String, String> readSource() {
        try {
            return source.get();
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[ArpSniffer] Nachbartabelle nicht lesbar: " + e);
            return Map.of();
        }
    }

    private void notifyListener(ArpSighting sighting) {
        Consumer<ArpSighting> target = listener;
        if (target == null) return;
        try {
            target.accept(sighting);
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[ArpSniffer] Listener fehlgeschlagen: " + e);
        }
    }
}
