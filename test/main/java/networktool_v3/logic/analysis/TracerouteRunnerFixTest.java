package main.java.networktool_v3.logic.analysis;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für TracerouteRunner (Fix: doppeltes Semikolon). */
class TracerouteRunnerFixTest {

    @Test
    void parseLine_noDoubleSemicolon_msValuesCorrect() {
        String line = "  1   12 ms   14 ms   13 ms  192.168.1.1";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        // Vor dem Fix: msValues hatte Duplikate durch doppeltes .add()
        assertEquals(3, hop.msValues.size());
    }

    @Test
    void parseLine_singleMs_noExtraEntry() {
        String line = " 2  10 ms  10 ms  10 ms  10.0.0.1";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertFalse(hop.msValues.isEmpty());
        // Kein Duplikat durch doppeltes Semikolon
        hop.msValues.forEach(v -> assertTrue(v >= 0));
    }

    @Test
    void parseLine_timeout_emptyMsValues() {
        String line = "  3     *        *        *     Request timed out.";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertTrue(hop.timeout);
        assertTrue(hop.msValues.isEmpty());
    }
}