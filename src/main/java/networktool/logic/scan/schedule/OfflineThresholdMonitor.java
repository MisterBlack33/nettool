package main.java.networktool.logic.scan.schedule;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.scan.host.HostAliveChecker;
import main.java.networktool.model.HostResult;
import main.java.networktool.storage.StorageLocations;
import main.java.networktool.storage.network.NetworkStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Meldet gespeicherte Hosts, die länger als eine Schwelle (Stunden) offline sind.
 * "Zuletzt gesehen" überlebt Neustarts (saves/cache/offlineTracker.tsv).
 * Jeder Alarm wird zusätzlich im Debug-Log persistiert.
 */
public final class OfflineThresholdMonitor {

    private static final class Holder { static final OfflineThresholdMonitor INSTANCE = new OfflineThresholdMonitor(); }
    public static OfflineThresholdMonitor getInstance() { return Holder.INSTANCE; }

    static final long MS_PER_HOUR = 3_600_000L;
    private static final String STATE_FILE = "offlineTracker.tsv";

    private final OfflineTracker tracker = new OfflineTracker();
    private volatile boolean active;
    private volatile int thresholdHours = 1;
    private volatile Consumer<String> alertSink = msg -> {};
    private volatile Path stateFile;
    private ScheduledExecutorService scheduler;

    private OfflineThresholdMonitor() {}

    public synchronized void start(int hours, int intervalMin, Consumer<String> sink) {
        if (active || hours <= 0 || intervalMin <= 0) return;
        thresholdHours = hours;
        alertSink = sink != null ? sink : msg -> {};
        active = true;
        loadState();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OfflineMonitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runCheck, intervalMin, intervalMin, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
    }

    public boolean isActive()       { return active; }
    public int getThresholdHours()  { return thresholdHours; }

    void reset() { tracker.clear(); }

    void setStateFile(Path file) { stateFile = file; }

    void loadState() { tracker.restore(OfflineTrackerStore.load(resolveStateFile())); }

    void persistState() {
        try {
            OfflineTrackerStore.save(resolveStateFile(), tracker.snapshot());
        } catch (IOException e) {
            DebugLogger.getInstance().log("WARN", "[OfflineThresholdMonitor] Zustand nicht gespeichert: " + e);
        }
    }

    private Path resolveStateFile() {
        Path file = stateFile;
        return file != null ? file : StorageLocations.cache().resolve(STATE_FILE);
    }

    private void runCheck() {
        try {
            checkOnce(NetworkStore.getInstance().getAllHosts(),
                    HostAliveChecker::isAlive, System.currentTimeMillis());
            persistState();
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[OfflineThresholdMonitor] " + e);
        }
    }

    void checkOnce(List<HostResult> hosts, Predicate<String> alive, long nowMs) {
        Set<String> known = hosts.stream().map(h -> h.ip).collect(Collectors.toSet());
        tracker.retainOnly(known);
        Set<String> aliveIps = OfflineAliveProbe.aliveOf(known, alive);
        for (String ip : known) {
            tracker.register(ip, nowMs);
            if (aliveIps.contains(ip)) tracker.markSeen(ip, nowMs);
        }
        for (String ip : tracker.newlyOffline(nowMs, thresholdHours * MS_PER_HOUR)) {
            raiseAlert(ip, hosts, nowMs);
        }
    }

    private void raiseAlert(String ip, List<HostResult> hosts, long nowMs) {
        String name = hosts.stream().filter(h -> h.ip.equals(ip))
                .map(h -> h.hostname).findFirst().orElse(ip);
        long hours = tracker.offlineMs(ip, nowMs) / MS_PER_HOUR;
        String msg = "[OfflineMonitor] " + ip + " (" + name + ") seit " + hours + " h offline";
        DebugLogger.getInstance().log("WARN", msg);
        alertSink.accept(msg);
    }
}
