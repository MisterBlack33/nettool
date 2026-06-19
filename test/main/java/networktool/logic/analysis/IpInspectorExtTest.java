package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.*;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class IpInspectorExtTest {

    @BeforeEach
    void enableTestMode() {
        IpInspector.testMode = true;
    }

    @AfterEach
    void disableTestMode() {
        IpInspector.testMode = false;
    }

    static boolean loopbackReachable() {
        try { return InetAddress.getByName("127.0.0.1").isReachable(500); }
        catch (Exception e) { return false; }
    }

    @Test
    void quickScan_testMode_doesNotThrow() {
        assertDoesNotThrow(() -> IpInspector.quickScan("192.0.2.1", 500));
    }

    @Test
    void inspect_testMode_doesNotThrow() {
        assertDoesNotThrow(() -> IpInspector.inspect("192.0.2.1"));
    }

    @Test
    void inspectHopsOnly_testMode_doesNotThrow() {
        assertDoesNotThrow(() -> IpInspector.inspectHopsOnly("192.0.2.1"));
    }

    @Test
    void quickScan_unreachable_doesNotThrow() {
        IpInspector.testMode = false;
        assertDoesNotThrow(() -> IpInspector.quickScan("192.0.2.1", 300));
    }

    @Test
    void inspect_unreachable_doesNotThrow() {
        IpInspector.testMode = false;
        assertDoesNotThrow(() -> IpInspector.inspect("192.0.2.1"));
    }

    @Test
    void quickScan_localhost_doesNotThrow() {
        IpInspector.testMode = false;
        assumeTrue(loopbackReachable());
        assertDoesNotThrow(() -> IpInspector.quickScan("127.0.0.1", 1000));
    }

    @Test
    void inspect_localhost_doesNotThrow() {
        IpInspector.testMode = false;
        assumeTrue(loopbackReachable());
        assertDoesNotThrow(() -> IpInspector.inspect("127.0.0.1"));
    }
}