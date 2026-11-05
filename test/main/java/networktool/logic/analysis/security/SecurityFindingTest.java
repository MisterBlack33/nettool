package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

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
        assertEquals(SecurityFinding.Source.CVE_BANNER_MATCH, f.source());
        assertEquals(SecurityFinding.Confidence.UNKNOWN, f.confidence());
        assertNotNull(f.observedAt());
    }

    @Test void explicitEvidenceMetadata_isPreserved() {
        Instant observedAt = Instant.parse("2026-10-29T12:34:56Z");
        SecurityFinding finding = new SecurityFinding("1.2.3.4", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.WARN, "certificate expires soon",
                SecurityFinding.Source.TLS_CERT_INSPECTOR, SecurityFinding.Confidence.CONFIRMED, observedAt);

        assertEquals(SecurityFinding.Source.TLS_CERT_INSPECTOR, finding.source());
        assertEquals(SecurityFinding.Confidence.CONFIRMED, finding.confidence());
        assertEquals(observedAt, finding.observedAt());
    }

    @Test void categories_haveExpectedValues() {
        assertEquals(4, SecurityFinding.Category.values().length);
    }

    @Test void severities_haveExpectedValues() {
        assertEquals(3, SecurityFinding.Severity.values().length);
    }

    @Test void confidence_valuesHaveExplicitUnknownFallback() {
        assertEquals(SecurityFinding.Confidence.UNKNOWN, SecurityFinding.Confidence.valueOf("UNKNOWN"));
    }
}
