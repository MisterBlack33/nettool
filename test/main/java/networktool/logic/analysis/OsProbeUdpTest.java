package main.java.networktool.logic.analysis;

import main.java.networktool.logic.analysis.OsProbeUdp;
import main.java.networktool.logic.analysis.OsSignature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests fÃ¼r OsProbeUdp: NetBIOS, mDNS, SNMP. */
class OsProbeUdpTest {

    // â”€â”€ probeNetBios â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ probeMdns â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void probeMdns_unreachable_returnsNull() {
        OsSignature r = OsProbeUdp.probeMdns("192.0.2.2");
        assertNull(r);
    }

    @Test
    void probeMdns_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> OsProbeUdp.probeMdns("127.0.0.1"));
    }

    // â”€â”€ probeSnmp â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void probeSnmp_unreachable_returnsNull() {
        OsSignature r = OsProbeUdp.probeSnmp("192.0.2.3");
        assertNull(r);
    }

    @Test
    void probeSnmp_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> OsProbeUdp.probeSnmp("127.0.0.1"));
    }

    // â”€â”€ probe (kombiniert) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        // 3 UDP-Probes Ã  800ms Timeout â€” maximal ~3s
        assertTrue(elapsed < 4000, "probe() hing zu lange: " + elapsed + "ms");
    }
}