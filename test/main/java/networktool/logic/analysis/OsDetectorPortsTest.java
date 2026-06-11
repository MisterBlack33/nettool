// test/main/java/networktool/logic/analysis/OsDetectorPortsTest.java
package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class OsDetectorPortsTest {

    @Test void detectByPorts_unreachable_returnsUnbekannt() {
        assertEquals("Unbekannt", OsDetectorPorts.detectByPorts("192.0.2.1"));
    }

    @Test void detectWithSignature_unreachable_returnsNull() {
        assertNull(OsDetectorPorts.detectWithSignature("192.0.2.1"));
    }

    @Test void detectWithSignature_doesNotThrow() {
        assertDoesNotThrow(() -> OsDetectorPorts.detectWithSignature("192.0.2.1"));
    }

    @Test void isOpen_closedPort_false() {
        assertFalse(OsDetectorPorts.isOpen("127.0.0.1", 19987));
    }

    @Test void detectWebServer_doesNotThrow() {
        assertDoesNotThrow(() -> OsDetectorPorts.detectWebServer("192.0.2.1"));
    }

    @Test void detectWebServer_unreachable_returnsWebServer() {
        assertEquals("Web-Server", OsDetectorPorts.detectWebServer("192.0.2.1"));
    }
}