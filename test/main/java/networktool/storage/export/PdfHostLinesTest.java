package main.java.networktool.storage.export;

import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfHostLinesTest {

    @Test void from_emptyHosts_headerOnly() {
        List<String> l = PdfHostLines.from(List.of(), "T");
        assertEquals("T", l.get(0));
        assertEquals("Hosts: 0", l.get(1));
    }

    @Test void from_hostLine_containsFields() {
        HostResult h = new HostResult("1.1.1.1", "srv", "Linux", null, Map.of(22, "SSH"));
        List<String> l = PdfHostLines.from(List.of(h), "T");
        assertEquals("1.1.1.1  srv  Linux  [22]", l.get(3));
    }

    @Test void from_nullFields_noNullText() {
        HostResult h = new HostResult("1.1.1.1", null, null);
        assertFalse(PdfHostLines.from(List.of(h), "T").get(3).contains("null"));
    }

    @Test void from_longLine_clipped() {
        HostResult h = new HostResult("1.1.1.1", "x".repeat(300), "Linux");
        String line = PdfHostLines.from(List.of(h), "T").get(3);
        assertTrue(line.length() <= 95);
        assertTrue(line.endsWith("…"));
    }
}
