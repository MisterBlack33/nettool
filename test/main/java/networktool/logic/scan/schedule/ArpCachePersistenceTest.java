package main.java.networktool.logic.scan.schedule;

import main.java.networktool.storage.backup.CacheCrypto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArpCachePersistenceTest {

    @TempDir Path tmp;

    @BeforeEach void setup() {
        ArpCachePersistence.testDataDir = tmp;
        ArpCachePersistence.disable();
    }

    @AfterEach void cleanup() {
        ArpCachePersistence.disable();
        ArpCachePersistence.testDataDir = null;
    }

    @Test void disabledByDefault() {
        assertFalse(ArpCachePersistence.isEnabled());
    }

    @Test void save_whenDisabled_writesNothing() {
        ArpCachePersistence.save(Map.of("1.1.1.1", "AA:BB:CC:DD:EE:FF"));
        assertFalse(Files.exists(tmp.resolve("arp_cache.enc")));
    }

    @Test void enable_requiresNonBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> ArpCachePersistence.enable(""));
        assertThrows(IllegalArgumentException.class, () -> ArpCachePersistence.enable(null));
    }

    @Test void saveAndLoad_roundtrip() {
        ArpCachePersistence.enable("pw123456");
        ArpCachePersistence.save(Map.of("192.168.1.5", "AA:BB:CC:DD:EE:FF"));
        Map<String, String> loaded = ArpCachePersistence.load("pw123456");
        assertEquals("AA:BB:CC:DD:EE:FF", loaded.get("192.168.1.5"));
    }

    @Test void savedFile_neverContainsPlaintextMac() throws Exception {
        ArpCachePersistence.enable("pw123456");
        ArpCachePersistence.save(Map.of("10.0.0.9", "DE:AD:BE:EF:00:01"));
        String raw = Files.readString(tmp.resolve("arp_cache.enc"), StandardCharsets.UTF_8);
        assertFalse(raw.contains("DE:AD:BE:EF:00:01"));
        assertFalse(raw.contains("10.0.0.9"));
    }

    @Test void load_wrongPassword_returnsEmpty_noLeak() {
        ArpCachePersistence.enable("correct-pw");
        ArpCachePersistence.save(Map.of("1.2.3.4", "11:22:33:44:55:66"));
        assertTrue(ArpCachePersistence.load("wrong-pw").isEmpty());
    }

    @Test void load_noFile_returnsEmpty() {
        assertTrue(ArpCachePersistence.load("any-pw").isEmpty());
    }

    @Test void load_corruptFile_returnsEmpty_doesNotThrow() throws Exception {
        Files.writeString(tmp.resolve("arp_cache.enc"), "not-valid-encrypted-data",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        assertDoesNotThrow(() -> assertTrue(ArpCachePersistence.load("pw").isEmpty()));
    }

    @Test void disable_removesPersistedFile() {
        ArpCachePersistence.enable("pw123456");
        ArpCachePersistence.save(Map.of("1.1.1.1", "AA:BB:CC:DD:EE:FF"));
        assertTrue(Files.exists(tmp.resolve("arp_cache.enc")));
        ArpCachePersistence.disable();
        assertFalse(Files.exists(tmp.resolve("arp_cache.enc")));
        assertFalse(ArpCachePersistence.isEnabled());
    }

    @Test void load_expiredEntry_filteredOut() throws Exception {
        long expiredTs = System.currentTimeMillis() - (25L * 60 * 60 * 1000);
        String raw = "1.1.1.1\tAA:BB:CC:DD:EE:FF\t" + expiredTs + "\n";
        String encrypted = CacheCrypto.encrypt(raw, "pw123456");
        Files.writeString(tmp.resolve("arp_cache.enc"), encrypted, StandardCharsets.UTF_8);

        Map<String, String> loaded = ArpCachePersistence.load("pw123456");
        assertTrue(loaded.isEmpty());
    }

    @Test void load_expiredEntry_deletesStaleFile() throws Exception {
        long expiredTs = System.currentTimeMillis() - (25L * 60 * 60 * 1000);
        String raw = "1.1.1.1\tAA:BB:CC:DD:EE:FF\t" + expiredTs + "\n";
        Files.writeString(tmp.resolve("arp_cache.enc"), CacheCrypto.encrypt(raw, "pw123456"), StandardCharsets.UTF_8);

        ArpCachePersistence.load("pw123456");
        assertFalse(Files.exists(tmp.resolve("arp_cache.enc")));
    }

    @Test void load_mixedFreshAndExpired_onlyFreshReturned() throws Exception {
        long now     = System.currentTimeMillis();
        long expired = now - (25L * 60 * 60 * 1000);
        String raw = "1.1.1.1\tAA:AA:AA:AA:AA:AA\t" + expired + "\n"
                + "2.2.2.2\tBB:BB:BB:BB:BB:BB\t" + now + "\n";
        Files.writeString(tmp.resolve("arp_cache.enc"), CacheCrypto.encrypt(raw, "pw123456"), StandardCharsets.UTF_8);

        Map<String, String> loaded = ArpCachePersistence.load("pw123456");
        assertEquals(1, loaded.size());
        assertEquals("BB:BB:BB:BB:BB:BB", loaded.get("2.2.2.2"));
    }

    @Test void save_multipleEntries_allPersisted() {
        ArpCachePersistence.enable("pw123456");
        ArpCachePersistence.save(Map.of(
                "1.1.1.1", "AA:AA:AA:AA:AA:AA",
                "2.2.2.2", "BB:BB:BB:BB:BB:BB"));
        Map<String, String> loaded = ArpCachePersistence.load("pw123456");
        assertEquals(2, loaded.size());
    }
}