package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityFindingTest {

    @Test void nullFields_defaulted() {
        SecurityFinding f = new SecurityFinding(null, SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, null);
        assertEquals("", f.ip());
        assertEquals("", f.detail());
    }

    @Test void fields_accessible() {
        SecurityFinding f = new SecurityFinding("1.2.3.4", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.CRITICAL, "test");
        assertEquals("1.2.3.4", f.ip());
        assertEquals(SecurityFinding.Category.KNOWN_VULNERABLE, f.category());
        assertEquals(SecurityFinding.Severity.CRITICAL, f.severity());
        assertEquals("test", f.detail());
    }

    @Test void categories_haveExpectedValues() {
        assertEquals(3, SecurityFinding.Category.values().length);
    }

    @Test void severities_haveExpectedValues() {
        assertEquals(3, SecurityFinding.Severity.values().length);
    }
}
