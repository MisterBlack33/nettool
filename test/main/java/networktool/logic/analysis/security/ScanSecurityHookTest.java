package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScanSecurityHookTest {

    SecurityFindingsCollector sink = SecurityFindingsCollector.getInstance();
    ScanSecurityHook hook = new ScanSecurityHook(sink,
            (ip, port) -> port == 21 ? "220 vsftpd 2.3.4 ready" : "harmless");

    @BeforeEach @AfterEach void clear() { sink.clear(); }

    @Test void disabledByDefault_noFindings() {
        hook.onHost("10.0.0.5", Map.of(21, "offen"));
        assertTrue(sink.getAll().isEmpty());
    }

    @Test void enabled_vulnerableBanner_findingStored() {
        hook.setEnabled(true);
        hook.onHost("10.0.0.5", Map.of(21, "offen"));
        assertEquals(1, sink.getAll().size());
        assertEquals(SecurityFinding.Category.KNOWN_VULNERABLE, sink.getAll().get(0).category());
    }

    @Test void enabled_harmlessBanner_noFinding() {
        hook.setEnabled(true);
        hook.onHost("10.0.0.5", Map.of(80, "offen"));
        assertTrue(sink.getAll().isEmpty());
    }

    @Test void enabled_sameHostTwice_noDuplicate() {
        hook.setEnabled(true);
        hook.onHost("10.0.0.5", Map.of(21, "offen"));
        hook.onHost("10.0.0.5", Map.of(21, "offen"));
        assertEquals(1, sink.getAll().size());
    }

    @Test void enabled_nullPorts_doesNotThrow() {
        hook.setEnabled(true);
        assertDoesNotThrow(() -> hook.onHost("10.0.0.5", null));
    }

    @Test void setEnabled_reflected() {
        hook.setEnabled(true);
        assertTrue(hook.isEnabled());
        hook.setEnabled(false);
        assertFalse(hook.isEnabled());
    }

    @Test void getInstance_isSingleton() {
        assertSame(ScanSecurityHook.getInstance(), ScanSecurityHook.getInstance());
    }

    @Test void asyncExecutor_doesNotBlockCaller_andReportsLater() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        ScanSecurityHook async = new ScanSecurityHook(sink, (ip, port) -> {
            done.countDown();
            return "220 vsftpd 2.3.4 ready";
        }, r -> new Thread(r).start());
        async.setEnabled(true);
        async.onHost("10.0.0.7", Map.of(21, "offen"));
        assertTrue(done.await(3, TimeUnit.SECONDS));
        networktool.util.PollHelper.waitFor(() -> !sink.getAll().isEmpty(), 2000);
        assertEquals(1, sink.getAll().size());
    }

    @Test void bannerSourceThrows_doesNotPropagate() {
        ScanSecurityHook failing = new ScanSecurityHook(sink, (ip, port) -> { throw new IllegalStateException("x"); });
        failing.setEnabled(true);
        assertDoesNotThrow(() -> failing.onHost("10.0.0.8", Map.of(21, "offen")));
    }
}
