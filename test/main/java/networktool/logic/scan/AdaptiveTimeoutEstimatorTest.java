package main.java.networktool.logic.scan;

import main.java.networktool.logic.TimeoutConfig;
import main.java.networktool.logic.analysis.IcmpAnalyzer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveTimeoutEstimatorTest {

    AdaptiveTimeoutEstimator est = AdaptiveTimeoutEstimator.getInstance();

    @BeforeEach void setup() { est.reset(); est.setEnabled(false); }
    @AfterEach  void teardown() { est.reset(); est.setEnabled(false); }

    // ── Opt-in / disabled ─────────────────────────────────────────────────

    @Test void disabled_returnsDefault_evenWithHistory() {
        est.record("10.0.0.1", 5.0);
        est.record("10.0.0.2", 5.0);
        assertEquals(500, est.estimateTimeoutMs("10.0.0.3", 500));
    }

    @Test void isEnabled_reflectsSetEnabled() {
        assertFalse(est.isEnabled());
        est.setEnabled(true);
        assertTrue(est.isEnabled());
    }

    // ── Empty / insufficient history ─────────────────────────────────────

    @Test void estimate_emptyHistory_returnsDefault() {
        est.setEnabled(true);
        assertEquals(500, est.estimateTimeoutMs("10.0.0.1", 500));
    }

    @Test void estimate_singleSample_returnsDefault() {
        est.setEnabled(true);
        est.record("10.0.0.5", 20.0);
        assertEquals(500, est.estimateTimeoutMs("10.0.0.5", 500));
    }

    // ── Stable history → narrow, floored at default ──────────────────────

    @Test void estimate_stableHistory_atLeastDefault() {
        est.setEnabled(true);
        for (int i = 0; i < 10; i++) est.record("192.168.1." + i, 2.0);
        int result = est.estimateTimeoutMs("192.168.1.50", 500);
        assertTrue(result >= 500, "Timeout darf nie unter defaultMs fallen");
    }

    @Test void estimate_stableHistory_closeToMean_whenAboveDefault() {
        est.setEnabled(true);
        for (int i = 0; i < 10; i++) est.record("172.16.5." + i, 300.0);
        int result = est.estimateTimeoutMs("172.16.5.99", 100);
        assertTrue(result > 100 && result <= 200, "Erwartet Wert nahe Mittelwert, war " + result);
    }

    // ── Volatile history → capped at 2x default ──────────────────────────

    @Test void estimate_volatileHistory_cappedAtDouble() {
        est.setEnabled(true);
        est.record("10.1.1.1", 10.0);
        est.record("10.1.1.2", 5000.0);
        est.record("10.1.1.3", 1.0);
        int result = est.estimateTimeoutMs("10.1.1.4", 500);
        assertEquals(1000, result, "Muss bei hoher Streuung auf 2x defaultMs gedeckelt sein");
    }

    // ── Subnet isolation ──────────────────────────────────────────────────

    @Test void subnets_trackedIndependently() {
        est.setEnabled(true);
        for (int i = 0; i < 5; i++) est.record("10.0.0." + i, 400.0);
        for (int i = 0; i < 5; i++) est.record("10.0.1." + i, 5.0);

        int resultA = est.estimateTimeoutMs("10.0.0.99", 100);
        int resultB = est.estimateTimeoutMs("10.0.1.99", 100);

        assertTrue(resultA > resultB, "Unterschiedliche Subnetze müssen getrennte Historien haben");
    }

    @Test void subnet24_null_forInvalidIp() {
        assertNull(AdaptiveTimeoutEstimator.subnet24(null));
        assertNull(AdaptiveTimeoutEstimator.subnet24("no-dot"));
    }

    @Test void record_nullResult_doesNotThrow() {
        assertDoesNotThrow(() -> est.record("10.0.0.1", (IcmpAnalyzer.Result) null));
    }

    @Test void record_nullIp_doesNotThrow() {
        assertDoesNotThrow(() -> est.record(null, 5.0));
    }

    @Test void record_withIcmpResult_feedsHistory() {
        est.setEnabled(true);
        IcmpAnalyzer.Result r = new IcmpAnalyzer.Result(50, 10, 100, 5, 0, 5, 5);
        est.record("10.0.0.1", r);
        est.record("10.0.0.2", r);
        assertEquals(500, Math.max(500, est.estimateTimeoutMs("10.0.0.3", 500)));
    }

    @Test void reset_clearsHistory() {
        est.setEnabled(true);
        est.record("10.0.0.1", 400.0);
        est.record("10.0.0.2", 400.0);
        est.reset();
        assertEquals(100, est.estimateTimeoutMs("10.0.0.3", 100));
    }

    // ── No coupling to security-critical static timeouts ──────────────────

    @Test void doesNotAffect_securityRelatedStaticTimeouts() {
        int icmpBefore = TimeoutConfig.ICMP_REACHABLE_MS;
        int tcpBefore  = TimeoutConfig.TCP_PROBE_MS;

        est.setEnabled(true);
        est.record("10.0.0.1", 999.0);
        est.record("10.0.0.2", 1.0);
        est.estimateTimeoutMs("10.0.0.3", 500);

        assertEquals(icmpBefore, TimeoutConfig.ICMP_REACHABLE_MS,
                "ArpMonitor/SecurityMonitor-relevante Timeouts dürfen unberührt bleiben");
        assertEquals(tcpBefore, TimeoutConfig.TCP_PROBE_MS);
    }
}