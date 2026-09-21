package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class FindingReporterTest {

    SecurityFindingsCollector sink = SecurityFindingsCollector.getInstance();
    FindingReporter reporter = new FindingReporter(sink);

    private static SecurityFinding finding(String ip) {
        return new SecurityFinding(ip, SecurityFinding.Category.ROGUE_DHCP,
                SecurityFinding.Severity.CRITICAL, "d");
    }

    @BeforeEach @AfterEach void clear() { sink.clear(); }

    @Test void report_new_returnsTrueAndStores() {
        assertTrue(reporter.report(finding("1.1.1.1")));
        assertEquals(1, sink.getAll().size());
    }

    @Test void report_duplicate_returnsFalse() {
        reporter.report(finding("1.1.1.1"));
        assertFalse(reporter.report(finding("1.1.1.1")));
        assertEquals(1, sink.getAll().size());
    }

    @Test void report_null_returnsFalse() {
        assertFalse(reporter.report(null));
    }
}
