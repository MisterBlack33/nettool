package main.java.networktool.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CIDRUtilsExtTest {

    @Test void getSubnet24Prefixes_slash24_one() {
        assertEquals(1, CIDRUtils.getSubnet24Prefixes("192.168.1.0/24").size());
        assertEquals("192.168.1", CIDRUtils.getSubnet24Prefixes("192.168.1.0/24").get(0));
    }

    @Test void getSubnet24Prefixes_slash23_two() {
        assertEquals(2, CIDRUtils.getSubnet24Prefixes("192.168.0.0/23").size());
    }

    @Test void getSubnet24Prefixes_slash19_32() {
        // 10.32.0.0/19 → 32 /24-Blöcke (10.32.0 .. 10.32.31)
        List<String> p = CIDRUtils.getSubnet24Prefixes("10.32.0.0/19");
        assertEquals(32, p.size());
        assertEquals("10.32.0", p.get(0));
        assertEquals("10.32.31", p.get(31));
    }

    @Test void getSubnet24Prefixes_slash16_256() {
        assertEquals(256, CIDRUtils.getSubnet24Prefixes("10.0.0.0/16").size());
    }

    @Test void getAllIPs_slash19_count() {
        // /19 = 8190 Host-IPs
        assertEquals(8190, CIDRUtils.getAllIPs("10.32.0.0/19").size());
    }

    @Test void getAllIPs_slash30_two() {
        assertEquals(2, CIDRUtils.getAllIPs("192.168.0.0/30").size());
    }

    @Test void intToIp_roundtrip() {
        String ip = "10.32.16.5";
        assertEquals(ip, CIDRUtils.intToIp(CIDRUtils.ipToInt(ip)));
    }
}