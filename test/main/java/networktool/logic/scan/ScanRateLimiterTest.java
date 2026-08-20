package main.java.networktool.logic.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanRateLimiterTest {

    @Test
    void acquire_burstAllowsImmediateTokens() {
        ScanRateLimiter limiter = new ScanRateLimiter(10, 10);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) assertTrue(limiter.acquire(200));
        assertTrue(System.currentTimeMillis() - start < 150);
    }

    @Test
    void acquire_respectsConfiguredRate_afterBurstDrained() {
        ScanRateLimiter limiter = new ScanRateLimiter(50, 5);
        for (int i = 0; i < 5; i++) assertTrue(limiter.acquire(200)); // Burst leeren

        int count = 20;
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) assertTrue(limiter.acquire(3000));
        long elapsed = System.currentTimeMillis() - start;

        double expectedMs = count / 50.0 * 1000; // 400ms
        assertTrue(elapsed >= expectedMs * 0.5,
                "Rate wurde nicht eingehalten, elapsed=" + elapsed + "ms");
    }

    @Test
    void acquire_timeout_returnsFalseWhenExhausted() {
        ScanRateLimiter limiter = new ScanRateLimiter(1, 1);
        assertTrue(limiter.acquire(50));
        assertFalse(limiter.acquire(50)); // nächstes Token braucht ~1000ms
    }

    @Test
    void acquire_recoversAfterPause() throws InterruptedException {
        ScanRateLimiter limiter = new ScanRateLimiter(20, 2);
        assertTrue(limiter.acquire(100));
        assertTrue(limiter.acquire(100));
        assertFalse(limiter.acquire(10)); // Burst erschöpft

        Thread.sleep(200); // ~4 Tokens bei 20/s nachgefüllt
        assertTrue(limiter.acquire(100), "Limiter sollte sich nach Pause erholt haben");
    }

    @Test
    void acquire_doesNotBlockForeverOnBurst() {
        ScanRateLimiter limiter = new ScanRateLimiter(5, 100);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) assertTrue(limiter.acquire(500));
        assertTrue(System.currentTimeMillis() - start < 500);
    }

    @Test
    void setRate_updatesRateAndBurst() {
        ScanRateLimiter limiter = new ScanRateLimiter(10, 10);
        limiter.setRate(100, 20);
        assertEquals(100.0, limiter.getRatePerSecond(), 0.01);
        assertEquals(20.0,  limiter.getBurstCapacity(), 0.01);
    }

    @Test
    void setRate_clampsToMinimumOne() {
        ScanRateLimiter limiter = new ScanRateLimiter(10, 10);
        limiter.setRate(-5, 0);
        assertEquals(1.0, limiter.getRatePerSecond(), 0.01);
        assertEquals(1.0, limiter.getBurstCapacity(), 0.01);
    }

    @Test
    void getInstance_returnsSingleton() {
        assertSame(ScanRateLimiter.getInstance(), ScanRateLimiter.getInstance());
    }

    @Test
    void acquire_defaultTimeout_doesNotThrow() {
        ScanRateLimiter limiter = new ScanRateLimiter(1000, 1000);
        assertDoesNotThrow(() -> limiter.acquire());
    }

    @Test
    void acquire_interrupted_returnsFalse() throws InterruptedException {
        ScanRateLimiter limiter = new ScanRateLimiter(1, 1);
        limiter.acquire(50); // Burst leeren

        Thread t = new Thread(() -> assertFalse(limiter.acquire(5000)));
        t.start();
        Thread.sleep(50);
        t.interrupt();
        t.join(1000);
        assertFalse(t.isAlive());
    }
}