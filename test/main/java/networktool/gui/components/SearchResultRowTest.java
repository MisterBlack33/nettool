package main.java.networktool.gui.components;

import main.java.networktool.model.HostResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultRowTest {

    private HostResult host(String ip, String hostname, String os, String notes) {
        HostResult h = new HostResult(ip, hostname, os);
        h.notes = notes;
        return h;
    }

    @Test void matchesByIp() {
        assertTrue(SearchResultRow.matches(host("192.168.1.42", "pc", "Windows", null), "168.1.4"));
    }

    @Test void matchesByHostnameCaseInsensitive() {
        assertTrue(SearchResultRow.matches(host("1.1.1.1", "MeinLaptop", "Linux", null), "meinlaptop"));
    }

    @Test void matchesByOs() {
        assertTrue(SearchResultRow.matches(host("1.1.1.1", "h", "Windows 11 Pro", null), "windows"));
    }

    @Test void matchesByNotes() {
        assertTrue(SearchResultRow.matches(host("1.1.1.1", "h", "Linux", "Serverraum EG"), "serverraum"));
    }

    @Test void noMatchReturnsFalse() {
        assertFalse(SearchResultRow.matches(host("1.1.1.1", "h", "Linux", "keller"), "dachboden"));
    }

    @Test void nullFieldsDoNotThrow() {
        assertFalse(SearchResultRow.matches(host("1.1.1.1", null, null, null), "x"));
    }
}
