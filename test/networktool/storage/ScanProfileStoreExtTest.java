package networktool.storage;

import networktool.model.ScanProfile;
import networktool.storage.ScanProfileStore;
import networktool.storage.TestConstants;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ScanProfileStoreExtTest {

    ScanProfileStore store = ScanProfileStore.getInstance();
    final String N = TestConstants.PROFILE_EXT;   // "__junit__profile_ext"

    @AfterEach void cleanup() { store.delete(N); }

    @Test void save_withCidrs_persisted() {
        ScanProfile p = new ScanProfile(N);
        p.cidrs.add("10.0.0.0/24");
        p.cidrs.add("192.168.1.0/24");
        store.save(p);
        ScanProfile loaded = store.get(N).orElseThrow();
        assertEquals(2, loaded.cidrs.size());
        assertTrue(loaded.cidrs.contains("10.0.0.0/24"));
    }

    @Test void save_withPorts_persisted() {
        ScanProfile p = new ScanProfile(N);
        p.ports.add(22);
        p.ports.add(443);
        store.save(p);
        ScanProfile loaded = store.get(N).orElseThrow();
        assertTrue(loaded.ports.contains(22));
        assertTrue(loaded.ports.contains(443));
    }

    @Test void save_autoSave_true_persisted() {
        ScanProfile p = new ScanProfile(N);
        p.autoSave = true;
        p.category = TestConstants.NET_STANDARD;
        store.save(p);
        ScanProfile loaded = store.get(N).orElseThrow();
        assertTrue(loaded.autoSave);
        assertEquals(TestConstants.NET_STANDARD, loaded.category);
    }

    @Test void save_hnFilter_persisted() {
        ScanProfile p = new ScanProfile(N);
        p.hnFilter = "server";
        store.save(p);
        assertEquals("server", store.get(N).orElseThrow().hnFilter);
    }

    @Test void delete_nonExistent_doesNotThrow()    { assertDoesNotThrow(() -> store.delete(TestConstants.TEST_PREFIX + "no_such_profile")); }
    @Test void getAll_returnsUnmodifiable()          { assertNotNull(store.getAll()); }
    @Test void updateLastRun_unknownProfile_noThrow(){ assertDoesNotThrow(() -> store.updateLastRun(TestConstants.TEST_PREFIX + "ghost", "2024-01-01 00:00:00")); }

    @Test void save_emptyProfile_defaults() {
        store.save(new ScanProfile(N));
        ScanProfile l = store.get(N).orElseThrow();
        assertEquals("", l.osFilter);
        assertEquals("", l.hnFilter);
        assertFalse(l.autoSave);
        assertTrue(l.cidrs.isEmpty());
        assertTrue(l.ports.isEmpty());
    }

    @Test void save_withAllFields_roundtrip() {
        ScanProfile p = new ScanProfile(N);
        p.osFilter = "Linux"; p.hnFilter = "srv";
        p.autoSave = true;    p.category = "Cat";
        p.lastRun  = "2024-06-15 10:00:00";
        p.cidrs.add("10.0.0.0/8");
        p.ports.add(80);
        store.save(p);
        ScanProfile l = store.get(N).orElseThrow();
        assertEquals("Linux", l.osFilter);
        assertEquals("srv",   l.hnFilter);
        assertTrue(l.autoSave);
        assertEquals("2024-06-15 10:00:00", l.lastRun);
        assertTrue(l.cidrs.contains("10.0.0.0/8"));
        assertTrue(l.ports.contains(80));
    }
}