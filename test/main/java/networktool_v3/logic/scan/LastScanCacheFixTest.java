package main.java.networktool_v3.logic.scan;

import main.java.networktool.logic.scan.LastScanCache;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für LastScanCache — jetzt korrekt über GuiTableRenderer verdrahtet. */
class LastScanCacheFixTest {

    @BeforeEach
    void clear() { LastScanCache.updateFromHostResults(List.of()); }

    @Test
    void isEmpty_afterClear() {
        assertTrue(LastScanCache.isEmpty());
    }

    @Test
    void updateFromHostResults_storesData() {
        LastScanCache.updateFromHostResults(
                List.of(new HostResult("1.1.1.1", "host [AA:BB:CC]", "Linux")));
        assertFalse(LastScanCache.isEmpty());
        assertEquals("1.1.1.1", LastScanCache.getAll().get(0).ip());
    }

    @Test
    void updateFromHostResults_stripsMAC() {
        LastScanCache.updateFromHostResults(
                List.of(new HostResult("1.1.1.1", "server [AA:BB:CC:DD:EE:FF]", "Linux")));
        assertEquals("server", LastScanCache.getAll().get(0).hostname());
    }

    @Test
    void updateFromHostResults_noMac_unchanged() {
        LastScanCache.updateFromHostResults(
                List.of(new HostResult("2.2.2.2", "plain-host", "Win")));
        assertEquals("plain-host", LastScanCache.getAll().get(0).hostname());
    }

    @Test
    void updateFromScanResults_storesData() {
        LastScanCache.updateFromScanResults(
                List.of(new ScanResult("3.3.3.3", "srv", new HashMap<>(), "Linux")));
        assertFalse(LastScanCache.isEmpty());
        assertEquals("3.3.3.3", LastScanCache.getAll().get(0).ip());
    }

    @Test
    void updateFromScanResults_replacesOld() {
        LastScanCache.updateFromHostResults(
                List.of(new HostResult("1.1.1.1", "h", "Linux")));
        LastScanCache.updateFromScanResults(
                List.of(new ScanResult("9.9.9.9", "new", new HashMap<>(), "Win")));
        assertEquals(1, LastScanCache.getAll().size());
        assertEquals("9.9.9.9", LastScanCache.getAll().get(0).ip());
    }

    @Test
    void getAll_isUnmodifiable() {
        LastScanCache.updateFromHostResults(
                List.of(new HostResult("1.1.1.1", "h", "Linux")));
        assertThrows(UnsupportedOperationException.class,
                () -> LastScanCache.getAll().clear());
    }

    @Test
    void record_fieldsAccessible() {
        LastScanCache.updateFromScanResults(
                List.of(new ScanResult("4.4.4.4", "host4", new HashMap<>(), "macOS")));
        LastScanCache.CachedHost h = LastScanCache.getAll().get(0);
        assertEquals("4.4.4.4", h.ip());
        assertEquals("host4", h.hostname());
        assertEquals("macOS", h.os());
    }
}