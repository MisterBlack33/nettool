package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CveLookupTest {

    @Test void classify_knownBanner_matches() {
        var f = CveLookup.classify("1.1.1.1", "220 vsftpd 2.3.4 ready");
        assertTrue(f.isPresent());
        assertEquals(SecurityFinding.Category.KNOWN_VULNERABLE, f.get().category());
        assertTrue(f.get().detail().contains("CVE-2011-2523"));
    }

    @Test void classify_unknownBanner_empty() {
        assertTrue(CveLookup.classify("1.1.1.1", "totally unknown banner xyz").isEmpty());
    }

    @Test void classify_nullBanner_empty() {
        assertTrue(CveLookup.classify("1.1.1.1", null).isEmpty());
    }

    @Test void classify_blankBanner_empty() {
        assertTrue(CveLookup.classify("1.1.1.1", "   ").isEmpty());
    }

    @Test void classify_ipEchoedInFinding() {
        var f = CveLookup.classify("10.0.0.5", "ProFTPD 1.3.3 Server");
        assertTrue(f.isPresent());
        assertEquals("10.0.0.5", f.get().ip());
    }

    @Test void entries_loadedFromResource() {
        assertTrue(CveLookup.entryCount() > 0);
    }
}
