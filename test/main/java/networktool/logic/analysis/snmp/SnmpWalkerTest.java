package main.java.networktool.logic.analysis.snmp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SnmpWalkerTest {

    private static final SnmpOid ROOT = SnmpOid.parse("1.3.6.1.2.1.2.2.1.2");

    private static FakeSnmpAgent agent() {
        return new FakeSnmpAgent()
                .text("1.3.6.1.2.1.2.2.1.2.1", "eth0")
                .text("1.3.6.1.2.1.2.2.1.2.2", "eth1")
                .text("1.3.6.1.2.1.2.2.1.2.3", "eth2")
                .integer("1.3.6.1.2.1.2.2.1.3.1", 6)
                .text("1.3.6.1.2.1.1.1.0", "Switch");
    }

    @Test void walk_returnsSubtreeInOrder() {
        List<SnmpVarBind> result = new SnmpWalker(agent(), "public").walk(ROOT);
        assertEquals(List.of("eth0", "eth1", "eth2"), result.stream().map(SnmpVarBind::value).toList());
    }

    @Test void walk_stopsAtSubtreeEnd_withoutEndOfMib() {
        FakeSnmpAgent agent = agent();
        new SnmpWalker(agent, "public").walk(ROOT);
        assertEquals(4, agent.requestCount);
    }

    @Test void walk_reachesEndOfMib() {
        FakeSnmpAgent agent = new FakeSnmpAgent().text("1.3.6.1.2.1.2.2.1.2.1", "eth0");
        assertEquals(1, new SnmpWalker(agent, "public").walk(ROOT).size());
    }

    @Test void walk_emptyMib_returnsEmpty() {
        assertTrue(new SnmpWalker(new FakeSnmpAgent(), "public").walk(ROOT).isEmpty());
    }

    @Test void walk_noResponse_returnsEmpty() {
        assertTrue(new SnmpWalker(request -> Optional.empty(), "public").walk(ROOT).isEmpty());
    }

    @Test void walk_garbageResponse_returnsEmpty() {
        assertTrue(new SnmpWalker(request -> Optional.of(new byte[]{1, 2}), "public").walk(ROOT).isEmpty());
    }

    @Test void walk_nonAscendingOid_terminates() {
        SnmpTransport stuck = request -> {
            FakeSnmpAgent.Request r = FakeSnmpAgent.parse(request);
            return Optional.of(FakeSnmpAgent.response(r.id(), 0, r.oid(), BerWriter.integer(1)));
        };
        assertTrue(new SnmpWalker(stuck, "public").walk(ROOT).isEmpty());
    }

    @Test void walk_endlessAgent_isCappedAtMaxEntries() {
        SnmpTransport endless = request -> {
            FakeSnmpAgent.Request r = FakeSnmpAgent.parse(request);
            return Optional.of(FakeSnmpAgent.response(r.id(), 0, r.oid().child(1), BerWriter.integer(1)));
        };
        assertEquals(SnmpWalker.MAX_ENTRIES, new SnmpWalker(endless, "public").walk(ROOT).size());
    }

    @Test void walk_resultIsImmutable() {
        List<SnmpVarBind> result = new SnmpWalker(agent(), "public").walk(ROOT);
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }

    @Test void forHost_createsWalker() {
        assertNotNull(SnmpWalker.forHost("192.0.2.1", "public"));
    }
}
