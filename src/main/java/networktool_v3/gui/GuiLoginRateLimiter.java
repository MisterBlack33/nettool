package main.java.networktool_v3.gui;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate-Limiter für GUI-Login-Versuche.
 * Max. MAX_ATTEMPTS Versuche, dann LOCKOUT_MS Sperre.
 */
public final class GuiLoginRateLimiter {

    private GuiLoginRateLimiter() {}

    public static final int  MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_MS   = 30_000L;

    private static final AtomicInteger attempts   = new AtomicInteger(0);
    private static final AtomicLong    lockedUntil = new AtomicLong(0);

    public static boolean isLocked() {
        return System.currentTimeMillis() < lockedUntil.get();
    }

    /** Verbleibende Sperrzeit in Sekunden (0 wenn nicht gesperrt). */
    public static long remainingSeconds() {
        long rem = lockedUntil.get() - System.currentTimeMillis();
        return rem > 0 ? (rem / 1000) + 1 : 0;
    }

    /** Registriert einen Fehlversuch. Gibt true zurück wenn jetzt gesperrt. */
    public static boolean recordFailure() {
        int count = attempts.incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.set(System.currentTimeMillis() + LOCKOUT_MS);
            attempts.set(0);
            return true;
        }
        return false;
    }

    public static void recordSuccess() {
        attempts.set(0);
        lockedUntil.set(0);
    }

    public static void reset() {
        attempts.set(0);
        lockedUntil.set(0);
    }

    public static int getAttempts() { return attempts.get(); }
}