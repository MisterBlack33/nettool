package main.java.networktool.logic.scan.schedule;

import networktool.util.PollHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workstream B: Statusübergänge von {@link ScanScheduler}, die von den
 * bestehenden Tests (ScanSchedulerPackageTest, ScanSchedulerFixTest) nicht
 * abgedeckt werden — insbesondere der Selbst-Stopp bei einem Profil, das nie
 * gespeichert oder zwischenzeitlich gelöscht wurde.
 */
@Tag("slow")
class ScanSchedulerLifecycleRegressionTest {

    private static final String GHOST_PROFILE = "__junit__ws_b_ghost_profile__";
    private static final String RESTART_PROFILE = "__junit__ws_b_restart_profile__";

    ScanScheduler sched = ScanScheduler.getInstance();

    @AfterEach
    void cleanup() {
        sched.stop(GHOST_PROFILE);
        sched.stop(RESTART_PROFILE);
    }

    // ── Selbst-Stopp bei unbekanntem Profil ──────────────────────────────

    @Test
    void start_unknownProfile_selfStopsWithoutThrowing() {
        // Kein ScanProfileStore-Eintrag für GHOST_PROFILE vorhanden.
        // runScheduledScan() muss den Fehlpfad "opt.isEmpty() → stop()" nehmen,
        // statt eine NoSuchElementException zu werfen oder dauerhaft aktiv zu bleiben.
        assertDoesNotThrow(() -> sched.start(GHOST_PROFILE, 1, ""));
        assertTrue(PollHelper.waitFor(() -> !sched.isRunning(GHOST_PROFILE), 3000),
                "Scheduler sollte sich bei unbekanntem Profil selbst stoppen");
    }

    // ── Schnelles Neustarten ersetzt statt zu verdoppeln ─────────────────

    @Test
    void rapidRestart_neverLeavesDuplicateSchedule() {
        // start() ruft intern stop() vor der Neuplanung auf; mehrfaches,
        // schnelles Neustarten desselben (auch unbekannten) Profils darf nie
        // zu mehr als einem aktiven Eintrag in getRunning() führen.
        for (int i = 0; i < 5; i++) {
            sched.start(GHOST_PROFILE, 999, "");
        }
        long count = sched.getRunning().stream().filter(GHOST_PROFILE::equals).count();
        assertTrue(count <= 1, "Erwartet höchstens 1 aktiven Eintrag, war " + count);
    }

    // ── stop() auf bereits selbst-gestopptes Profil bleibt idempotent ────

    @Test
    void stop_afterSelfStop_doesNotThrow() {
        sched.start(GHOST_PROFILE, 1, "");
        PollHelper.waitFor(() -> !sched.isRunning(GHOST_PROFILE), 3000);
        assertDoesNotThrow(() -> sched.stop(GHOST_PROFILE));
    }

    // ── getRunning() bleibt über mehrere Profile hinweg konsistent ───────

    @Test
    void multipleGhostProfiles_independentLifecycles() {
        sched.start(GHOST_PROFILE, 1, "");
        sched.start(RESTART_PROFILE, 999, "");
        assertTrue(PollHelper.waitFor(() -> !sched.isRunning(GHOST_PROFILE), 3000));
        // RESTART_PROFILE ist ebenfalls unbekannt und wird beim ersten Lauf
        // ebenso automatisch gestoppt — beide Profile dürfen sich dabei nicht
        // gegenseitig beeinflussen.
        assertTrue(PollHelper.waitFor(() -> !sched.isRunning(RESTART_PROFILE), 3000));
    }
}
