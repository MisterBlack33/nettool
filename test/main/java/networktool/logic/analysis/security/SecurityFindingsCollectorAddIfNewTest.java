package main.java.networktool.logic.analysis.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class SecurityFindingsCollectorAddIfNewTest {

    private final SecurityFindingsCollector c = SecurityFindingsCollector.getInstance();

    private static SecurityFinding f(String ip) {
        return new SecurityFinding(ip, SecurityFinding.Category.TLS_CERT, SecurityFinding.Severity.WARN, "x");
    }

    @BeforeEach
    void setUp() {
        c.clear();
        FindingsSourceRegistry.register(null);
        FindingsSourceRegistry.register(c);
    }

    @AfterEach
    void tearDown() {
        c.clear();
        FindingsSourceRegistry.register(null);
    }

    @Test void addIfNew_new_true()        { assertTrue(c.addIfNew(f("1.1.1.1"))); }
    @Test void addIfNew_duplicate_false() { c.addIfNew(f("1.1.1.1")); assertFalse(c.addIfNew(f("1.1.1.1"))); }
    @Test void addIfNew_null_false()      { assertFalse(c.addIfNew(null)); }
    @Test void addIfNew_differentIp_both() { c.addIfNew(f("1.1.1.1")); c.addIfNew(f("2.2.2.2")); assertEquals(2, c.getAll().size()); }
}
