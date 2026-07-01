package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PsArpResolverTest {
    @Test void lookup_unknownIp_returnsNull() { assertNull(PsArpResolver.lookup("192.0.2.1")); }
    @Test void table_notNull() { assertNotNull(PsArpResolver.table()); }
    @Test void lookup_doesNotThrow() { assertDoesNotThrow(() -> PsArpResolver.lookup("1.1.1.1")); }
}