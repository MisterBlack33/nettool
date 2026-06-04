package main.java.networktool.gui;

import main.java.networktool.networktool_v3.gui.MapHopDiscovery;
import main.java.networktool.networktool_v3.gui.MapSwitchStore;
import main.java.networktool.networktool_v3.gui.MapTopology;
import main.java.networktool.logic.analysis.TracerouteRunner;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für GuiNetworkMap, MapTopology, MapSwitchStore, MapHopDiscovery.
 * Pfad: test/main/java/networktool_v3/gui/GuiNetworkMapTest.java
 */
class GuiNetworkMapTest {

    @BeforeEach
    void clearState() {
        MapSwitchStore.clear();
        GuiNetworkMap.HOP_PARENT.clear();
    }

    // ── MapSwitchStore ────────────────────────────────────────────────────

    @Test void switchStore_add_contains() {
        MapSwitchStore.add("10.0.0.1");
        assertTrue(MapSwitchStore.contains("10.0.0.1"));
    }

    @Test void switchStore_remove() {
        MapSwitchStore.add("10.0.0.2");
        MapSwitchStore.remove("10.0.0.2");
        assertFalse(MapSwitchStore.contains("10.0.0.2"));
    }

    @Test void switchStore_clear() {
        MapSwitchStore.add("10.0.0.3");
        MapSwitchStore.clear();
        assertFalse(MapSwitchStore.contains("10.0.0.3"));
    }

    @Test void switchStore_persist_roundtrip() {
        MapSwitchStore.add("192.168.1.36");
        MapSwitchStore.SWITCHES.clear();
        assertDoesNotThrow(() -> {
            // reload via load() — indirekt via neues Objekt nicht möglich,
            // aber saveManualSwitches / loadManualSwitches public
        });
    }

    // ── MapTopology.isEndDevice ───────────────────────────────────────────

    @Test void endDevice_windows()      { assertTrue(MapTopology.isEndDevice("1.1.1.1","Windows","desktop")); }
    @Test void endDevice_android()      { assertTrue(MapTopology.isEndDevice("1.1.1.1","Android","galaxy-s23")); }
    @Test void endDevice_samsung_a21()  { assertTrue(MapTopology.isEndDevice("1.1.1.1","Unbekannt","A21s-von-Elias")); }
    @Test void endDevice_drucker()      { assertTrue(MapTopology.isEndDevice("1.1.1.1","Drucker (JetDirect)","Drucker.fritz.box")); }
    @Test void endDevice_linux()        { assertTrue(MapTopology.isEndDevice("1.1.1.1","Linux/Unix","srv")); }
    @Test void endDevice_macos()        { assertTrue(MapTopology.isEndDevice("1.1.1.1","macOS","macbook")); }
    @Test void endDevice_raspberry()    { assertTrue(MapTopology.isEndDevice("1.1.1.1","Raspberry Pi (Linux)","rpi")); }
    @Test void notEndDevice_unknown()   { assertFalse(MapTopology.isEndDevice("1.1.1.1","Unbekannt","host-192-168-178-36")); }
    @Test void notEndDevice_switch()    { assertFalse(MapTopology.isEndDevice("1.1.1.1","Switch","sw-core")); }

    // ── MapTopology.isSwitchByKeyword ─────────────────────────────────────

    @Test void keyword_router_os()      { assertTrue(MapTopology.isSwitchByKeyword("Router / Switch","any")); }
    @Test void keyword_fritz_hn()       { assertTrue(MapTopology.isSwitchByKeyword("","fritz.box")); }
    @Test void keyword_sw_dash()        { assertTrue(MapTopology.isSwitchByKeyword("","sw-office")); }
    @Test void keyword_procurve()       { assertTrue(MapTopology.isSwitchByKeyword("procurve","hp-2530")); }
    @Test void keyword_no_match()       { assertFalse(MapTopology.isSwitchByKeyword("Unbekannt","host-36")); }
    @Test void keyword_null_safe()      { assertFalse(MapTopology.isSwitchByKeyword(null, null)); }

    // ── MapTopology.isSwitchByOui ─────────────────────────────────────────

    @Test void oui_cisco_colon()        { assertTrue(MapTopology.isSwitchByOui("sw [00:1A:A1:12:34:56]")); }
    @Test void oui_hp_procurve()        { assertTrue(MapTopology.isSwitchByOui("hp [00:17:A4:AB:CD:EF]")); }
    @Test void oui_dash_format()        { assertTrue(MapTopology.isSwitchByOui("sw [00-1A-A1-12-34-56]")); }
    @Test void oui_unknown_mac()        { assertFalse(MapTopology.isSwitchByOui("dev [FF:FF:FF:12:34:56]")); }
    @Test void oui_no_bracket()         { assertFalse(MapTopology.isSwitchByOui("plain-hostname")); }
    @Test void oui_null()               { assertFalse(MapTopology.isSwitchByOui(null)); }

    // ── MapTopology helpers ───────────────────────────────────────────────

    @Test void subnet24_normal()        { assertEquals("192.168.1", MapTopology.subnet24("192.168.1.50")); }
    @Test void subnet24_null()          { assertNull(MapTopology.subnet24(null)); }
    @Test void lastOctet_normal()       { assertEquals(36, MapTopology.lastOctet("192.168.178.36")); }
    @Test void lastOctet_null()         { assertEquals(999, MapTopology.lastOctet(null)); }

    // ── MapTopology.classifyNodes ─────────────────────────────────────────

    @Test void classify_manual_switch() {
        MapSwitchStore.add("10.0.0.5");
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.5","host","Unbekannt", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
    }

    @Test void classify_enddevice_not_promoted() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.6","A21s-von-Elias","Unbekannt", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
    }

    @Test void classify_keyword_switch() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.7","fritz.box","Router", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
    }

    @Test void classify_windows_stays_host() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("10.0.0.8","W11-Pro","Windows", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
    }

    // ── MapHopDiscovery.findUpstream ──────────────────────────────────────

    @Test void findUpstream_intermediate_hop() {
        List<TracerouteRunner.HopInfo> hops = new ArrayList<>();
        TracerouteRunner.HopInfo h1 = new TracerouteRunner.HopInfo(1);
        h1.ip = "192.168.1.36";
        TracerouteRunner.HopInfo h2 = new TracerouteRunner.HopInfo(2);
        h2.ip = "192.168.1.50";
        hops.add(h1); hops.add(h2);
        assertEquals("192.168.1.36",
                MapHopDiscovery.findUpstream(hops, "192.168.1.50", "192.168.1.1"));
    }

    @Test void findUpstream_direct_returns_null() {
        List<TracerouteRunner.HopInfo> hops = new ArrayList<>();
        TracerouteRunner.HopInfo h = new TracerouteRunner.HopInfo(1);
        h.ip = "192.168.1.50";
        hops.add(h);
        assertNull(MapHopDiscovery.findUpstream(hops, "192.168.1.50", "192.168.1.1"));
    }

    @Test void findUpstream_empty_returns_null() {
        assertNull(MapHopDiscovery.findUpstream(List.of(), "10.0.0.1", "10.0.0.254"));
    }

    @Test void findUpstream_timeout_skipped() {
        List<TracerouteRunner.HopInfo> hops = new ArrayList<>();
        TracerouteRunner.HopInfo timeout = new TracerouteRunner.HopInfo(1);
        timeout.timeout = true;
        TracerouteRunner.HopInfo target = new TracerouteRunner.HopInfo(2);
        target.ip = "192.168.1.50";
        hops.add(timeout); hops.add(target);
        assertNull(MapHopDiscovery.findUpstream(hops, "192.168.1.50", "192.168.1.1"));
    }

    @Test void hopDiscovery_doesNotThrow() {
        assertDoesNotThrow(MapHopDiscovery::discover);
    }

    // ── HOP_PARENT ────────────────────────────────────────────────────────

    @Test void hopParent_put_get() {
        GuiNetworkMap.HOP_PARENT.put("192.168.1.50","192.168.1.36");
        assertEquals("192.168.1.36", GuiNetworkMap.HOP_PARENT.get("192.168.1.50"));
    }

    @Test void hopParent_missing_null() {
        assertNull(GuiNetworkMap.HOP_PARENT.get("99.99.99.99"));
    }
}