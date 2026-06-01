package main.java.networktool_v3.logic.scan;

import main.java.networktool.logic.scan.ScanScheduler;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.storage.ScanProfileStore;
import main.java.networktool.storage.TestConstants;
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

    @Test void start_knownProfile_isRunning() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(100);
        assertTrue(sched.isRunning(PROFILE));
    }

    @Test void stop_afterStart_notRunning() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(100);
        sched.stop(PROFILE);
        assertFalse(sched.isRunning(PROFILE));
    }

    @Test void getRunning_containsStarted() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(100);
        assertTrue(sched.getRunning().contains(PROFILE));
    }

    @Test void startTwice_replacesExisting() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(50);
        sched.start(PROFILE, 999, "");
        Thread.sleep(50);
        assertEquals(1, sched.getRunning().stream().filter(PROFILE::equals).count());
    }

    @Test void stopAll_clearsAll() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(50);
        sched.stopAll();
        assertTrue(sched.getRunning().isEmpty());
    }
}