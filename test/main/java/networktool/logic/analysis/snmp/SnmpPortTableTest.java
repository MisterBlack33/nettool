package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnmpPortTableTest {

    private static SnmpVarBind bind(String oid, String value) {
        return new SnmpVarBind(SnmpOid.parse(oid), value);
    }

    private static FakeSnmpAgent switchAgent() {
        return new FakeSnmpAgent()
                .text("1.3.6.1.2.1.2.2.1.2.1", "GigabitEthernet1/0/1")
                .text("1.3.6.1.2.1.2.2.1.2.2", "GigabitEthernet1/0/2")
                .gauge("1.3.6.1.2.1.2.2.1.5.1", 1_000_000_000L)
                .gauge("1.3.6.1.2.1.2.2.1.5.2", 100_000_000L)
                .integer("1.3.6.1.2.1.2.2.1.8.1", 1)
                .integer("1.3.6.1.2.1.2.2.1.8.2", 2);
    }

    @Test void read_buildsRowsFromAgent() {
        List<SnmpPortTable.Row> rows = SnmpPortTable.read(new SnmpWalker(switchAgent(), "public"));
        assertEquals(2, rows.size());
        assertEquals("GigabitEthernet1/0/1", rows.get(0).name());
        assertTrue(rows.get(0).up());
        assertEquals(1_000_000_000L, rows.get(0).speedBps());
        assertFalse(rows.get(1).up());
    }

    @Test void read_emptyAgent_returnsEmpty() {
        assertTrue(SnmpPortTable.read(new SnmpWalker(new FakeSnmpAgent(), "public")).isEmpty());
    }

    @Test void build_missingStatusAndSpeed_defaultsToDown() {
        var rows = SnmpPortTable.build(List.of(bind("1.3.6.1.2.1.2.2.1.2.5", "lo")), List.of(), List.of());
        assertFalse(rows.get(0).up());
        assertEquals(0, rows.get(0).speedBps());
    }

    @Test void build_sortsByIndex() {
        var rows = SnmpPortTable.build(List.of(
                bind("1.3.6.1.2.1.2.2.1.2.10", "b"), bind("1.3.6.1.2.1.2.2.1.2.2", "a")), List.of(), List.of());
        assertEquals(List.of(2, 10), rows.stream().map(SnmpPortTable.Row::index).toList());
    }

    @Test void build_garbageSpeed_becomesZero() {
        var rows = SnmpPortTable.build(List.of(bind("1.3.6.1.2.1.2.2.1.2.1", "p")), List.of(),
                List.of(bind("1.3.6.1.2.1.2.2.1.5.1", "abc")));
        assertEquals(0, rows.get(0).speedBps());
    }

    @Test void speedLabel_variants() {
        assertEquals("–", new SnmpPortTable.Row(1, "p", true, 0).speedLabel());
        assertEquals("64 kbit/s", new SnmpPortTable.Row(1, "p", true, 64_000).speedLabel());
        assertEquals("1000 Mbit/s", new SnmpPortTable.Row(1, "p", true, 1_000_000_000L).speedLabel());
    }

    @Test void toLine_containsNameAndStatus() {
        String line = new SnmpPortTable.Row(3, "Port3", true, 100_000_000L).toLine();
        assertTrue(line.contains("Port3") && line.contains("UP") && line.contains("100 Mbit/s"));
    }

    @Test void toLine_downStatus() {
        assertTrue(new SnmpPortTable.Row(1, "p", false, 0).toLine().contains("DOWN"));
    }
}
