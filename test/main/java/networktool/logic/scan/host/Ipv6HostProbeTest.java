package main.java.networktool.logic.scan.host;

import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Ipv6HostProbeTest extends NetworkTimeoutTestBase {

    @Test void describe_loopback_fillsAllFields() {
        ScanResult result = Ipv6HostProbe.describe("::1");
        assertEquals("::1", result.getIp());
        assertNotNull(result.getHostname());
        assertNotNull(result.getOpenPorts());
        assertFalse(result.getOsGuess().isBlank());
    }

    @Test void isAlive_doesNotThrow() {
        assertDoesNotThrow(() -> Ipv6HostProbe.isAlive("::1"));
    }

    @Test void describe_invalidAddress_fallsBackToIp() {
        assertEquals("not-resolvable.invalid.", Ipv6HostProbe.describe("not-resolvable.invalid.").getHostname());
    }
}
