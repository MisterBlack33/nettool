package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TracerouteRunner.parseLine (package-private).
 * Must be in package main.java.networktool_v3.logic.analysis.
 */
class TracerouteRunnerPackageTest {

    @Test
    void parseLine_windows_normal() {
        String line = "  1    <1 ms    <1 ms    <1 ms  192.168.1.1";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertEquals(1, hop.number);
        assertEquals("192.168.1.1", hop.ip);
        assertFalse(hop.timeout);
    }

    @Test
    void parseLine_timeout() {
        String line = "  3     *        *        *     Request timed out.";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertTrue(hop.timeout);
    }

    @Test
    void parseLine_linux() {
        String line = " 2  router.local (10.0.0.1)  1.234 ms";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, false);
        assertNotNull(hop);
        assertEquals(2, hop.number);
        assertEquals("10.0.0.1", hop.ip);
    }

    @Test
    void parseLine_empty_returnsNull() {
        assertNull(TracerouteRunner.parseLine("", true));
    }

    @Test
    void parseLine_noNumber_returnsNull() {
        assertNull(TracerouteRunner.parseLine("no number here", false));
    }

    @Test
    void parseLine_multipleMs() {
        String line = "  5   12 ms   14 ms   13 ms  8.8.8.8";
        TracerouteRunner.HopInfo hop = TracerouteRunner.parseLine(line, true);
        assertNotNull(hop);
        assertFalse(hop.msValues.isEmpty());
    }

    @Test
    void hopInfo_latencyFormatted_single() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        hop.msValues.add(25L);
        assertEquals("25 ms", hop.latencyFormatted());
    }

    @Test
    void hopInfo_latencyFormatted_multiple() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        hop.msValues.addAll(List.of(10L, 20L, 30L));
        String s = hop.latencyFormatted();
        assertTrue(s.contains("10"));
        assertTrue(s.contains("30"));
    }

    @Test
    void hopInfo_latencyFormatted_empty() {
        TracerouteRunner.HopInfo hop = new TracerouteRunner.HopInfo(1);
        assertEquals("–", hop.latencyFormatted());
    }
}
