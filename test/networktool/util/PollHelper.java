package networktool.util;

import java.util.function.BooleanSupplier;
import java.util.concurrent.TimeUnit;

/**
 * Hilfsklasse für zuverlässiges Polling in Tests.
 * Ersetzt Thread.sleep mit adaptivem Polling.
 */
public final class PollHelper {

    private PollHelper() {}

    public static boolean waitFor(BooleanSupplier condition, long timeoutMs) {
        return waitFor(condition, timeoutMs, 50);
    }

    public static boolean waitFor(BooleanSupplier condition, long timeoutMs, long pollIntervalMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(Math.min(pollIntervalMs, deadline - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static boolean waitFor(BooleanSupplier condition, long timeout, TimeUnit unit) {
        return waitFor(condition, unit.toMillis(timeout));
    }
}

