package networktool.storage;

import networktool.storage.NotificationHistory;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {

    // ══════════════════════════════════════════════════════════════
    //  JsonHelper
    // ══════════════════════════════════════════════════════════════

    @Nested
    class JsonHelperTest {

        @Test void extractStr_simple()          { assertEquals("alice", invokeExtractStr("{\"name\":\"alice\"}", "name")); }
        @Test void extractStr_missing_null()     { assertNull(invokeExtractStr("{\"a\":\"b\"}", "missing")); }
        @Test void esc_nullReturnsEmpty()         { assertEquals("", invokeEsc(null)); }
        @Test void esc_escapesQuotes()            { assertTrue(invokeEsc("say \"hi\"").contains("\\\"")); }
        @Test void esc_escapesBackslash()         { assertTrue(invokeEsc("a\\b").contains("\\\\")); }
        @Test void esc_escapesNewline()           { assertTrue(invokeEsc("a\nb").contains("\\n")); }

        @Test void buildStringArrayJson_roundtrip() throws Exception {
            List<String> items = List.of("alpha", "beta", "gamma");
            String json = invokeBuildStringArrayJson("topics", items);
            assertTrue(json.contains("alpha"));
            List<String> back = invokeExtractStringArray(json, "topics");
            assertEquals(3, back.size());
            assertTrue(back.containsAll(items));
        }

        @Test void extractStr_withEscapes() {
            String json = "{\"msg\":\"hello\\nworld\"}";
            String val = invokeExtractStr(json, "msg");
            assertNotNull(val);
            assertTrue(val.contains("\n") || val.contains("world"));
        }

        @Test void extractStringArray_empty()      { assertTrue(invokeExtractStringArray("{\"topics\":[]}", "topics").isEmpty()); }
        @Test void extractStringArray_missingKey() { assertTrue(invokeExtractStringArray("{}", "nothere").isEmpty()); }

        @Test void findArrayStart_found() throws Exception {
            int idx = invokeFindArrayStart("{\"hosts\":[1,2,3]}", "hosts");
            assertTrue(idx > 0);
        }

        @Test void findArrayStart_missing() throws Exception { assertEquals(-1, invokeFindArrayStart("{}", "hosts")); }

        @Test void extractObjects_twoObjects() throws Exception {
            assertEquals(2, invokeExtractObjects("[{\"a\":\"1\"},{\"a\":\"2\"}]", 0).size());
        }

        // ── Reflection helpers ─────────────────────────────────────────────

        private String invokeExtractStr(String json, String field) {
            try { var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("extractStr", String.class, String.class);
                m.setAccessible(true); return (String) m.invoke(null, json, field);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        private String invokeEsc(String s) {
            try { var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("esc", String.class);
                m.setAccessible(true); return (String) m.invoke(null, s);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        private String invokeBuildStringArrayJson(String key, List<String> items) {
            try { var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("buildStringArrayJson", String.class, List.class);
                m.setAccessible(true); return (String) m.invoke(null, key, items);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        @SuppressWarnings("unchecked")
        private List<String> invokeExtractStringArray(String json, String key) {
            try { var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("extractStringArray", String.class, String.class);
                m.setAccessible(true); return (List<String>) m.invoke(null, json, key);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        private int invokeFindArrayStart(String json, String key) throws Exception {
            var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("findArrayStart", String.class, String.class);
            m.setAccessible(true); return (int) m.invoke(null, json, key);
        }

        @SuppressWarnings("unchecked")
        private List<String> invokeExtractObjects(String json, int start) throws Exception {
            var m = Class.forName("networktool.storage.JsonHelper")
                    .getDeclaredMethod("extractObjects", String.class, int.class);
            m.setAccessible(true); return (List<String>) m.invoke(null, json, start);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NotificationHistory
    // ══════════════════════════════════════════════════════════════

    @Nested
    class NotificationHistoryTest {

        NotificationHistory hist = NotificationHistory.getInstance();

        @BeforeEach void setup() { hist.clear(); }

        @Test void add_and_getAll()          { hist.add("src1", "title1", "msg1"); assertEquals(1, hist.size()); assertEquals("msg1", hist.getAll().get(0).message); }
        @Test void clear_removesAll()        { hist.add("s", "t", "m"); hist.clear(); assertEquals(0, hist.size()); }
        @Test void maxEntries_respected()    { for (int i = 0; i < 510; i++) hist.add("s", "t", "m" + i); assertEquals(NotificationHistory.MAX_ENTRIES, hist.size()); }
        @Test void newestFirst()             { hist.add("s", "t", "first"); hist.add("s", "t", "second"); assertEquals("second", hist.getAll().get(0).message); }
        @Test void listener_calledOnAdd()    { int[] c = {0}; hist.addListener(() -> c[0]++); hist.add("s", "t", "m"); assertEquals(1, c[0]); }
        @Test void listener_calledOnClear()  { int[] c = {0}; hist.addListener(() -> c[0]++); hist.clear(); assertEquals(1, c[0]); }
    }
}