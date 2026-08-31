package main.java.networktool.storage.network;

import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class HostSchemaMigrationV2Test {

    private String migrate(String hostJson) throws Exception {
        Method m = HostSchemaMigration.class.getDeclaredMethod("migrateHostV1ToV2", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, hostJson);
    }

    private static final String V1_HOST =
            "{\"ip\":\"10.0.0.5\",\"hostname\":\"h\",\"os\":\"Linux\",\"savedAt\":\"2024\","
                    + "\"ports\":{},\"notes\":\"note\"}";

    @Test void migrate_addsLastSeenAtField() throws Exception {
        String migrated = migrate(V1_HOST);
        assertTrue(migrated.contains("\"lastSeenAt\""));
    }

    @Test void migrate_preservesExistingFields() throws Exception {
        String migrated = migrate(V1_HOST);
        HostResult h = HostJsonBuilder.parseHost(migrated);
        assertNotNull(h);
        assertEquals("10.0.0.5", h.ip);
        assertEquals("h", h.hostname);
        assertEquals("Linux", h.os);
        assertEquals("note", h.notes);
    }

    @Test void migrate_isIdempotent() throws Exception {
        String once  = migrate(V1_HOST);
        String twice = migrate(once);
        assertEquals(once, twice);
    }

    @Test void migrate_null_returnsNull() throws Exception {
        assertNull(migrate(null));
    }

    @Test void migrate_malformedJson_noBrace_returnsUnchanged() throws Exception {
        assertEquals("not json", migrate("not json"));
    }

    @Test void logIfLegacy_doesNotThrow() {
        assertDoesNotThrow(() -> HostSchemaMigration.logIfLegacy("Net", 0));
        assertDoesNotThrow(() -> HostSchemaMigration.logIfLegacy("Net", HostJsonBuilder.CURRENT_SCHEMA_VERSION));
    }

    @Test void schemaV2Constant_isTwo() {
        assertEquals(2, HostSchemaMigration.SCHEMA_V2);
    }

    /** Belegt, dass die Migration im echten Lade-Pfad (parseHost) greift, kein Dead Code mehr. */
    @Test void parseHost_migratesLegacyV1Json_withoutExplicitMigrationCall() {
        HostResult h = HostJsonBuilder.parseHost(V1_HOST);
        assertNotNull(h);
        assertEquals("10.0.0.5", h.ip);
        assertEquals("note", h.notes);
    }
}