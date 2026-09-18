package main.java.networktool.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Property-artige Tests (JUnit5-Parametrisierung statt zusätzlicher PBT-Bibliothek). */
class CIDRUtilsPropertyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "1.2.3.4", "10.0.0.1", "192.168.1.1", "255.255.255.255"})
    void ipToInt_intToIp_isRoundtrip(String ip) {
        assertEquals(ip, CIDRUtils.intToIp(CIDRUtils.ipToInt(ip)));
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 16, 20, 24, 28, 30})
    void getAllIPs_countMatchesPrefixFormula(int prefix) {
        String cidr = "10.0.0.0/" + prefix;
        int expected = Math.max(0, (1 << (32 - prefix)) - 2);
        assertEquals(expected, CIDRUtils.getAllIPs(cidr).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 20, 24, 28})
    void getSubnet24Prefixes_countIsPowerOfTwo(int prefix) {
        int expected = prefix >= 24 ? 1 : 1 << (24 - prefix);
        assertEquals(expected, CIDRUtils.getSubnet24Prefixes("10.0.0.0/" + prefix).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.5", "172.16.5.9", "192.168.100.200"})
    void ipToInt_isMonotonicWithLastOctet(String ip) {
        String[] parts = ip.split("\\.");
        String next = parts[0] + "." + parts[1] + "." + parts[2] + "." + (Integer.parseInt(parts[3]) < 254 ? Integer.parseInt(parts[3]) + 1 : parts[3]);
        assertTrue(CIDRUtils.ipToInt(ip) <= CIDRUtils.ipToInt(next));
    }
}
