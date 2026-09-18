package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityFindingsCollectorTest {

    SecurityFindingsCollector collector = SecurityFindingsCollector.getInstance();

    @BeforeEach void clear() { collector.clear(); }

    @Test void add_and_getAll() {
        collector.add(new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, "ok"));
        assertEquals(1, collector.getAll().size());
    }

    @Test void add_null_ignored() {
        collector.add(null);
        assertTrue(collector.getAll().isEmpty());
    }

    @Test void clear_removesAll() {
        collector.add(new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, "ok"));
        collector.clear();
        assertTrue(collector.getAll().isEmpty());
    }

    @Test void getAll_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> collector.getAll().add(null));
    }

    @Test void implementsFindingsSource() {
        assertInstanceOf(FindingsSource.class, collector);
    }

    @Test void getInstance_returnsSingleton() {
        assertSame(SecurityFindingsCollector.getInstance(), SecurityFindingsCollector.getInstance());
    }

    /** Regression: Collector muss sich selbst als FindingsSource registrieren (sonst sieht das Dashboard nie echte Daten). */
    @Test void getInstance_selfRegistersInFindingsSourceRegistry() {
        collector.add(new SecurityFinding("2.2.2.2", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.CRITICAL, "cve"));
        assertEquals(1, FindingsSourceRegistry.getAll().size());
    }
}
