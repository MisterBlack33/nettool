package main.java.networktool.logic.scan;

/**
 * Token-Bucket Rate-Limiter für neue Verbindungsversuche (ICMP/TCP-Connects).
 *
 * Drosselt nur den *Start* neuer Verbindungen (Ping-/Port-Sweeps) auf ein
 * konfigurierbares Paket/Sekunde-Ziel, um IDS/Firewall-Erkennung als
 * Angriffsmuster zu vermeiden. Bereits etablierte Verbindungen sind
 * unbeeinflusst — Zuverlässigkeit laufender Scans bleibt unverändert.
 *
 * Thread-sicher; Standard-Instanz über {@link #getInstance()}, für Tests
 * auch direkt mit eigener Rate instanzierbar.
 */
public final class ScanRateLimiter {

    /** Konservative Obergrenze — bestehende Bruttolast als Deckel, nicht als Ziel. */
    private static final double DEFAULT_RATE_PPS = 300.0;
    private static final double DEFAULT_BURST    = 60.0;

    private static final class Holder { static final ScanRateLimiter INSTANCE = new ScanRateLimiter(); }
    public static ScanRateLimiter getInstance() { return Holder.INSTANCE; }

    private volatile double ratePerSecond;
    private volatile double burstCapacity;
    private double tokens;
    private long   lastRefillNanos;

    private ScanRateLimiter() {
        this(DEFAULT_RATE_PPS, DEFAULT_BURST);
    }

    public ScanRateLimiter(double ratePerSecond, double burstCapacity) {
        this.ratePerSecond   = Math.max(1, ratePerSecond);
        this.burstCapacity   = Math.max(1, burstCapacity);
        this.tokens          = this.burstCapacity;
        this.lastRefillNanos = System.nanoTime();
    }

    /** Setzt die Rate zur Laufzeit (Produktionskonfiguration oder Tests). */
    public synchronized void setRate(double ratePerSecond, double burstCapacity) {
        this.ratePerSecond = Math.max(1, ratePerSecond);
        this.burstCapacity = Math.max(1, burstCapacity);
        this.tokens        = Math.min(this.tokens, this.burstCapacity);
    }

    public double getRatePerSecond()  { return ratePerSecond; }
    public double getBurstCapacity()  { return burstCapacity; }

    /**
     * Blockiert bis ein Token verfügbar ist (max. {@code maxWaitMs}), dann
     * wird ein Token verbraucht.
     *
     * @return true wenn ein Token verbraucht wurde, false bei Timeout/Interrupt
     */
    public synchronized boolean acquire(long maxWaitMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (true) {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return false;
            long neededMs = (long) Math.ceil((1.0 - tokens) / ratePerSecond * 1000);
            try {
                wait(Math.max(1, Math.min(neededMs, remaining)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /** Bequeme Variante mit 5s Standard-Timeout (verhindert Dauerblockade bei Fehlkonfiguration). */
    public boolean acquire() {
        return acquire(5000);
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSec = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSec <= 0) return;
        lastRefillNanos = now;
        tokens = Math.min(burstCapacity, tokens + elapsedSec * ratePerSecond);
    }
}