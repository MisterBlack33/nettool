package networktool.logic.scan;

import networktool.model.ScanProfile;
import networktool.storage.ScanProfileStore;
import networktool.storage.TestConstants;
import networktool.util.PollHelper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ScanSchedulerFixTest {

    final String PROFILE = TestConstants.PROFILE_SCHED_FIX;  // "__junit__sched_fix"
    ScanScheduler sched = ScanScheduler.getInstance();

    @BeforeEach void setup() {
        ScanProfile p = new ScanProfile(PROFILE);
        p.autoSave = true;
        p.category = TestConstants.NET_STANDARD;
        ScanProfileStore.getInstance().save(p);
    }

    @AfterEach void cleanup() {
        sched.stopAll();
        ScanProfileStore.getInstance().delete(PROFILE);
    }

    @Test void start_doesNotThrow()                   { assertDoesNotThrow(() -> sched.start(PROFILE, 999, "")); }

    @Test void isRunning_afterStart() {
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

    @Test void stopAll_clearsAll() {
        sched.start(PROFILE, 999, "");
        sched.stopAll();
        assertTrue(PollHelper.waitFor(() -> sched.getRunning().isEmpty(), 2000),
                "Alle Scanner sollten innerhalb von 2 Sekunden gestoppt sein");
    }

    @Test void getRunning_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> sched.getRunning().add("test"));
    }
}