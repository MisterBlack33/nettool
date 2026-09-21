package main.java.networktool.logic.analysis.security;

import networktool.util.PollHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PeriodicJobTest {

    private final CountDownLatch ran = new CountDownLatch(1);
    private final PeriodicJob job = new PeriodicJob("test", ran::countDown);

    @AfterEach void cleanup() { job.stop(); }

    @Test void start_runsTaskImmediately() throws Exception {
        assertTrue(job.start(3600));
        assertTrue(ran.await(2, TimeUnit.SECONDS));
    }

    @Test void start_setsActive() {
        job.start(3600);
        assertTrue(job.isActive());
    }

    @Test void startTwice_secondIgnored() {
        assertTrue(job.start(3600));
        assertFalse(job.start(3600));
    }

    @Test void start_nonPositiveInterval_rejected() {
        assertFalse(job.start(0));
        assertFalse(job.isActive());
    }

    @Test void stop_clearsActive() {
        job.start(3600);
        assertTrue(job.stop());
        assertFalse(job.isActive());
    }

    @Test void stop_whenInactive_returnsFalse() {
        assertFalse(job.stop());
    }

    @Test void throwingTask_doesNotKillScheduler() {
        int[] runs = {0};
        PeriodicJob failing = new PeriodicJob("fail", () -> { runs[0]++; throw new IllegalStateException("x"); });
        failing.start(1);
        assertTrue(PollHelper.waitFor(() -> runs[0] >= 2, 3500));
        failing.stop();
    }
}
