// test/main/java/networktool/gui/MapNodeClassifierTest.java
package networktool.gui;

import networktool.logic.scan.MapTrafficObserver;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MapNodeClassifierTest {

    MapTrafficObserver observer = new MapTrafficObserver();

    GuiNetworkMap.Node host(String os, String hn) {
        return new GuiNetworkMap.Node("1.1.1.1", hn, os, GuiNetworkMap.NodeType.HOST);
    }

    @Test void endDevice_windows()   { assertTrue(MapNodeClassifier.isEndDevice(host("Windows","desktop"))); }
    @Test void endDevice_android()   { assertTrue(MapNodeClassifier.isEndDevice(host("Android","galaxy"))); }
    @Test void endDevice_printer()   { assertTrue(MapNodeClassifier.isEndDevice(host("Drucker","printer"))); }
    @Test void endDevice_raspberry() { assertTrue(MapNodeClassifier.isEndDevice(host("Raspberry Pi","rpi"))); }
    @Test void notEndDevice_switch() { assertFalse(MapNodeClassifier.isEndDevice(host("Switch","sw-core"))); }
    @Test void notEndDevice_unknown(){ assertFalse(MapNodeClassifier.isEndDevice(host("Unbekannt","host-10"))); }

    @Test void switchByKeyword_router()  { assertTrue(MapNodeClassifier.isSwitchByKeyword(host("Router","any"))); }
    @Test void switchByKeyword_fritz()   { assertTrue(MapNodeClassifier.isSwitchByKeyword(host("","fritz.box"))); }
    @Test void switchByKeyword_null()    { assertFalse(MapNodeClassifier.isSwitchByKeyword(host(null,null))); }

    @Test void switchByTrafficRole_dns() {
        // Manually inject role for testing
        MapTrafficObserver obs = new MapTrafficObserver() {
            @Override public NodeRole getRole(String ip) { return NodeRole.DNS_SERVER; }
        };
        assertTrue(MapNodeClassifier.isSwitchByTrafficRole("1.1.1.1", obs));
    }

    @Test void switchByTrafficRole_unknown() {
        assertFalse(MapNodeClassifier.isSwitchByTrafficRole("1.1.1.1", observer));
    }

    @Test void classify_promotesSwitch() {
        GuiNetworkMap.Node n = host("Router","fritz.box");
        MapNodeClassifier.classify(List.of(n), observer);
        assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
    }

    @Test void classify_keepsEndDevice() {
        GuiNetworkMap.Node n = host("Windows","desktop-abc");
        MapNodeClassifier.classify(List.of(n), observer);
        assertEquals(GuiNetworkMap.NodeType.HOST, n.type);
    }

    @Test void classify_manualSwitch_promoted() {
        MapSwitchStore.add("9.9.9.9");
        GuiNetworkMap.Node n = new GuiNetworkMap.Node("9.9.9.9","h","Unbekannt", GuiNetworkMap.NodeType.HOST);
        MapNodeClassifier.classify(List.of(n), observer);
        assertEquals(GuiNetworkMap.NodeType.SWITCH, n.type);
        MapSwitchStore.remove("9.9.9.9");
    }
}