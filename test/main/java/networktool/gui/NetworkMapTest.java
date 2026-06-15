package main.java.networktool.gui;

import main.java.networktool.logic.analysis.TracerouteRunner;
import main.java.networktool.logic.scan.MapTrafficObserver;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NetworkMapTest {

    @BeforeEach
    void clearState() {
        MapSwitchStore.clear();
        GuiNetworkMap.HOP_PARENT.clear();
    }

    // ── MapTopology ───────────────────────────────────────────────────────

    @Nested
    class MapTopologyTest {

        @Test void isEndDevice_windows() {
            assertTrue(MapTopology.isEndDevice("1.1.1.1", "Windows", "desktop-abc"));
        }

        @Test void isEndDevice_android() {
            assertTrue(MapTopology.isEndDevice("1.1.1.1", "Android (Samsung)", "galaxy-s23"));
        }

        @Test void isEndDevice_drucker() {
            assertTrue(MapTopology.isEndDevice("1.1.1.1", "Drucker (JetDirect)", "printer.local"));
        }

        @Test void isEndDevice_raspberry() {
            assertTrue(MapTopology.isEndDevice("1.1.1.1", "Raspberry Pi (Linux)", "rpi4"));
        }

        @Test void notEndDevice_router() {
            assertFalse(MapTopology.isEndDevice("1.1.1.1", "Router / Switch", "sw-core"));
        }

        @Test void notEndDevice_unknown() {
            assertFalse(MapTopology.isEndDevice("1.1.1.1", "Unbekannt", "host-192-168-178-36"));
        }

        @Test void isSwitchByKeyword_router_os() {
            assertTrue(MapTopology.isSwitchByKeyword("Router / Switch", "any"));
        }

        @Test void isSwitchByKeyword_fritz_hn() {
            assertTrue(MapTopology.isSwitchByKeyword("", "fritz.box"));
        }

        @Test void isSwitchByKeyword_dns_os() {
            assertTrue(MapTopology.isSwitchByKeyword("DNS-Server", "any"));
        }

        @Test void isSwitchByKeyword_dhcp_os() {
            assertTrue(MapTopology.isSwitchByKeyword("DHCP-Server", "any"));
        }

        @Test void isSwitchByKeyword_null_safe() {
            assertFalse(MapTopology.isSwitchByKeyword(null, null));
        }

        @Test void isSwitchByOui_cisco_colon() {
            assertTrue(MapTopology.isSwitchByOui("sw [00:1A:A1:12:34:56]"));
        }

        @Test void isSwitchByOui_hp_procurve() {
            assertTrue(MapTopology.isSwitchByOui("hp [00:17:A4:AB:CD:EF]"));
        }

        @Test void isSwitchByOui_mikrotik() {
            assertTrue(MapTopology.isSwitchByOui("rb [20:E5:2A:11:22:33]"));
        }

        @Test void isSwitchByOui_ubiquiti() {
            assertTrue(MapTopology.isSwitchByOui("ap [50:C7:BF:AA:BB:CC]"));
        }

        @Test void isSwitchByOui_dash_format() {
            assertTrue(MapTopology.isSwitchByOui("sw [00-1A-A1-12-34-56]"));
        }

        @Test void isSwitchByOui_unknown_mac() {
            assertFalse(MapTopology.isSwitchByOui("dev [FF:FF:FF:12:34:56]"));
        }

        @Test void isSwitchByOui_no_bracket() {
            assertFalse(MapTopology.isSwitchByOui("plain-hostname"));
        }

        @Test void isSwitchByOui_null() {
            assertFalse(MapTopology.isSwitchByOui(null));
        }

        @Test void subnet24_normal() {
            assertEquals("192.168.1", MapTopology.subnet24("192.168.1.50"));
        }

        @Test void subnet24_null() {
            assertNull(MapTopology.subnet24(null));
        }

        @Test void lastOctet_normal() {
            assertEquals(36, MapTopology.lastOctet("192.168.178.36"));
        }

        @Test void lastOctet_null() {
            assertEquals(999, MapTopology.lastOctet(null));
        }

        @Test void classifyNodes_manual_switch() {
            MapSwitchStore.add("10.0.0.5");
            GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.5", "host", "Unbekannt", GuiNetworkMap.NodeType.HOST);
            MapTopology.classifyNodes(List.of(n));
            assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
        }

        @Test void classifyNodes_enddevice_not_promoted() {
            GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.6", "galaxy-s23", "Android (Samsung)", GuiNetworkMap.NodeType.HOST);
            MapTopology.classifyNodes(List.of(n));
            assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
        }

        @Test void classifyNodes_keyword_switch() {
            GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.7", "fritz.box", "Router", GuiNetworkMap.NodeType.HOST);
            MapTopology.classifyNodes(List.of(n));
            assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
        }

        @Test void classifyNodes_windows_stays_host() {
            GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.8", "W11-Pro", "Windows", GuiNetworkMap.NodeType.HOST);
            MapTopology.classifyNodes(List.of(n));
            assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
        }

        @Test void classifyNodes_dns_keyword_promoted() {
            GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.9", "dns-server", "DNS-Server", GuiNetworkMap.NodeType.HOST);
            MapTopology.classifyNodes(List.of(n));
            assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
        }
    }

    // ── MapHopDiscovery ───────────────────────────────────────────────────

    @Nested
    class MapHopDiscoveryTest {

        @Test void findUpstream_empty_returns_null() {
            assertNull(MapHopDiscovery.findUpstream(List.of(), "10.0.0.1", "192.168.1.1"));
        }

        @Test void findUpstream_single_hop_returns_null() {
            TracerouteRunner.HopInfo h = new TracerouteRunner.HopInfo(1);
            h.ip = "10.0.0.1";
            assertNull(MapHopDiscovery.findUpstream(List.of(h), "10.0.0.1", "192.168.1.1"));
        }

        @Test void findUpstream_intermediate_hop_detected() {
            TracerouteRunner.HopInfo h1 = new TracerouteRunner.HopInfo(1); h1.ip = "192.168.1.36";
            TracerouteRunner.HopInfo h2 = new TracerouteRunner.HopInfo(2); h2.ip = "192.168.1.50";
            assertEquals("192.168.1.36",
                    MapHopDiscovery.findUpstream(List.of(h1, h2), "192.168.1.50", "192.168.1.1"));
        }

        @Test void findUpstream_timeout_hops_skipped() {
            TracerouteRunner.HopInfo t = new TracerouteRunner.HopInfo(1); t.timeout = true;
            TracerouteRunner.HopInfo h = new TracerouteRunner.HopInfo(2); h.ip = "10.0.0.5";
            assertNull(MapHopDiscovery.findUpstream(List.of(t, h), "10.0.0.5", "10.0.0.1"));
        }

        @Test void findUpstream_gateway_skipped() {
            TracerouteRunner.HopInfo gw  = new TracerouteRunner.HopInfo(1); gw.ip  = "192.168.1.1";
            TracerouteRunner.HopInfo mid = new TracerouteRunner.HopInfo(2); mid.ip = "192.168.1.5";
            TracerouteRunner.HopInfo dst = new TracerouteRunner.HopInfo(3); dst.ip = "192.168.1.50";
            assertEquals("192.168.1.5",
                    MapHopDiscovery.findUpstream(List.of(gw, mid, dst), "192.168.1.50", "192.168.1.1"));
        }

        @Test void discover_doesNotThrow() {
            assertDoesNotThrow(MapHopDiscovery::discover);
        }
    }

    // ── MapTrafficObserver ────────────────────────────────────────────────

    @Nested
    class MapTrafficObserverTest {

        MapTrafficObserver observer = new MapTrafficObserver();

        @Test void getRole_unknown_byDefault() {
            assertEquals(MapTrafficObserver.NodeRole.UNKNOWN, observer.getRole("1.2.3.4"));
        }

        @Test void getAllRoles_empty_initially() {
            assertTrue(observer.getAllRoles().isEmpty());
        }

        @Test void getAllRoles_isUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                    () -> observer.getAllRoles().put("x", MapTrafficObserver.NodeRole.DNS_SERVER));
        }

        @Test void clear_removesRoles() {
            observer.probe("192.0.2.1");
            observer.clear();
            assertTrue(observer.getAllRoles().isEmpty());
        }

        @Test void probe_unreachable_staysUnknown() {
            observer.probe("192.0.2.1");
            assertEquals(MapTrafficObserver.NodeRole.UNKNOWN, observer.getRole("192.0.2.1"));
        }

        @Test void probeAll_doesNotThrow() {
            assertDoesNotThrow(() -> observer.probeAll(List.of("192.0.2.1", "192.0.2.2")));
        }

        @Test void nodeRole_values_exist() {
            assertNotNull(MapTrafficObserver.NodeRole.DNS_SERVER);
            assertNotNull(MapTrafficObserver.NodeRole.DHCP_SERVER);
            assertNotNull(MapTrafficObserver.NodeRole.MDNS_NODE);
            assertNotNull(MapTrafficObserver.NodeRole.NTP_SERVER);
            assertNotNull(MapTrafficObserver.NodeRole.UNKNOWN);
        }

        @Test void probe_loopback_doesNotThrow() {
            assertDoesNotThrow(() -> observer.probe("127.0.0.1"));
        }
    }

    // ── MapSwitchStore ────────────────────────────────────────────────────

    @Nested
    class MapSwitchStoreTest {

        @Test void add_contains() {
            MapSwitchStore.add("10.0.0.1");
            assertTrue(MapSwitchStore.contains("10.0.0.1"));
        }

        @Test void remove_notContained() {
            MapSwitchStore.add("10.0.0.2");
            MapSwitchStore.remove("10.0.0.2");
            assertFalse(MapSwitchStore.contains("10.0.0.2"));
        }

        @Test void clear_removesAll() {
            MapSwitchStore.add("10.0.0.3");
            MapSwitchStore.clear();
            assertFalse(MapSwitchStore.contains("10.0.0.3"));
        }
    }

    // ── HOP_PARENT ────────────────────────────────────────────────────────

    @Nested
    class HopParentTest {

        @Test void put_get() {
            GuiNetworkMap.HOP_PARENT.put("192.168.1.50", "192.168.1.36");
            assertEquals("192.168.1.36", GuiNetworkMap.HOP_PARENT.get("192.168.1.50"));
        }

        @Test void missing_null() {
            assertNull(GuiNetworkMap.HOP_PARENT.get("99.99.99.99"));
        }
    }
}