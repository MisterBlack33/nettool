package main.java.networktool.gui.map;

import main.java.networktool.gui.components.GuiNetworkMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapTopologyTest {

    // â”€â”€ subnet24 / lastOctet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void subnet24ExtractsFirstThreeOctets() {
        assertEquals("192.168.1", MapTopology.subnet24("192.168.1.42"));
    }

    @Test void subnet24NullForInvalidIp() {
        assertNull(MapTopology.subnet24(null));
        assertNull(MapTopology.subnet24("nohost"));
    }

    @Test void lastOctetParsesCorrectly() {
        assertEquals(42, MapTopology.lastOctet("192.168.1.42"));
    }

    @Test void lastOctetFallbackForInvalidIp() {
        assertEquals(999, MapTopology.lastOctet(null));
        assertEquals(999, MapTopology.lastOctet("nohost"));
    }

    // â”€â”€ isEndDevice â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void endDeviceDetectedByWindowsOs() {
        assertTrue(MapTopology.isEndDevice("1.1.1.1", "Windows 11", "desktop-pc"));
    }

    @Test void endDeviceDetectedByPhoneHostname() {
        assertTrue(MapTopology.isEndDevice("1.1.1.1", null, "Galaxy-S23"));
    }

    @Test void endDeviceFalseForUnknownIsolated() {
        assertFalse(MapTopology.isEndDevice("203.0.113.7", "", "host-99"));
    }

    // â”€â”€ isSwitchByKeyword â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void switchDetectedByRouterKeyword() {
        assertTrue(MapTopology.isSwitchByKeyword("Router", "any"));
    }

    @Test void switchDetectedByFritzHostname() {
        assertTrue(MapTopology.isSwitchByKeyword(null, "fritz.box"));
    }

    @Test void switchFalseForNullValues() {
        assertFalse(MapTopology.isSwitchByKeyword(null, null));
    }

    // â”€â”€ isSwitchByOui â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test void switchByOuiDetectsCiscoMac() {
        assertTrue(MapTopology.isSwitchByOui("host [00:1A:A1:22:33:44]"));
    }

    @Test void switchByOuiFalseForUnknownVendor() {
        assertFalse(MapTopology.isSwitchByOui("host [12:34:56:78:9A:BC]"));
    }

    @Test void switchByOuiFalseWithoutBrackets() {
        assertFalse(MapTopology.isSwitchByOui("plain-hostname"));
        assertFalse(MapTopology.isSwitchByOui(null));
    }

    // â”€â”€ classifyNodes (Integration der reinen Regeln, keine Store-Ports) â”€â”€

    @Test void classifyPromotesKeywordSwitch() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("1.1.1.1", "fritz.box", "", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
    }

    @Test void classifyKeepsEndDeviceAsHost() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("1.1.1.2", "desktop-abc", "Windows 11", GuiNetworkMap.NodeType.HOST);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
    }

    @Test void classifyIgnoresNonHostNodes() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("1.1.1.3", "gw", "Router", GuiNetworkMap.NodeType.GATEWAY);
        MapTopology.classifyNodes(List.of(n));
        assertEquals(GuiNetworkMap.NodeType.GATEWAY, n.type); // unverÃ¤ndert, kein HOST
    }
}
