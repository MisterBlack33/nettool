package main.java.networktool.gui.map;

import main.java.networktool.gui.components.map.GuiNetworkMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapExporterTest {

    private GuiNetworkMap.Node node(String ip) {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node(ip, ip, "", GuiNetworkMap.NodeType.HOST);
        n.x = 10; n.y = 20;
        return n;
    }

    @Test void toSvg_containsSvgTag() {
        String svg = MapExporter.toSvg(List.of(), List.of());
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.trim().endsWith("</svg>"));
    }

    @Test void toSvg_containsNodeCircleAndLabel() {
        GuiNetworkMap.Node n = node("1.2.3.4");
        String svg = MapExporter.toSvg(List.of(n), List.of());
        assertTrue(svg.contains("<circle"));
        assertTrue(svg.contains("1.2.3.4"));
    }

    @Test void toSvg_containsEdgeLine() {
        GuiNetworkMap.Node a = node("1.1.1.1");
        GuiNetworkMap.Node b = node("2.2.2.2");
        GuiNetworkMap.Edge e = new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.NORMAL);
        String svg = MapExporter.toSvg(List.of(a, b), List.of(e));
        assertTrue(svg.contains("<line"));
    }

    @Test void toSvg_escapesXmlChars() {
        GuiNetworkMap.Node n = node("host<evil>");
        String svg = MapExporter.toSvg(List.of(n), List.of());
        assertFalse(svg.contains("<evil>"));
        assertTrue(svg.contains("&lt;evil&gt;"));
    }

    @Test void toGraphml_hasHeaderAndGraphTag() {
        String gml = MapExporter.toGraphml(List.of(), List.of());
        assertTrue(gml.startsWith("<?xml"));
        assertTrue(gml.contains("<graphml"));
        assertTrue(gml.contains("<graph "));
    }

    @Test void toGraphml_containsNodeEntry() {
        String gml = MapExporter.toGraphml(List.of(node("5.5.5.5")), List.of());
        assertTrue(gml.contains("<node id=\"5.5.5.5\""));
    }

    @Test void toGraphml_containsEdgeEntry() {
        GuiNetworkMap.Node a = node("1.1.1.1");
        GuiNetworkMap.Node b = node("2.2.2.2");
        GuiNetworkMap.Edge e = new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.UPLINK);
        String gml = MapExporter.toGraphml(List.of(a, b), List.of(e));
        assertTrue(gml.contains("source=\"1.1.1.1\""));
        assertTrue(gml.contains("target=\"2.2.2.2\""));
    }

    @Test void toGraphml_escapesXmlChars() {
        String gml = MapExporter.toGraphml(List.of(node("a&b")), List.of());
        assertTrue(gml.contains("a&amp;b"));
    }

    @Test void emptyInputs_doNotThrow() {
        assertDoesNotThrow(() -> MapExporter.toSvg(List.of(), List.of()));
        assertDoesNotThrow(() -> MapExporter.toGraphml(List.of(), List.of()));
    }

    @Test void viewBox_empty_isDefault() {
        assertEquals("0 0 800 600", MapExporter.viewBox(List.of()));
    }

    @Test void viewBox_coversAllNodesWithMargin() {
        GuiNetworkMap.Node a = node("1.1.1.1"); a.x = -100; a.y = 0;
        GuiNetworkMap.Node b = node("2.2.2.2"); b.x = 1500; b.y = 900;
        assertEquals("-160 -60 1720 1020", MapExporter.viewBox(List.of(a, b)));
    }

    @Test void toSvg_edgeColorDependsOnType() {
        GuiNetworkMap.Node a = node("1.1.1.1");
        GuiNetworkMap.Node b = node("2.2.2.2");
        String uplink = MapExporter.toSvg(List.of(a, b),
                List.of(new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.UPLINK)));
        String self = MapExporter.toSvg(List.of(a, b),
                List.of(new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.SELF_LINK)));
        assertTrue(uplink.contains("#ffa030"));
        assertTrue(self.contains("#4cc260"));
    }

    @Test void toSvg_nodeHasFillColor() {
        assertTrue(MapExporter.toSvg(List.of(node("1.1.1.1")), List.of()).contains("fill=\"#"));
    }

    @Test void toGraphml_declaresKeysAndNodeType() {
        String gml = MapExporter.toGraphml(List.of(node("1.1.1.1")), List.of());
        assertTrue(gml.contains("<key id=\"type\""));
        assertTrue(gml.contains("<data key=\"type\">HOST</data>"));
    }

    @Test void toGraphml_edgeKind() {
        GuiNetworkMap.Node a = node("1.1.1.1");
        GuiNetworkMap.Node b = node("2.2.2.2");
        String gml = MapExporter.toGraphml(List.of(a, b),
                List.of(new GuiNetworkMap.Edge(a, b, GuiNetworkMap.EdgeType.NORMAL)));
        assertTrue(gml.contains("<data key=\"kind\">NORMAL</data>"));
    }

    @Test void toSvg_nullHostnameAndIp_doNotThrow() {
        GuiNetworkMap.Node n = new GuiNetworkMap.Node(null, null, null, GuiNetworkMap.NodeType.HOST);
        assertDoesNotThrow(() -> MapExporter.toSvg(List.of(n), List.of()));
        assertDoesNotThrow(() -> MapExporter.toGraphml(List.of(n), List.of()));
    }
}
