package main.java.networktool_v3.logic.ports;

import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für PortScanner-Fix: COMMON_PORTS() entfernt, kein Changelog-Kommentar. */
class PortScannerFixTest {

    @Test
    void commonPorts_methodDoesNotExist() {
        boolean found = false;
        for (Method m : PortScanner.class.getDeclaredMethods()) {
            if (m.getName().equals("COMMON_PORTS")) { found = true; break; }
        }
        assertFalse(found, "COMMON_PORTS() sollte entfernt sein");
    }

    @Test
    void defaultPorts_notEmpty() {
        assertFalse(PortScanner.DEFAULT_PORTS.isEmpty());
    }

    @Test
    void fastPorts_notEmpty() {
        assertFalse(PortScanner.FAST_PORTS.isEmpty());
    }

    @Test
    void fastPorts_subsetOfDefaultPorts() {
        assertTrue(PortScanner.DEFAULT_PORTS.containsAll(PortScanner.FAST_PORTS));
    }

    @Test
    void setActivePorts_null_resetsToDefault() {
        PortScanner.setActivePorts(null);
        assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
    }

    @Test
    void setActivePorts_empty_resetsToDefault() {
        PortScanner.setActivePorts(java.util.List.of());
        assertEquals(PortScanner.DEFAULT_PORTS, PortScanner.getActivePorts());
    }

    @Test
    void setActivePorts_custom_applied() {
        PortScanner.setActivePorts(java.util.List.of(22, 80, 443));
        assertEquals(3, PortScanner.getActivePorts().size());
        PortScanner.setActivePorts(null); // reset
    }

    @Test
    void probePort_closed_returnsClosed() {
        assertEquals(PortScanner.PortState.CLOSED,
                PortScanner.probePort("127.0.0.1", 19995, 300));
    }

    @Test
    void isOpen_closedPort_false() {
        assertFalse(PortScanner.isOpen("127.0.0.1", 19996, 300));
    }

    @Test
    void scanSimple_doesNotThrow() {
        assertDoesNotThrow(() -> PortScanner.scanSimple("127.0.0.1", 300));
    }
}