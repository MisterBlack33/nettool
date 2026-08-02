package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.OsDetectorPorts;
import main.java.networktool.logic.ports.PortScanner;
import org.junit.jupiter.api.*;

public abstract class NetworkTimeoutTestBase {
    @BeforeAll static void shrinkTimeouts() {
        HostAliveChecker.setTestTimeouts(150, 100);
        OsDetectorPorts.setTestTimeout(100);
        PortScanner.TIMEOUT_LAN  = 200;
        PortScanner.TIMEOUT_FAST = 100;
    }
}