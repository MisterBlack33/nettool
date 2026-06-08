package main.java.networktool.logic.scan;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SubnetDetectorExtTest {

    @Test void getAllCidrs_doesNotThrow() {
        assertDoesNotThrow(() -> SubnetDetector.getAllCidrs());
    }

    @Test void getAllCidrs_validFormat() throws Exception {
        for (String cidr : SubnetDetector.getAllCidrs()) {
            assertTrue(cidr.contains("/"), "Kein CIDR-Format: " + cidr);
            String[] parts = cidr.split("/");
            assertEquals(2, parts.length);
            int prefix = Integer.parseInt(parts[1]);
            assertTrue(prefix >= 8 && prefix <= 30);
        }
    }

    @Test void getAllCidrs_noLoopback() throws Exception {
        assertFalse(SubnetDetector.getAllCidrs().stream()
                .anyMatch(c -> c.startsWith("127.")));
    }

    @Test void getAllSubnets_backwardCompat() throws Exception {
        List<String> subnets = SubnetDetector.getAllSubnets();
        for (String s : subnets) {
            assertEquals(3, s.split("\\.").length,
                    "getAllSubnets() muss 3-Oktet-Präfixe liefern: " + s);
        }
    }

    @Test void getAllSubnets_noLoopback() throws Exception {
        assertFalse(SubnetDetector.getAllSubnets().contains("127.0.0"));
    }
}