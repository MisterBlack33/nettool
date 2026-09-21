package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TlsCertSchedulerTest {

    SecurityFindingsCollector sink = SecurityFindingsCollector.getInstance();

    private static SecurityFinding f(String ip, SecurityFinding.Severity s) {
        return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, s, "d");
    }

    private TlsCertScheduler scheduler(List<String> ips) {
        return new TlsCertScheduler(() -> ips, ip -> switch (ip) {
            case "1.1.1.1" -> Optional.of(f(ip, SecurityFinding.Severity.WARN));
            case "2.2.2.2" -> Optional.of(f(ip, SecurityFinding.Severity.INFO));
            default        -> Optional.empty();
        }, sink);
    }

    @BeforeEach @AfterEach void clear() { sink.clear(); }

    @Test void runOnce_warnStored_infoAndEmptyIgnored() {
        scheduler(List.of("1.1.1.1", "2.2.2.2", "3.3.3.3")).runOnce();
        assertEquals(1, sink.getAll().size());
        assertEquals("1.1.1.1", sink.getAll().get(0).ip());
    }

    @Test void runOnce_twice_noDuplicate() {
        TlsCertScheduler s = scheduler(List.of("1.1.1.1"));
        s.runOnce();
        s.runOnce();
        assertEquals(1, sink.getAll().size());
    }

    @Test void startStop_lifecycle() {
        TlsCertScheduler s = scheduler(List.of());
        assertTrue(s.start(3600));
        assertTrue(s.isActive());
        assertFalse(s.start(3600));
        assertTrue(s.stop());
        assertFalse(s.stop());
    }

    @Test void savedTlsHosts_notNull() {
        assertNotNull(TlsCertScheduler.savedTlsHosts());
    }

    @Test void getInstance_isSingleton() {
        assertSame(TlsCertScheduler.getInstance(), TlsCertScheduler.getInstance());
    }

    @Test void runOnce_dailyChangingDetail_noDuplicate() {
        int[] day = {5};
        TlsCertScheduler s = new TlsCertScheduler(() -> List.of("1.1.1.1"),
                ip -> Optional.of(new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT,
                        SecurityFinding.Severity.WARN, "Zertifikat läuft in " + day[0]-- + " Tag(en) ab")), sink);
        s.runOnce();
        s.runOnce();
        assertEquals(1, sink.getAll().size());
    }

    @Test void stabilize_expired_and_selfSigned() {
        SecurityFinding expired = new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.CRITICAL, "Zertifikat abgelaufen seit 3 Tag(en)");
        assertEquals("Zertifikat abgelaufen", TlsCertScheduler.stabilize(expired).detail());
        SecurityFinding self = new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.WARN, "Self-signed Zertifikat: CN=x");
        assertSame(self, TlsCertScheduler.stabilize(self));
    }
}
