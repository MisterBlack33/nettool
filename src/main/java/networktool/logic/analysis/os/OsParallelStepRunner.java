package main.java.networktool.logic.analysis.os;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Führt mehrere unabhängige OS-Erkennungsschritte parallel aus und liefert
 * die Signatur mit dem höchsten Score. Ersetzt sequenzielle Ketten wie
 * "erster Treffer ≥ Schwelle → sofort zurück" durch gesammeltes Warten auf
 * alle Futures innerhalb einer Sammelfrist — keine zusätzlichen Requests,
 * nur andere Reihenfolge/Nebenläufigkeit.
 */
final class OsParallelStepRunner {

    private OsParallelStepRunner() {}

    record NamedStep(String name, Supplier<OsSignature> supplier) {}

    /**
     * @param steps     parallel auszuführende Schritte
     * @param timeoutMs Sammelfrist für alle Schritte zusammen
     * @return beste gefundene Signatur, oder {@code null}
     */
    static OsSignature runParallel(List<NamedStep> steps, long timeoutMs) {
        if (steps.isEmpty()) return null;

        try (ExecutorService exec = Executors.newFixedThreadPool(steps.size())) {
            List<Future<OsSignature>> futures = submitAll(exec, steps);
            return collectBest(futures, timeoutMs);
        }
    }

    private static List<Future<OsSignature>> submitAll(ExecutorService exec, List<NamedStep> steps) {
        List<Future<OsSignature>> futures = new ArrayList<>(steps.size());
        for (NamedStep step : steps) {
            futures.add(exec.submit(() -> OsDetectionStepRunner.safeCall(step.name(), step.supplier())));
        }
        return futures;
    }

    private static OsSignature collectBest(List<Future<OsSignature>> futures, long timeoutMs) {
        OsSignature best = null;
        long deadline = System.currentTimeMillis() + timeoutMs;
        for (Future<OsSignature> f : futures) {
            long remaining = Math.max(0, deadline - System.currentTimeMillis());
            try {
                best = OsSignature.best(best, f.get(remaining, TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                f.cancel(true);
            }
        }
        return best;
    }
}