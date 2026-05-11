package networktool_v3.storage;

import main.java.networktool_v3.logic.scan.ScanDelta;
import main.java.networktool_v3.logic.scan.ScanHistory;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;
import main4.networktool_v3.storage.NetworkStorePersistence;
import main.java.networktool_v3.storage.NotificationHistory;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// ── NotificationHistory ───────────────────────────────────────────────────────

class NotificationHistoryTest {

    private NotificationHistory hist;

    @BeforeEach
    void setUp() {
        hist = NotificationHistory.getInstance();
        hist.clear();
    }

    @Test
    void addAndRetrieve() {
        hist.add("TCP [1.2.3.4]", "Title", "Hello World");
        assertEquals(1, hist.size());
        NotificationHistory.Entry e = hist.getAll().get(0);
        assertEquals("TCP [1.2.3.4]", e.source);
        assertEquals("Title",         e.title);
        assertEquals("Hello World",   e.message);
    }

    @Test
    void newestFirst() {
        hist.add("src", "t1", "first");
        hist.add("src", "t2", "second");
        assertEquals("second", hist.getAll().get(0).message);
    }

    @Test
    void maxEntriesRespected() {
        for (int i = 0; i < NotificationHistory.MAX_ENTRIES + 10; i++)
            hist.add("src", "t", "msg" + i);
        assertEquals(NotificationHistory.MAX_ENTRIES, hist.size());
    }

    @Test
    void clearWorks() {
        hist.add("src", "t", "msg");
        hist.clear();
        assertEquals(0, hist.size());
    }

    @Test
    void listenerCalledOnAdd() {
        boolean[] called = {false};
        hist.addListener(() -> called[0] = true);
        hist.add("src", "t", "msg");
        assertTrue(called[0]);
    }

    @Test
    void timestampNotNull() {
        hist.add("src", "t", "msg");
        assertNotNull(hist.getAll().get(0).time);
        assertFalse(hist.getAll().get(0).time.isBlank());
    }
}

// ── ScanHistory ───────────────────────────────────────────────────────────────

class ScanHistoryTest {

    private ScanHistory history;

    @BeforeEach
    void setUp() {
        history = ScanHistory.getInstance();
        history.clear();
    }

    @Test
    void addAndGet() {
        List<ScanResult> results = List.of(
                new ScanResult("1.2.3.4", "host", Map.of(), "Linux")
        );
        history.add("test-cidr", results);
        assertTrue(history.getLast().isPresent());
        assertEquals("test-cidr", history.getLast().get().label);
    }

    @Test
    void sizeCorrect() {
        history.add("cidr1", List.of());
        history.add("cidr2", List.of());
        assertEquals(2, history.size());
    }

    @Test
    void maxHistoryRespected() {
        for (int i = 0; i < ScanHistory.MAX_HISTORY + 5; i++)
            history.add("cidr" + i, List.of());
        assertEquals(ScanHistory.MAX_HISTORY, history.size());
    }

    @Test
    void getByIndexWorks() {
        history.add("first", List.of());
        history.add("second", List.of());
        // Neueste zuerst
        assertEquals("second", history.get(0).get().label);
        assertEquals("first",  history.get(1).get().label);
    }

    @Test
    void getOutOfRangeEmpty() {
        assertTrue(history.get(99).isEmpty());
    }

    @Test
    void resultsAreImmutable() {
        List<ScanResult> r = new ArrayList<>(List.of(
                new ScanResult("1.2.3.4", "h", Map.of(), "Linux")));
        history.add("cidr", r);
        assertThrows(UnsupportedOperationException.class,
                () -> history.get(0).get().results.add(
                        new ScanResult("5.6.7.8", "h2", Map.of(), "Win")));
    }

    @Test
    void displayContainsLabelAndCount() {
        history.add("192.168.1.0/24", List.of(
                new ScanResult("1.2.3.4", "h", Map.of(), "Linux")));
        String display = history.get(0).get().display();
        assertTrue(display.contains("192.168.1.0/24"));
        assertTrue(display.contains("1"));
    }
}

// ── ScanDelta ─────────────────────────────────────────────────────────────────

class ScanDeltaTest {

    @Test
    void detectsNewHost() {
        List<ScanResult> before = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(), "Linux"));
        List<ScanResult> after = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(),      "Linux"),
                new ScanResult("1.1.1.2", "h2", Map.of(80,""), "Windows"));

        List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(before, after, "A", "B");
        assertTrue(delta.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.NEU
                && "1.1.1.2".equals(e.ip)));
    }

    @Test
    void detectsRemovedHost() {
        List<ScanResult> before = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(), "Linux"),
                new ScanResult("1.1.1.9", "h9", Map.of(), "Windows"));
        List<ScanResult> after = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(), "Linux"));

        List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(before, after, "A", "B");
        assertTrue(delta.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.WEG
                && "1.1.1.9".equals(e.ip)));
    }

    @Test
    void detectsOsChange() {
        List<ScanResult> before = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(), "Linux"));
        List<ScanResult> after = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(), "Windows"));

        List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(before, after, "A", "B");
        assertTrue(delta.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.OS_WECHSEL));
    }

    @Test
    void detectsPortChange() {
        List<ScanResult> before = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(80,"HTTP"), "Linux"));
        List<ScanResult> after = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(80,"HTTP", 443,"HTTPS"), "Linux"));

        List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(before, after, "A", "B");
        assertTrue(delta.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.PORT_AENDERUNG));
    }

    @Test
    void noDeltaWhenIdentical() {
        List<ScanResult> both = List.of(
                new ScanResult("1.1.1.1", "h1", Map.of(22,"SSH"), "Linux"));
        List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(both, both, "A", "B");
        assertTrue(delta.isEmpty());
    }

    @Test
    void hostDeltaDetectsNewHost() {
        List<HostResult> before = List.of(new HostResult("1.1.1.1","h1","Linux"));
        List<HostResult> after  = List.of(
                new HostResult("1.1.1.1","h1","Linux"),
                new HostResult("1.1.1.2","h2","Windows"));
        List<ScanDelta.DeltaEntry> delta = ScanDelta.compareHosts(before, after, "A", "B");
        assertTrue(delta.stream().anyMatch(e -> e.type == ScanDelta.ChangeType.NEU));
    }
}

// ── NetworkStorePersistence JSON helpers ──────────────────────────────────────

class NetworkStorePersistenceTest {

    @Test
    void extractJsonStr_simpleField() {
        String json = "{\"name\":\"Heim\",\"prefix\":\"192.168.1.\"}";
        assertEquals("Heim",       NetworkStorePersistence.extractJsonStr(json, "name"));
        assertEquals("192.168.1.", NetworkStorePersistence.extractJsonStr(json, "prefix"));
    }

    @Test
    void extractJsonStr_escapedQuote() {
        String json = "{\"notes\":\"say \\\"hello\\\"\"}";
        String val = NetworkStorePersistence.extractJsonStr(json, "notes");
        assertTrue(val != null && val.contains("\"hello\""));
    }

    @Test
    void extractJsonStr_missingField() {
        String json = "{\"name\":\"test\"}";
        assertNull(NetworkStorePersistence.extractJsonStr(json, "missing"));
    }

    @Test
    void escapeAndUnescape() {
        String original = "path\\to\\\"file\"";
        String escaped  = NetworkStorePersistence.esc(original);
        assertTrue(escaped.contains("\\\\"));
        assertTrue(escaped.contains("\\\""));
    }

    @Test
    void parsePorts_valid() {
        Map<Integer,String> ports = NetworkStorePersistence.parsePorts("80:HTTP,22:SSH");
        assertEquals("HTTP", ports.get(80));
        assertEquals("SSH",  ports.get(22));
    }

    @Test
    void parsePorts_empty() {
        Map<Integer,String> ports = NetworkStorePersistence.parsePorts("");
        assertTrue(ports.isEmpty());
    }

    @Test
    void parsePorts_null() {
        Map<Integer,String> ports = NetworkStorePersistence.parsePorts(null);
        assertTrue(ports.isEmpty());
    }
}
