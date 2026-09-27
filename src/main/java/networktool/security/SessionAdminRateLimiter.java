package main.java.networktool.security;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Brute-Force-Schutz für {@link UserAuth#grantSessionAdmin}.
 *
 * Finding: Der reguläre Login ist bereits über GuiLoginRateLimiter gegen
 * Passwort-Raten geschützt, die "GET ADMIN"-Freischaltung (Session-Admin-
 * Override für den Standard-User) bisher nicht — das Admin-Passwort ließ
 * sich beliebig oft über den Sidebar-Dialog raten (CWE-307).
 */
final class SessionAdminRateLimiter {

    private SessionAdminRateLimiter() {}

    static final int  MAX_ATTEMPTS = 5;
    static final long LOCKOUT_MS   = 30_000L;

    private static final AtomicInteger attempts    = new AtomicInteger(0);
    private static final AtomicLong    lockedUntil = new AtomicLong(0);

    static boolean isLocked() {
        return System.currentTimeMillis() < lockedUntil.get();
    }

    static void recordFailure() {
        if (attempts.incrementAndGet() >= MAX_ATTEMPTS) {
            lockedUntil.set(System.currentTimeMillis() + LOCKOUT_MS);
            attempts.set(0);
        }
    }

    static void recordSuccess() { reset(); }

    static void reset() {
        attempts.set(0);
        lockedUntil.set(0);
    }
}
