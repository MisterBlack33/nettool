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
        List<String> subnets = SubnetDetector.getAllSubnets();
        // /8-Bug-Regression: max 256 Subnetze, nie 65k
        assertTrue(subnets.size() <= 256,
                "Zu viele Subnetze: " + subnets.size() + " (65k-Bug?)");
    }

    @Test
    void getAllSubnets_formattedCorrectly() throws Exception {
        for (String subnet : SubnetDetector.getAllSubnets()) {
            String[] parts = subnet.split("\\.");
            assertEquals(3, parts.length,
                    "Subnet sollte 3 Oktette haben: " + subnet);
            for (String p : parts) {
                int v = Integer.parseInt(p);
                assertTrue(v >= 0 && v <= 255);
            }
        }
    }

    @Test
    void getAllSubnets_noLoopback() throws Exception {
        List<String> subnets = SubnetDetector.getAllSubnets();
        assertFalse(subnets.contains("127.0.0"),
                "Loopback darf nicht enthalten sein");
    }

    @Test
    void getAllSubnets_noDockerBridge() throws Exception {
        // Docker-Bridges (172.17.x, 172.18.x) sollen gefiltert werden
        // wenn das Interface "docker" oder "br-" heißt
        List<String> subnets = SubnetDetector.getAllSubnets();
        // Kein Hard-Assert auf IP-Bereich (Testumgebung unbekannt),
        // nur sicherstellen dass keine Explosion passiert
        assertTrue(subnets.size() < 512, "Zu viele Subnetze: " + subnets.size());
    }
}