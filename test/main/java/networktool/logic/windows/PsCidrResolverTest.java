package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PsCidrResolverTest {
    @Test void resolveCidrs_doesNotThrow() { assertDoesNotThrow(PsCidrResolver::resolveCidrs); }
    @Test void resolveCidrs_notNull() { assertNotNull(PsCidrResolver.resolveCidrs()); }
    @Test void resolveCidrs_entriesHaveSlash() {
        for (String c : PsCidrResolver.resolveCidrs()) assertTrue(c.contains("/"));
    }
}