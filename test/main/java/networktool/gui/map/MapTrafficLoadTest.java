package main.java.networktool.gui.map;

import main.java.networktool.logic.scan.schedule.MapTrafficObserver.NodeRole;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapTrafficLoadTest {

    @Test void fromRoles_null_empty() {
        assertTrue(MapTrafficLoad.fromRoles(null).isEmpty());
    }

    @Test void fromRoles_unknownSkipped() {
        assertTrue(MapTrafficLoad.fromRoles(Map.of("1.1.1.1", NodeRole.UNKNOWN)).isEmpty());
    }

    @Test void fromRoles_dnsHeavierThanMdns() {
        Map<String, Long> load = MapTrafficLoad.fromRoles(
                Map.of("1.1.1.1", NodeRole.DNS_SERVER, "1.1.1.2", NodeRole.MDNS_NODE));
        assertTrue(load.get("1.1.1.1") > load.get("1.1.1.2"));
    }

    @Test void weightOf_allRolesPositiveExceptUnknownAndNull() {
        assertTrue(MapTrafficLoad.weightOf(NodeRole.DHCP_SERVER) > 0);
        assertTrue(MapTrafficLoad.weightOf(NodeRole.NTP_SERVER) > 0);
        assertEquals(0, MapTrafficLoad.weightOf(NodeRole.UNKNOWN));
        assertEquals(0, MapTrafficLoad.weightOf(null));
    }
}
