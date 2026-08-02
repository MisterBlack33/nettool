package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InterfaceStatsReaderTest {

    @Test void read_unknownIface_doesNotThrow() {
        assertDoesNotThrow(() -> InterfaceStatsReader.read("__no_such_iface__"));
    }

    @Test void read_unknownIface_nullOrValid() {
        long[] r = InterfaceStatsReader.read("__no_such_iface__");
        assertTrue(r == null || r.length == 2);
    }
}