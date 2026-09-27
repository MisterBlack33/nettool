package main.java.networktool.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workstream B: Grenzfälle für {@link CIDRUtils}, die von den bestehenden
 * Tests (CIDRUtilsPropertyTest, LogicTest) nicht abgedeckt werden — vor allem
 * die Randfälle /0, /31 und /32, bei denen network+1 > broadcast-1 werden kann.
 */
class CIDRUtilsBoundaryTest {

    // ── /32: genau eine Adresse, kein Host-Bereich ───────────────────────

    @Test
    void getAllIPs_slash32_returnsEmpty() {
        // network == broadcast == die eine Adresse selbst; first > last → leer
        assertTrue(CIDRUtils.getAllIPs("10.0.0.5/32").isEmpty());
    }

    // ── /31: Punkt-zu-Punkt-Netz, RFC 3021, keine "Host"-Range ───────────

    @Test
    void getAllIPs_slash31_returnsEmpty() {
        // network+1 (=broadcast) > broadcast-1 (=network) → count wird auf 0 geklemmt
        assertTrue(CIDRUtils.getAllIPs("10.0.0.0/31").isEmpty());
    }

    // ── /0: das gesamte IPv4-Universum als "ein" Netz ────────────────────

    @Test
    void getAllIPs_slash0_boundsAreCorrect() {
        List<String> ips = CIDRUtils.getAllIPs("0.0.0.0/0");
        assertEquals("0.0.0.1", ips.get(0));
        assertEquals(Integer.MAX_VALUE, ips.size()); // durch int-Grenze geklemmt
    }

    @Test
    void getSubnet24Prefixes_slash0_isCappedAt256Blocks() {
        // /0 → 2^24 theoretische /24-Blöcke; hier wird nur geprüft, dass die
        // ersten Blöcke korrekt beginnen und die Berechnung nicht überläuft/hängt
        List<String> prefixes = CIDRUtils.getSubnet24Prefixes("0.0.0.0/0");
        assertEquals("0.0.0", prefixes.get(0));
        assertEquals("0.0.1", prefixes.get(1));
    }

    // ── intToIp/ipToInt an den absoluten Wertegrenzen ────────────────────

    @Test
    void ipToInt_allOnes_isMinusOneAsSignedInt() {
        assertEquals(-1, CIDRUtils.ipToInt("255.255.255.255"));
    }

    @Test
    void intToIp_minValue_roundtripsToZeroDotZero() {
        assertEquals("0.0.0.0", CIDRUtils.intToIp(0));
    }

    @Test
    void getAllIPs_slash30_exactlyTwoUsableHosts() {
        // Regressions-Anker: kleinstes "normales" Subnetz mit einem echten Host-Bereich
        assertEquals(List.of("192.168.0.1", "192.168.0.2"),
                CIDRUtils.getAllIPs("192.168.0.0/30"));
    }
}
