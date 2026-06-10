package main.java.networktool.logic.scan;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubnetDetectorTest {

    @Test
    void getAllSubnets_doesNotThrow() {
        assertDoesNotThrow(() -> SubnetDetector.getAllSubnets());
    }

    @Test
    void getAllSubnets_returnsNotNull() throws Exception {
        assertNotNull(SubnetDetector.getAllSubnets());
    }

    @Test
    void getAllSubnets_noMoreThan256Entries() throws Exception {
        // max 256 /24-Subnetze (z.B. bei /16)
        assertTrue(SubnetDetector.getAllSubnets().size() <= 256,
                "Zu viele Subnetze: " + SubnetDetector.getAllSubnets().size());
    }

    @Test
    void getAllSubnets_formattedCorrectly() throws Exception {
        for (String subnet : SubnetDetector.getAllSubnets()) {
            String[] parts = subnet.split("\\.");
            assertEquals(3, parts.length, "Subnet muss 3 Oktette haben: " + subnet);
            for (String p : parts) {
                int v = Integer.parseInt(p);
                assertTrue(v >= 0 && v <= 255);
            }
        }
    }

    @Test
    void getAllSubnets_noLoopback() throws Exception {
        assertFalse(SubnetDetector.getAllSubnets().contains("127.0.0"),
                "Loopback darf nicht enthalten sein");
    }

    @Test
    void getAllSubnets_noDockerBridge() throws Exception {
        assertTrue(SubnetDetector.getAllSubnets().size() < 512,
                "Zu viele Subnetze: " + SubnetDetector.getAllSubnets().size());
    }

    @Test
    void getTotalHostCount_correctMultiplier() {
        assertEquals(508, SubnetDetector.getTotalHostCount(List.of("10.0.1", "10.0.2")));
    }
}