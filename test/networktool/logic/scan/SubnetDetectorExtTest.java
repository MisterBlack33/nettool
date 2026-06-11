package networktool.logic.scan;

import main.java.networktool.logic.scan.SubnetDetector;
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
            assertTrue(prefix >= 16 && prefix <= 30);
        }
    }

    @Test void getAllCidrs_noLoopback() throws Exception {
        assertFalse(SubnetDetector.getAllCidrs().stream()
                .anyMatch(c -> c.startsWith("127.")));
    }

    @Test void getAllCidrs_noLinkLocal() throws Exception {
        assertFalse(SubnetDetector.getAllCidrs().stream()
                .anyMatch(c -> c.startsWith("169.254")));
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

    @Test void getAllSubnets_noLinkLocal() throws Exception {
        for (String s : SubnetDetector.getAllSubnets())
            assertFalse(s.startsWith("169.254"),
                    "Link-Local darf nicht enthalten sein: " + s);
    }
}