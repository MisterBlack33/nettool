package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FindingsSourceRegistryTest {

    @AfterEach void reset() { FindingsSourceRegistry.register(null); }

    @Test void getAll_noSource_returnsEmpty() {
        assertTrue(FindingsSourceRegistry.getAll().isEmpty());
    }

    @Test void register_thenGetAll_returnsFindings() {
        FindingsSource src = () -> List.of(
                new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                        SecurityFinding.Severity.CRITICAL, "expired cert"));
        FindingsSourceRegistry.register(src);
        assertEquals(1, FindingsSourceRegistry.getAll().size());
    }

    @Test void unregister_matchingSource_clears() {
        FindingsSource src = List::of;
        FindingsSourceRegistry.register(src);
        FindingsSourceRegistry.unregister(src);
        assertTrue(FindingsSourceRegistry.getAll().isEmpty());
    }

    @Test void unregister_nonMatchingSource_keepsCurrent() {
        FindingsSource src = () -> List.of(
                new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                        SecurityFinding.Severity.INFO, "x"));
        FindingsSourceRegistry.register(src);
        FindingsSourceRegistry.unregister(() -> List.of());
        assertEquals(1, FindingsSourceRegistry.getAll().size());
    }

    @Test void securityFinding_nullIpAndDetail_defaulted() {
        SecurityFinding f = new SecurityFinding(null, SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, null);
        assertEquals("", f.ip());
        assertEquals("", f.detail());
    }

    @Test void securityFinding_fieldsAccessible() {
        SecurityFinding f = new SecurityFinding("2.2.2.2", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.WARN, "d");
        assertEquals("2.2.2.2", f.ip());
        assertEquals(SecurityFinding.Category.KNOWN_VULNERABLE, f.category());
        assertEquals(SecurityFinding.Severity.WARN, f.severity());
        assertEquals("d", f.detail());
    }
}
