package main.java.networktool.storage;

import main.java.networktool.model.HostResult;
import networktool.storage.HostJsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fuzz-/Edge-Case-Tests für JsonHelper: Werte mit strukturellen Zeichen
 * ({, }, [, ], ,) innerhalb von JSON-Strings dürfen das Parsing nicht brechen.
 */
class JsonHelperFuzzTest {

    @SuppressWarnings("unchecked")
    private List<String> extractObjects(String json, int start) throws Exception {
        Method m = JsonHelper.class.getDeclaredMethod("extractObjects", String.class, int.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, json, start);
    }

    private int matchBracket(String json, int idx) throws Exception {
        Method m = JsonHelper.class.getDeclaredMethod("matchBracket", String.class, int.class);
        m.setAccessible(true);
        return (int) m.invoke(null, json, idx);
    }

    // ── extractObjects ───────────────────────────────────────────────────

    @Test void extractObjects_braceInStringValue_notMiscounted() throws Exception {
        String json = "[{\"a\":\"val{ue}\"},{\"a\":\"val}2\"}]";
        List<String> objs = extractObjects(json, 0);
        assertEquals(2, objs.size());
        assertTrue(objs.get(0).contains("val{ue}"));
    }

    @Test void extractObjects_bracketInStringValue_notMiscounted() throws Exception {
        String json = "[{\"note\":\"list [1,2]\"}]";
        assertEquals(1, extractObjects(json, 0).size());
    }

    @Test void extractObjects_commaInStringValue_singleObject() throws Exception {
        String json = "[{\"note\":\"a}, {fake\"}]";
        assertEquals(1, extractObjects(json, 0).size());
    }

    @Test void extractObjects_escapedQuoteInsideString_doesNotBreakScan() throws Exception {
        String json = "[{\"note\":\"say \\\"hi\\\" {here}\"},{\"note\":\"b\"}]";
        assertEquals(2, extractObjects(json, 0).size());
    }

    @Test void extractObjects_empty_returnsEmptyList() throws Exception {
        assertTrue(extractObjects("[]", 0).isEmpty());
    }

    // ── matchBracket ──────────────────────────────────────────────────────

    @Test void matchBracket_skipsStringContent() throws Exception {
        String json = "{\"ports\":{\"80\":\"HTTP | Nginx}v2\"}}";
        int s = json.indexOf('{', json.indexOf("ports"));
        assertEquals(json.length() - 2, matchBracket(json, s));
    }

    @Test void matchBracket_nested_objects() throws Exception {
        String json = "{\"a\":{\"b\":{\"c\":1}}}";
        int s = json.indexOf('{', json.indexOf("\"a\""));
        assertEquals(json.length() - 2, matchBracket(json, s));
    }

    @Test void matchBracket_unterminated_returnsMinusOne() throws Exception {
        assertEquals(-1, matchBracket("{\"a\":1", 0));
    }

    // ── HostJsonBuilder Integration ───────────────────────────────────────

    @Test void hostJsonBuilder_roundtrip_bannerWithBrace() {
        Map<Integer, String> ports = Map.of(80, "HTTP | {nginx}");
        var h = new HostResult(
                TestConstants.IP_1, TestConstants.HOST_1, "Linux", "2024", ports, "note {x}");
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", List.of(h));
        int start = json.indexOf('{', json.indexOf("\"hosts\""));
        String hostObj = json.substring(start, json.lastIndexOf('}', json.lastIndexOf('}') - 1) + 1);
        var parsed = HostJsonBuilder.parseHost(hostObj);
        assertNotNull(parsed);
        assertEquals("HTTP | {nginx}", parsed.getPorts().get(80));
        assertEquals("note {x}", parsed.notes);
    }

    @Test void hostJsonBuilder_roundtrip_notesWithBracketAndComma() {
        var h = new HostResult(
                TestConstants.IP_2, TestConstants.HOST_2, "Win", "2024", Map.of(), "list [1,2], done");
        String json = HostJsonBuilder.buildNetworkJson(TestConstants.NET_STANDARD, "", List.of(h));
        int start = json.indexOf('{', json.indexOf("\"hosts\""));
        String hostObj = json.substring(start, json.lastIndexOf('}', json.lastIndexOf('}') - 1) + 1);
        var parsed = HostJsonBuilder.parseHost(hostObj);
        assertEquals("list [1,2], done", parsed.notes);
    }

    // ── DataImporter Integration ─────────────────────────────────────────

    @Test void importJson_valueWithBraceComma_doesNotSplitIncorrectly(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("data.json");
        String json = "[{\"ip\":\"" + TestConstants.IP_1 + "\",\"hostname\":\"h\",\"os\":\"Linux\","
                + "\"savedAt\":\"\",\"ports\":\"\",\"notes\":\"a}, {b\",\"category\":\""
                + TestConstants.FIX_IMPORT_CAT + "\"}]";
        Files.writeString(f, json);
        assertEquals(1, DataImporter.importJson(f));
    }

    @Test void importJson_multipleEntries_allParsed(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("multi.json");
        String json = "[{\"ip\":\"" + TestConstants.IP_1 + "\",\"hostname\":\"a\",\"os\":\"Linux\","
                + "\"category\":\"" + TestConstants.FIX_IMPORT_CAT + "\"},"
                + "{\"ip\":\"" + TestConstants.IP_2 + "\",\"hostname\":\"b\",\"os\":\"Win\","
                + "\"category\":\"" + TestConstants.FIX_IMPORT_CAT + "\"}]";
        Files.writeString(f, json);
        assertEquals(2, DataImporter.importJson(f));
    }

    @Test void importJson_emptyArray_returnsZero(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("empty.json");
        Files.writeString(f, "[]");
        assertEquals(0, DataImporter.importJson(f));
    }

    @Test void importJson_noArray_returnsZero(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("bad.json");
        Files.writeString(f, "not json");
        assertEquals(0, DataImporter.importJson(f));
    }
}