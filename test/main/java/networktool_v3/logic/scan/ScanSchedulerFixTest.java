package main.java.networktool_v3.logic.scan;

import main.java.networktool.logic.scan.ScanScheduler;
import main.java.networktool.model.ScanProfile;
import main.java.networktool.storage.ScanProfileStore;
import main.java.networktool.storage.TestConstants;
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

    @Test void isRunning_afterStart() throws InterruptedException {
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

    @Test void stopAll_clearsAll() throws InterruptedException {
        sched.start(PROFILE, 999, "");
        Thread.sleep(50);
        sched.stopAll();
        assertTrue(sched.getRunning().isEmpty());
    }

    @Test void getRunning_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> sched.getRunning().add("test"));
    }
}