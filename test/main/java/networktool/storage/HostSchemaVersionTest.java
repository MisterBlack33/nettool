package main.java.networktool.storage;

import main.java.networktool.model.HostResult;
import networktool.storage.HostJsonBuilder;
import networktool.storage.HostSchemaMigration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HostSchemaVersionTest {

    @Test void buildNetworkJson_includesCurrentSchemaVersion() {
        String json = HostJsonBuilder.buildNetworkJson("Net", "", List.of());
        assertEquals(HostJsonBuilder.CURRENT_SCHEMA_VERSION, HostJsonBuilder.readSchemaVersion(json));
    }

    @Test void readSchemaVersion_missing_returnsZero() {
        assertEquals(0, HostJsonBuilder.readSchemaVersion("{\"network\":\"x\",\"hosts\":[]}"));
    }

    @Test void readSchemaVersion_negativeOrGarbage_null() {
        assertEquals(0, HostJsonBuilder.readSchemaVersion("{\"schemaVersion\":\"notanumber\"}"));
    }

    @Test void legacyFile_stillParsesHosts() {
        // Altbestand ohne schemaVersion darf weiterhin vollständig geladen werden.
        String legacy = "{\n  \"network\": \"Old\",\n  \"prefix\": \"\",\n  \"hosts\": [\n"
                + "    {\"ip\":\"1.2.3.4\",\"hostname\":\"h\",\"os\":\"Linux\",\"savedAt\":\"\",\"ports\":{},\"notes\":\"\"}\n"
                + "  ]\n}";
        assertEquals(0, HostJsonBuilder.readSchemaVersion(legacy));
        HostResult h = HostJsonBuilder.parseHost(
                legacy.substring(legacy.indexOf('{', legacy.indexOf("hosts")),
                        legacy.lastIndexOf('}', legacy.lastIndexOf('}') - 1) + 1));
        assertEquals("1.2.3.4", h.ip);
    }

    @Test void migrationLog_doesNotThrow_onLegacyOrCurrent() {
        assertDoesNotThrow(() -> HostSchemaMigration.logIfLegacy("Net", 0));
        assertDoesNotThrow(() -> HostSchemaMigration.logIfLegacy("Net", HostJsonBuilder.CURRENT_SCHEMA_VERSION));
    }

    @Test void extractInt_valid() {
        assertEquals(42, JsonHelper.extractInt("{\"n\":42}", "n"));
    }

    @Test void extractInt_negative() {
        assertEquals(-5, JsonHelper.extractInt("{\"n\":-5}", "n"));
    }

    @Test void extractInt_missing_null() {
        assertNull(JsonHelper.extractInt("{\"a\":1}", "n"));
    }

    @Test void extractInt_nonNumeric_null() {
        assertNull(JsonHelper.extractInt("{\"n\":\"x\"}", "n"));
    }

    @Test void extractStr_unicodeEscape_decoded() {
        assertEquals("café", JsonHelper.extractStr("{\"s\":\"caf\\u00e9\"}", "s"));
    }

    @Test void extractStr_malformedUnicodeEscape_keptRaw() {
        String result = JsonHelper.extractStr("{\"s\":\"a\\uZZZZb\"}", "s");
        assertTrue(result.contains("\\u"));
    }

    @Test void extractStr_slashEscape() {
        assertEquals("a/b", JsonHelper.extractStr("{\"s\":\"a\\/b\"}", "s"));
    }

    @Test void extractStr_carriageReturnEscape() {
        assertEquals("a\rb", JsonHelper.extractStr("{\"s\":\"a\\rb\"}", "s"));
    }
}
