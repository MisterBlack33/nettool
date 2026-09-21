package main.java.networktool.logic.scan.schedule;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.analysis.probe.WakeOnLan;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

/**
 * Sendet Wake-on-LAN-Pakete täglich zu einer festen Uhrzeit. Jeder Lauf plant
 * den nächsten neu, damit Zeitumstellungen nicht zu Drift führen.
 */
public final class WolScheduler {

    private static final Duration ONE_DAY = Duration.ofDays(1);
    /** Ein Zieltermin, der kaum noch in der Zukunft liegt, gilt als verstrichen (verhindert Doppelversand). */
    private static final Duration MIN_DELAY = Duration.ofSeconds(1);

    private static final class Holder {
        static final WolScheduler INSTANCE = new WolScheduler(WakeOnLan::send);
    }

    public static WolScheduler getInstance() { return Holder.INSTANCE; }

    private final BiPredicate<String, String> sender;
    private final Map<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "WolScheduler");
        thread.setDaemon(true);
        return thread;
    });

    WolScheduler(BiPredicate<String, String> sender) {
        this.sender = sender;
    }

    public void start(String name, WolSchedule schedule) {
        stop(name);
        scheduleNext(name, schedule);
    }

    public void stop(String name) {
        ScheduledFuture<?> future = running.remove(name);
        if (future != null) future.cancel(false);
    }

    public void stopAll() {
        Set<String> names = Set.copyOf(running.keySet());
        names.forEach(this::stop);
    }

    public boolean isRunning(String name) { return running.containsKey(name); }

    public Set<String> getRunning() { return Collections.unmodifiableSet(running.keySet()); }

    /** Wartezeit bis zur nächsten Ausführung von {@code target}, nie unter {@link #MIN_DELAY}. */
    static Duration delayUntil(LocalTime now, LocalTime target) {
        Duration delay = Duration.between(now, target);
        while (delay.compareTo(MIN_DELAY) < 0) delay = delay.plus(ONE_DAY);
        return delay;
    }

    void fire(String name, WolSchedule schedule) {
        try {
            boolean sent = sender.test(schedule.mac(), schedule.broadcast());
            DebugLogger.getInstance().log(sent ? "INFO" : "WARN",
                    "[WolScheduler] " + name + (sent ? " gesendet" : " fehlgeschlagen"));
        } catch (RuntimeException e) {
            DebugLogger.getInstance().log("WARN", "[WolScheduler] " + name + ": " + e);
        } finally {
            if (running.containsKey(name)) scheduleNext(name, schedule);
        }
    }

    private void scheduleNext(String name, WolSchedule schedule) {
        long delayMs = delayUntil(LocalTime.now(), schedule.time()).toMillis();
        running.put(name, executor.schedule(() -> fire(name, schedule), delayMs, TimeUnit.MILLISECONDS));
    }
}
