package main.java.networktool.logic.analysis.security;

import main.java.networktool.logging.DebugLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Gemeinsamer Daemon-Scheduler für periodische Security-Jobs. */
final class PeriodicJob {

    private final String   name;
    private final Runnable task;
    private ScheduledExecutorService scheduler;

    PeriodicJob(String name, Runnable task) {
        this.name = name;
        this.task = task;
    }

    synchronized boolean start(int intervalSec) {
        if (scheduler != null || intervalSec <= 0) return false;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runSafely, 0, intervalSec, TimeUnit.SECONDS);
        return true;
    }

    synchronized boolean stop() {
        if (scheduler == null) return false;
        scheduler.shutdownNow();
        scheduler = null;
        return true;
    }

    synchronized boolean isActive() { return scheduler != null; }

    /** Eine Exception würde sonst alle Folgeläufe des Schedulers still beenden. */
    private void runSafely() {
        try {
            task.run();
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[" + name + "] " + e);
        }
    }
}
