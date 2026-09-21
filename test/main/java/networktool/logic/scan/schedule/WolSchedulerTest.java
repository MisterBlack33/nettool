package main.java.networktool.logic.scan.schedule;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class WolSchedulerTest {

    private static final String MAC = "AA:BB:CC:DD:EE:FF";

    private final List<String> sent = new CopyOnWriteArrayList<>();
    private boolean senderResult = true;
    private final WolScheduler scheduler = new WolScheduler((mac, broadcast) -> {
        sent.add(mac + "@" + broadcast);
        return senderResult;
    });

    private static WolSchedule inSixHours() {
        return new WolSchedule(MAC, "192.168.1.255", LocalTime.now().plusHours(6));
    }

    @AfterEach void cleanup() { scheduler.stopAll(); }

    // ── delayUntil ────────────────────────────────────────────────────────

    @Test void delay_laterToday() {
        assertEquals(Duration.ofHours(2), WolScheduler.delayUntil(LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test void delay_alreadyPassed_wrapsToTomorrow() {
        assertEquals(Duration.ofHours(22), WolScheduler.delayUntil(LocalTime.of(12, 0), LocalTime.of(10, 0)));
    }

    @Test void delay_sameTime_waitsFullDay() {
        assertEquals(Duration.ofDays(1), WolScheduler.delayUntil(LocalTime.of(10, 0), LocalTime.of(10, 0)));
    }

    @Test void delay_underOneSecond_countsAsPassed() {
        Duration d = WolScheduler.delayUntil(LocalTime.of(10, 0), LocalTime.of(10, 0, 0, 500_000_000));
        assertTrue(d.compareTo(Duration.ofDays(1)) > 0);
    }

    @Test void delay_acrossMidnight() {
        assertEquals(Duration.ofMinutes(2), WolScheduler.delayUntil(LocalTime.of(23, 59), LocalTime.of(0, 1)));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Test void start_marksRunning() {
        scheduler.start("a", inSixHours());
        assertTrue(scheduler.isRunning("a"));
        assertTrue(scheduler.getRunning().contains("a"));
    }

    @Test void stop_removesEntry() {
        scheduler.start("a", inSixHours());
        scheduler.stop("a");
        assertFalse(scheduler.isRunning("a"));
    }

    @Test void stop_unknown_doesNotThrow() { assertDoesNotThrow(() -> scheduler.stop("ghost")); }

    @Test void start_sameNameTwice_keepsSingleEntry() {
        scheduler.start("a", inSixHours());
        scheduler.start("a", inSixHours());
        assertEquals(1, scheduler.getRunning().size());
    }

    @Test void stopAll_clearsEverything() {
        scheduler.start("a", inSixHours());
        scheduler.start("b", inSixHours());
        scheduler.stopAll();
        assertTrue(scheduler.getRunning().isEmpty());
    }

    @Test void getRunning_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> scheduler.getRunning().add("x"));
    }

    @Test void start_doesNotSendImmediately() {
        scheduler.start("a", inSixHours());
        assertTrue(sent.isEmpty());
    }

    // ── fire ──────────────────────────────────────────────────────────────

    @Test void fire_sendsToConfiguredTarget() {
        WolSchedule schedule = inSixHours();
        scheduler.fire("a", schedule);
        assertEquals(List.of(MAC + "@192.168.1.255"), sent);
    }

    @Test void fire_whileRunning_reschedules() {
        WolSchedule schedule = inSixHours();
        scheduler.start("a", schedule);
        scheduler.fire("a", schedule);
        assertTrue(scheduler.isRunning("a"));
    }

    @Test void fire_afterStop_doesNotReschedule() {
        scheduler.fire("gone", inSixHours());
        assertFalse(scheduler.isRunning("gone"));
    }

    @Test void fire_failedSend_doesNotThrow() {
        senderResult = false;
        assertDoesNotThrow(() -> scheduler.fire("a", inSixHours()));
    }

    @Test void getInstance_isSingleton() { assertSame(WolScheduler.getInstance(), WolScheduler.getInstance()); }

    @Test void fire_senderThrows_scheduleStaysActive() {
        WolScheduler failing = new WolScheduler((mac, broadcast) -> { throw new IllegalStateException("boom"); });
        try {
            failing.start("x", inSixHours());
            assertDoesNotThrow(() -> failing.fire("x", inSixHours()));
            assertTrue(failing.isRunning("x"));
        } finally {
            failing.stopAll();
        }
    }
}
