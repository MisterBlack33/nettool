package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests für OsProbeUdp: NetBIOS, mDNS, SNMP. */
class OsProbeUdpTest {

    // ── probeNetBios ──────────────────────────────────────────────────────

    @Test
    void probeNetBios_unreachable_returnsNull() {
        // Unreachable IP: kein Absturz, kein Hang
        OsSignature r = OsProbeUdp.probeNetBios("192.0.2.1");
        assertNull(r);
    }

    @Test
    void probeNetBios_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> OsProbeUdp.probeNetBios("127.0.0.1"));
    }

    // ── probeMdns ─────────────────────────────────────────────────────────

    @Test
    void probeMdns_unreachable_returnsNull() {
        OsSignature r = OsProbeUdp.probeMdns("192.0.2.2");
        assertNull(r);
    }

    @Test
    void probeMdns_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> OsProbeUdp.probeMdns("127.0.0.1"));
    }

    // ── probeSnmp ─────────────────────────────────────────────────────────

    @Test
    void probeSnmp_unreachable_returnsNull() {
        OsSignature r = OsProbeUdp.probeSnmp("192.0.2.3");
        assertNull(r);
    }

    @Test
    void probeSnmp_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> OsProbeUdp.probeSnmp("127.0.0.1"));
    }

    // ── probe (kombiniert) ────────────────────────────────────────────────

    @Test
    void probe_unreachable_returnsNull() {
        OsSignature r = OsProbeUdp.probe("192.0.2.4");
        assertNull(r);
    }

    @Test
    void probe_doesNotHang() {
        long start = System.currentTimeMillis();
        OsProbeUdp.probe("192.0.2.5");
        long elapsed = System.currentTimeMillis() - start;
        // 3 UDP-Probes à 800ms Timeout — maximal ~3s
        assertTrue(elapsed < 4000, "probe() hing zu lange: " + elapsed + "ms");
    }
}