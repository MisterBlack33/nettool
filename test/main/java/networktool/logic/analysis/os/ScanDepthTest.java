package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanDepthTest {

    @Test void values_containsAllThree() {
        assertEquals(3, ScanDepth.values().length);
        assertNotNull(ScanDepth.SCHNELL);
        assertNotNull(ScanDepth.STANDARD);
        assertNotNull(ScanDepth.GRUENDLICH);
    }

    @Test void valueOf_roundtrip() {
        assertEquals(ScanDepth.GRUENDLICH, ScanDepth.valueOf("GRUENDLICH"));
    }
}