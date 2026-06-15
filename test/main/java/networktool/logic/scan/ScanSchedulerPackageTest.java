package main.java.networktool.logic.scan;

import main.java.networktool.model.ScanProfile;
import main.java.networktool.storage.ScanProfileStore;
import main.java.networktool.storage.TestConstants;
import networktool.util.PollHelper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ScanSchedulerPackageTest {

    final String PROFILE = TestConstants.PROFILE_SCHED;  // "__junit__sched"
    ScanScheduler sched = ScanScheduler.getInstance();

    @BeforeEach void setup() {
        ScanProfileStore.getInstance().save(new ScanProfile(PROFILE));
    }

    @AfterEach void cleanup() {
        sched.stopAll();
        ScanProfileStore.getInstance().delete(PROFILE);
    }

    @Test void start_knownProfile_isRunning() {
        sched.start(PROFILE, 999, "");
        assertTrue(PollHelper.waitFor(() -> sched.isRunning(PROFILE), 2000),
                "Scanner sollte innerhalb von 2 Sekunden laufen");
    }

    @Test void stop_afterStart_notRunning() {
        sched.start(PROFILE, 999, "");
        assertTrue(PollHelper.waitFor(() -> sched.isRunning(PROFILE), 2000));
        sched.stop(PROFILE);
        assertTrue(PollHelper.waitFor(() -> !sched.isRunning(PROFILE), 2000),
                "Scanner sollte innerhalb von 2 Sekunden gestoppt sein");
    }

    @Test void getRunning_containsStarted() {
        sched.start(PROFILE, 999, "");
        assertTrue(PollHelper.waitFor(() -> sched.getRunning().contains(PROFILE), 2000),
                "Started Scanner sollte in getRunning() enthalten sein");
    }

    @Test void startTwice_replacesExisting() {
        sched.start(PROFILE, 999, "");
        sched.start(PROFILE, 999, "");
        assertTrue(PollHelper.waitFor(() ->
                sched.getRunning().stream().filter(PROFILE::equals).count() == 1, 2000),
                "Sollte nur noch 1 Instanz des Profils gibt");
    }

    @Test void stopAll_clearsAll() {
        sched.start(PROFILE, 999, "");
        sched.stopAll();
        assertTrue(PollHelper.waitFor(() -> sched.getRunning().isEmpty(), 2000),
                "getRunning() sollte leer sein nach stopAll()");
    }
}