package main.java.networktool.logic.scan.schedule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArpCacheEntryTest {

    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    @Test void isExpired_freshEntry_false() {
        long now = System.currentTimeMillis();
        assertFalse(new ArpCacheEntry("1.1.1.1", "AA:BB:CC:DD:EE:FF", now).isExpired(DAY_MS, now));
    }

    @Test void isExpired_justUnderTtl_false() {
        long now = System.currentTimeMillis();
        long ts  = now - (DAY_MS - 1000);
        assertFalse(new ArpCacheEntry("1.1.1.1", "AA:BB:CC:DD:EE:FF", ts).isExpired(DAY_MS, now));
    }

    @Test void isExpired_pastTtl_true() {
        long now = System.currentTimeMillis();
        long ts  = now - (DAY_MS + 1000);
        assertTrue(new ArpCacheEntry("1.1.1.1", "AA:BB:CC:DD:EE:FF", ts).isExpired(DAY_MS, now));
    }

    @Test void fields_accessible() {
        ArpCacheEntry e = new ArpCacheEntry("10.0.0.1", "00:11:22:33:44:55", 12345L);
        assertEquals("10.0.0.1", e.ip());
        assertEquals("00:11:22:33:44:55", e.mac());
        assertEquals(12345L, e.timestampMs());
    }
}