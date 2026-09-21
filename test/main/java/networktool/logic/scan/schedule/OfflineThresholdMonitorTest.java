package main.java.networktool.logic.scan.schedule;

import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class OfflineThresholdMonitorTest {

    static final long H = OfflineThresholdMonitor.MS_PER_HOUR;
    OfflineThresholdMonitor m = OfflineThresholdMonitor.getInstance();
    final List<String> alerts = new CopyOnWriteArrayList<>();
    final List<HostResult> hosts = List.of(new HostResult("10.0.0.1", "srv", "Linux"));

    @BeforeEach void setup() {
        m.stop();
        m.reset();
        m.setStateFile(tmp.resolve("setup.tsv"));
        m.start(1, 60, alerts::add);
    }

    @AfterEach void teardown() {
        m.stop();
        m.reset();
    }

    @Test void offlineBelowThreshold_noAlert() {
        m.checkOnce(hosts, ip -> false, 0);
        m.checkOnce(hosts, ip -> false, H / 2);
        assertTrue(alerts.isEmpty());
    }

    @Test void offlineAtThreshold_singleAlertWithNameAndHours() {
        m.checkOnce(hosts, ip -> false, 0);
        m.checkOnce(hosts, ip -> false, 2 * H);
        m.checkOnce(hosts, ip -> false, 3 * H);
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("10.0.0.1"));
        assertTrue(alerts.get(0).contains("srv"));
        assertTrue(alerts.get(0).contains("2 h"));
    }

    @Test void aliveHost_neverAlerts() {
        m.checkOnce(hosts, ip -> true, 0);
        m.checkOnce(hosts, ip -> true, 5 * H);
        assertTrue(alerts.isEmpty());
    }

    @Test void hostReturns_thenOfflineAgain_alertsAgain() {
        m.checkOnce(hosts, ip -> false, 0);
        m.checkOnce(hosts, ip -> false, H);
        m.checkOnce(hosts, ip -> true, 2 * H);
        m.checkOnce(hosts, ip -> false, 3 * H);
        m.checkOnce(hosts, ip -> false, 4 * H);
        assertEquals(2, alerts.size());
    }

    @Test void removedHost_noAlert() {
        m.checkOnce(hosts, ip -> false, 0);
        m.checkOnce(List.of(), ip -> false, 2 * H);
        assertTrue(alerts.isEmpty());
    }

    @Test void start_invalidParams_notActive() {
        m.stop();
        m.start(0, 5, alerts::add);
        assertFalse(m.isActive());
        m.start(1, 0, alerts::add);
        assertFalse(m.isActive());
    }

    @Test void start_twice_keepsFirstThreshold() {
        m.start(9, 5, alerts::add);
        assertEquals(1, m.getThresholdHours());
    }

    @Test void stop_clearsActive_andIsIdempotent() {
        assertTrue(m.isActive());
        m.stop();
        assertFalse(m.isActive());
        assertDoesNotThrow(m::stop);
    }

    @Test void nullSink_doesNotThrow() {
        m.stop();
        m.start(1, 60, null);
        m.checkOnce(hosts, ip -> false, 0);
        assertDoesNotThrow(() -> m.checkOnce(hosts, ip -> false, 2 * H));
    }

    @TempDir Path tmp;

    @Test void persistedState_survivesRestart() throws IOException {
        m.setStateFile(tmp.resolve("state.tsv"));
        m.checkOnce(hosts, ip -> false, 0);
        m.persistState();
        m.reset();
        m.loadState();
        m.checkOnce(hosts, ip -> false, 2 * H);
        assertEquals(1, alerts.size());
    }

    @Test void loadState_missingFile_doesNotThrow() {
        m.setStateFile(tmp.resolve("none.tsv"));
        assertDoesNotThrow(m::loadState);
    }

    @Test void persistState_unwritablePath_doesNotThrow() throws IOException {
        Path file = java.nio.file.Files.createFile(tmp.resolve("plain"));
        m.setStateFile(file.resolve("child.tsv"));
        assertDoesNotThrow(m::persistState);
    }
}
