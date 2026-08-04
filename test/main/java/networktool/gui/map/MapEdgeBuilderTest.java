package networktool.gui.map;

import networktool.gui.components.GuiNetworkMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapEdgeBuilderTest {

    private GuiNetworkMap.Node node(String ip, GuiNetworkMap.NodeType type) {
        return new GuiNetworkMap.Node(ip, ip, "", type);
    }

    @AfterEach
    void cleanup() {
        MapSwitchStore.remove("10.0.0.250");
    }

    @Test void nullGatewayYieldsNoEdges() {
        GuiNetworkMap.Node host = node("10.0.0.5", GuiNetworkMap.NodeType.HOST);
        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(List.of(host), null, null, Map.of());
        assertTrue(edges.isEmpty());
    }

    @Test void hostWithoutSwitchConnectsToGateway() {
        GuiNetworkMap.Node gw   = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node host = node("10.0.0.5", GuiNetworkMap.NodeType.HOST);
        List<GuiNetworkMap.Node> nodes = List.of(gw, host);

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(nodes, gw, null, Map.of());

        assertEquals(1, edges.size());
        assertSame(host, edges.get(0).from);
        assertSame(gw,   edges.get(0).to);
        assertEquals(GuiNetworkMap.EdgeType.NORMAL, edges.get(0).type);
    }

    @Test void selfNodeGetsSelfLinkEdge() {
        GuiNetworkMap.Node gw   = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node self = node("10.0.0.9", GuiNetworkMap.NodeType.SELF);

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(List.of(gw, self), gw, self, Map.of());

        assertTrue(edges.stream().anyMatch(e ->
                e.type == GuiNetworkMap.EdgeType.SELF_LINK && e.from == self && e.to == gw));
    }

    @Test void switchesUplinkToGateway() {
        GuiNetworkMap.Node gw = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node sw = node("10.0.0.2", GuiNetworkMap.NodeType.SWITCH);

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(List.of(gw, sw), gw, null, Map.of());

        assertTrue(edges.stream().anyMatch(e ->
                e.type == GuiNetworkMap.EdgeType.UPLINK && e.from == sw && e.to == gw));
    }

    @Test void hostInSameSubnetAsSwitchConnectsToSwitch() {
        GuiNetworkMap.Node gw   = node("10.0.0.1",  GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node sw   = node("10.0.0.2",  GuiNetworkMap.NodeType.SWITCH);
        GuiNetworkMap.Node host = node("10.0.0.50", GuiNetworkMap.NodeType.HOST);

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(List.of(gw, sw, host), gw, null, Map.of());

        GuiNetworkMap.Edge hostEdge = edges.stream()
                .filter(e -> e.from == host).findFirst().orElseThrow();
        assertSame(sw, hostEdge.to, "Host im selben /24 wie ein Switch sollte an diesen andocken");
    }

    @Test void hopParentCreatesIntermediateSwitchNode() {
        GuiNetworkMap.Node gw   = node("10.0.0.1", GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node host = node("10.0.0.5", GuiNetworkMap.NodeType.HOST);
        List<GuiNetworkMap.Node> nodes = new java.util.ArrayList<>(List.of(gw, host));

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(
                nodes, gw, null, Map.of("10.0.0.5", "10.0.0.254"));

        // Zwischenknoten 10.0.0.254 sollte neu angelegt worden sein
        assertTrue(nodes.stream().anyMatch(n -> n.ip.equals("10.0.0.254")));
        GuiNetworkMap.Edge hostEdge = edges.stream()
                .filter(e -> e.from == host).findFirst().orElseThrow();
        assertEquals("10.0.0.254", hostEdge.to.ip);
    }

    @Test void manuallyMarkedSwitchIsPreferredOverNearestByOctet() {
        MapSwitchStore.add("10.0.0.250");
        GuiNetworkMap.Node gw     = node("10.0.0.1",   GuiNetworkMap.NodeType.GATEWAY);
        GuiNetworkMap.Node near   = node("10.0.0.49",  GuiNetworkMap.NodeType.SWITCH);
        GuiNetworkMap.Node manual = node("10.0.0.250", GuiNetworkMap.NodeType.SWITCH);
        GuiNetworkMap.Node host   = node("10.0.0.50",  GuiNetworkMap.NodeType.HOST);

        List<GuiNetworkMap.Edge> edges = MapEdgeBuilder.build(
                List.of(gw, near, manual, host), gw, null, Map.of());

        GuiNetworkMap.Edge hostEdge = edges.stream()
                .filter(e -> e.from == host).findFirst().orElseThrow();
        assertSame(manual, hostEdge.to, "Manuell markierter Switch hat Vorrang vor Oktett-Nähe");
    }
}
