package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MapLayoutTest {
    private GuiNetworkMap.Node node(String ip, GuiNetworkMap.NodeType type) {
        return new GuiNetworkMap.Node(ip, ip, "", type);
    }
    @Test void apply_placesGatewayAtCenter() {
        GuiNetworkMap.Node gw = node("1.1.1.1", GuiNetworkMap.NodeType.GATEWAY);
        MapLayout.apply(List.of(gw), List.of(), 800, 600);
        assertEquals(400, gw.x); assertEquals(300, gw.y);
    }
    @Test void apply_placesSelfOffsetFromCenter() {
        GuiNetworkMap.Node self = node("1.1.1.2", GuiNetworkMap.NodeType.SELF);
        MapLayout.apply(List.of(self), List.of(), 800, 600);
        assertEquals(260, self.x); assertEquals(190, self.y);
    }
    @Test void apply_noNodes_doesNotThrow() {
        assertDoesNotThrow(() -> MapLayout.apply(List.of(), List.of(), 800, 600));
    }
    @Test void apply_singleSwitch_placedOnRingRadius() {
        GuiNetworkMap.Node sw = node("1.1.1.3", GuiNetworkMap.NodeType.SWITCH);
        MapLayout.apply(List.of(sw), List.of(), 800, 600);
        assertEquals(200, Math.hypot(sw.x - 400, sw.y - 300), 1.0);
    }
    @Test void apply_twoSwitches_bothOnRing_atDifferentAngles() {
        GuiNetworkMap.Node sw1 = node("1.1.1.3", GuiNetworkMap.NodeType.SWITCH);
        GuiNetworkMap.Node sw2 = node("1.1.1.4", GuiNetworkMap.NodeType.SWITCH);
        MapLayout.apply(List.of(sw1, sw2), List.of(), 800, 600);
        assertEquals(200, Math.hypot(sw1.x - 400, sw1.y - 300), 1.0);
        assertEquals(200, Math.hypot(sw2.x - 400, sw2.y - 300), 1.0);
        assertNotEquals(sw1.y, sw2.y);
    }
    @Test void apply_singleHost_placedAtParentRadius() {
        GuiNetworkMap.Node gw = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node host = node("10.0.0.2", GuiNetworkMap.NodeType.HOST);
        MapLayout.apply(List.of(gw, host),
                List.of(new GuiNetworkMap.Edge(host, gw, GuiNetworkMap.EdgeType.NORMAL)), 800, 600);
        assertEquals(87, Math.hypot(host.x - gw.x, host.y - gw.y), 1.0);
    }
    @Test void apply_multipleHosts_spreadAroundParent_atDistinctPositions() {
        GuiNetworkMap.Node gw = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node h1 = node("10.0.0.2", GuiNetworkMap.NodeType.HOST);
        GuiNetworkMap.Node h2 = node("10.0.0.3", GuiNetworkMap.NodeType.HOST);
        List<GuiNetworkMap.Edge> edges = List.of(
                new GuiNetworkMap.Edge(h1, gw, GuiNetworkMap.EdgeType.NORMAL),
                new GuiNetworkMap.Edge(h2, gw, GuiNetworkMap.EdgeType.NORMAL));
        MapLayout.apply(List.of(gw, h1, h2), edges, 800, 600);
        assertNotEquals(h1.y, h2.y);
    }
    @Test void apply_hostWithoutHostEdges_stayAtDefaultOrigin() {
        GuiNetworkMap.Node gw = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node host = node("10.0.0.9", GuiNetworkMap.NodeType.HOST);
        MapLayout.apply(List.of(gw, host), List.of(), 800, 600);
        assertEquals(0, host.x); assertEquals(0, host.y);
    }
}
