package main.java.networktool_v3.logic.scan;

import main.java.networktool.logic.scan.NetworkHostScanner;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkHostScannerPackageTest {

    @Test
    void readMacFromArp_localhost_doesNotThrow() {
        assertDoesNotThrow(() -> NetworkHostScanner.readMacFromArp("127.0.0.1"));
    }

    @Test
    void readMacFromArp_unreachable_returnsNull() {
        assertNull(NetworkHostScanner.readMacFromArp("192.0.2.1"));
    }
}