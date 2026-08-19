package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OsParallelStepRunnerTest {

    @Test void runParallel_empty_returnsNull() {
        assertNull(OsParallelStepRunner.runParallel(List.of(), 100));
    }

    @Test void runParallel_picksHighestScore() {
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("low",  () -> OsSignature.of("Linux", 40, "x")),
                new OsParallelStepRunner.NamedStep("high", () -> OsSignature.of("Windows", 90, "y")),
                new OsParallelStepRunner.NamedStep("null", () -> null)
        );
        OsSignature best = OsParallelStepRunner.runParallel(steps, 1000);
        assertNotNull(best);
        assertEquals("Windows", best.os);
    }

    @Test void runParallel_allNull_returnsNull() {
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("a", () -> null),
                new OsParallelStepRunner.NamedStep("b", () -> null)
        );
        assertNull(OsParallelStepRunner.runParallel(steps, 500));
    }

    @Test void runParallel_throwingStep_ignoredNotThrown() {
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("boom", () -> { throw new RuntimeException("x"); }),
                new OsParallelStepRunner.NamedStep("ok",   () -> OsSignature.of("Linux", 55, "y"))
        );
        OsSignature best = OsParallelStepRunner.runParallel(steps, 1000);
        assertNotNull(best);
        assertEquals("Linux", best.os);
    }

    @Test void runParallel_slowStep_doesNotBlockOthers() {
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("fast", () -> OsSignature.of("Linux", 60, "x")),
                new OsParallelStepRunner.NamedStep("slow", OsParallelStepRunnerTest::sleep3s)
        );
        long start = System.currentTimeMillis();
        OsSignature best = OsParallelStepRunner.runParallel(steps, 200);
        long elapsed = System.currentTimeMillis() - start;
        assertNotNull(best);
        assertEquals("Linux", best.os);
        assertTrue(elapsed < 2000, "sollte nicht auf den langsamen Schritt warten: " + elapsed);
    }

    @Test void runParallel_fasterThanSequential_whenAllSlow() {
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("s1", () -> sleepAndReturn(150)),
                new OsParallelStepRunner.NamedStep("s2", () -> sleepAndReturn(150)),
                new OsParallelStepRunner.NamedStep("s3", () -> sleepAndReturn(150))
        );
        long start = System.currentTimeMillis();
        OsParallelStepRunner.runParallel(steps, 1000);
        long elapsed = System.currentTimeMillis() - start;
        // sequenziell wäre >= 450ms, parallel deutlich darunter
        assertTrue(elapsed < 400, "Parallele Ausführung sollte schneller als sequenziell sein: " + elapsed);
    }

    @Test void runParallel_noThreadLeak() {
        int before = Thread.activeCount();
        var steps = List.of(
                new OsParallelStepRunner.NamedStep("a", () -> OsSignature.of("Linux", 50, "x")),
                new OsParallelStepRunner.NamedStep("b", () -> OsSignature.of("Windows", 60, "y"))
        );
        for (int i = 0; i < 5; i++) OsParallelStepRunner.runParallel(steps, 500);
        sleepQuiet(200); // Pool-Threads Zeit zum Beenden geben
        int after = Thread.activeCount();
        assertTrue(after - before < 10,
                "Thread-Anzahl sollte nicht anwachsen: before=" + before + " after=" + after);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static OsSignature sleep3s() {
        sleepQuiet(3000);
        return OsSignature.of("Windows", 90, "y");
    }

    private static OsSignature sleepAndReturn(long ms) {
        sleepQuiet(ms);
        return OsSignature.of("Linux", 50, "test");
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}