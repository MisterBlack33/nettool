// test/main/java/networktool/logic/scan/MapTrafficObserverTest.java
package networktool.logic.scan;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class MapTrafficObserverTest {

    MapTrafficObserver observer = new MapTrafficObserver();

    @Test void getRole_unknown_byDefault() {
        assertEquals(MapTrafficObserver.NodeRole.UNKNOWN, observer.getRole("1.2.3.4"));
    }

    @Test void getAllRoles_empty_initially() {
        assertTrue(observer.getAllRoles().isEmpty());
    }

    @Test void getAllRoles_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> observer.getAllRoles().put("x", MapTrafficObserver.NodeRole.DNS_SERVER));
    }

    @Test void clear_removesRoles() {
        observer.probe("192.0.2.1"); // closed ports → UNKNOWN, still clears
        observer.clear();
        assertTrue(observer.getAllRoles().isEmpty());
    }

    @Test void probe_unreachable_staysUnknown() {
        observer.probe("192.0.2.1");
        assertEquals(MapTrafficObserver.NodeRole.UNKNOWN, observer.getRole("192.0.2.1"));
    }

    @Test void nodeRole_values_exist() {
        assertNotNull(MapTrafficObserver.NodeRole.DNS_SERVER);
        assertNotNull(MapTrafficObserver.NodeRole.DHCP_SERVER);
        assertNotNull(MapTrafficObserver.NodeRole.MDNS_NODE);
        assertNotNull(MapTrafficObserver.NodeRole.UNKNOWN);
    }
}