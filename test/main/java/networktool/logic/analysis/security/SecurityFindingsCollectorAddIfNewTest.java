package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityFindingsCollectorAddIfNewTest {

    SecurityFindingsCollector c = SecurityFindingsCollector.getInstance();

    private static SecurityFinding f(String ip) {
        return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.WARN, "x");
    }

    @BeforeEach @AfterEach void clear() { c.clear(); }

    @Test void addIfNew_new_true()        { assertTrue(c.addIfNew(f("1.1.1.1"))); }
    @Test void addIfNew_duplicate_false() { c.addIfNew(f("1.1.1.1")); assertFalse(c.addIfNew(f("1.1.1.1"))); }
    @Test void addIfNew_null_false()      { assertFalse(c.addIfNew(null)); }
    @Test void addIfNew_differentIp_both() { c.addIfNew(f("1.1.1.1")); c.addIfNew(f("2.2.2.2")); assertEquals(2, c.getAll().size()); }
}
