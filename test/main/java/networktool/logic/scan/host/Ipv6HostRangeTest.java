package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Ipv6HostRangeTest {

    @Test void expand_slash126_threeHosts() {
        assertEquals(List.of("2001:db8:0:0:0:0:0:1", "2001:db8:0:0:0:0:0:2", "2001:db8:0:0:0:0:0:3"),
                Ipv6HostRange.expand("2001:db8::/126"));
    }

    @Test void expand_slash128_singleAddress() {
        assertEquals(List.of("2001:db8:0:0:0:0:0:9"), Ipv6HostRange.expand("2001:db8::9/128"));
    }

    @Test void expand_slash127_oneUsableHost() {
        assertEquals(1, Ipv6HostRange.expand("2001:db8::/127").size());
    }

    @Test void expand_slash64_isCapped() {
        List<String> hosts = Ipv6HostRange.expand("2001:db8::/64");
        assertEquals(Ipv6HostRange.MAX_HOSTS, hosts.size());
        assertEquals("2001:db8:0:0:0:0:0:1", hosts.get(0));
        assertEquals("2001:db8:0:0:0:0:0:100", hosts.get(hosts.size() - 1));
    }

    @Test void expand_unalignedAddress_startsAtNetwork() {
        assertEquals("2001:db8:0:0:0:0:0:5", Ipv6HostRange.expand("2001:db8::5/126").get(0));
    }

    @Test void expand_invalidCidr_throws() {
        assertThrows(IllegalArgumentException.class, () -> Ipv6HostRange.expand("nonsense"));
        assertThrows(IllegalArgumentException.class, () -> Ipv6HostRange.expand("2001:db8::1"));
    }

    @Test void contains_insideAndOutside() {
        assertTrue(Ipv6HostRange.contains("2001:db8::/32", "2001:db8:ffff::1"));
        assertFalse(Ipv6HostRange.contains("2001:db8::/32", "2001:db9::1"));
    }

    @Test void contains_invalidIp_false() {
        assertFalse(Ipv6HostRange.contains("2001:db8::/32", "192.168.1.1"));
    }

    @Test void contains_invalidCidr_throws() {
        assertThrows(IllegalArgumentException.class, () -> Ipv6HostRange.contains("bad", "::1"));
    }

    @Test void canonical_normalizesCompressedForm() {
        assertEquals("2001:db8:0:0:0:0:0:1", Ipv6HostRange.canonical("2001:0DB8::1"));
    }

    @Test void canonical_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Ipv6HostRange.canonical("nope"));
    }
}
