package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class OsDetectorArpTest {
    private static String extractMac(String s) throws Exception {
        Method m = OsDetectorArp.class.getDeclaredMethod("extractMac", String.class);
        m.setAccessible(true); return (String) m.invoke(null, s);
    }
    private static boolean valid(String s) throws Exception {
        Method m = OsDetectorArp.class.getDeclaredMethod("isValidMac", String.class);
        m.setAccessible(true); return (boolean) m.invoke(null, s);
    }
    @Test void extractMac_formatsAndInvalidValues() throws Exception {
        assertEquals("AA:BB:CC:DD:EE:FF", extractMac("10.0.0.1 AA:BB:CC:DD:EE:FF dynamic"));
        assertEquals("AA:BB:CC:DD:EE:FF", extractMac("10.0.0.1 AA-BB-CC-DD-EE-FF dynamic"));
        assertNull(extractMac("no mac address here"));
        assertNull(extractMac("10.0.0.1 00:00:00:00:00:00 dynamic"));
        assertNull(extractMac("10.0.0.1 FF:FF:FF:FF:FF:FF static"));
    }
    @Test void validMac_rejectsInvalidBoundaries() throws Exception {
        assertFalse(valid(null)); assertFalse(valid("AA:BB")); assertFalse(valid("01:00:5E:00:00:01"));
        assertFalse(valid("00:00:00:00:00:00")); assertTrue(valid("B8:27:EB:11:22:33"));
    }
    @Tag("slow") @Test void getTtl_unreachable_returnsMinusOne() {
        assertEquals(-1, OsDetectorArp.getTtl("192.0.2.1"));
    }
    @Tag("slow") @Test void getMacFromArp_unreachable_doesNotThrow() {
        assertDoesNotThrow(() -> OsDetectorArp.getMacFromArp("192.0.2.1"));
    }
}
