package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class SecurityFindingsCollectorTest {

    private final SecurityFindingsCollector collector = SecurityFindingsCollector.getInstance();

    @BeforeEach
    void resetState() {
        collector.clear();
        FindingsSourceRegistry.register(null);
        FindingsSourceRegistry.register(collector);
    }

    @AfterEach
    void tearDown() {
        collector.clear();
        FindingsSourceRegistry.register(null);
    }

    @Test void add_and_getAll() {
        SecurityFinding finding = new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, "ok");
        collector.add(finding);
        assertEquals(List.of(finding), collector.getAll());
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
        collector.add(new SecurityFinding("1.1.1.1", SecurityFinding.Category.TLS_CERT,
                SecurityFinding.Severity.INFO, "ok"));
        assertThrows(UnsupportedOperationException.class, () -> collector.getAll().add(null));
    }

    @Test void implementsFindingsSource() {
        assertInstanceOf(FindingsSource.class, collector);
    }

    @Test void getInstance_returnsSingleton() {
        assertSame(SecurityFindingsCollector.getInstance(), SecurityFindingsCollector.getInstance());
    }

    /** Regression-Test: Die Singleton-Instanz muss sich selbst in der Registry registrieren und dabei nicht auf fremden Zustand reagieren. */
    @Test void getInstance_selfRegistersInFindingsSourceRegistry() {
        SecurityFinding finding = new SecurityFinding("2.2.2.2", SecurityFinding.Category.KNOWN_VULNERABLE,
                SecurityFinding.Severity.CRITICAL, "cve");
        collector.add(finding);
        assertEquals(List.of(finding), FindingsSourceRegistry.getAll());
    }
}
