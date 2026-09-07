package main.java.networktool.logic.visualize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrafficSampleTest {

    @Test void fields_accessible() {
        TrafficSample s = new TrafficSample(1000L, 200L, 50L);
        assertEquals(1000L, s.timestampMs());
        assertEquals(200L,  s.rxDelta());
        assertEquals(50L,   s.txDelta());
    }

    @Test void zeroDeltas_allowed() {
        TrafficSample s = new TrafficSample(0L, 0L, 0L);
        assertEquals(0L, s.rxDelta());
        assertEquals(0L, s.txDelta());
    }

    @Test void equals_sameValues() {
        assertEquals(new TrafficSample(1, 2, 3), new TrafficSample(1, 2, 3));
    }

    @Test void equals_differentValues() {
        assertNotEquals(new TrafficSample(1, 2, 3), new TrafficSample(1, 2, 4));
    }

    @Test void hashCode_consistentWithEquals() {
        assertEquals(new TrafficSample(1, 2, 3).hashCode(), new TrafficSample(1, 2, 3).hashCode());
    }

    @Test void toString_containsFieldValues() {
        String s = new TrafficSample(5, 6, 7).toString();
        assertTrue(s.contains("5") && s.contains("6") && s.contains("7"));
    }
}