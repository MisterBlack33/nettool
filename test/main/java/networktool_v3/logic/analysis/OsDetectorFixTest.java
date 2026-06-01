package main.java.networktool_v3.logic.analysis;

import main.java.networktool.logic.analysis.OsDetector;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/** Tests für OsDetector-Fixes: tote extractOui() entfernt, ExecutorService-Leak geschlossen. */
class OsDetectorFixTest {

    @Test
    void extractOui_methodDoesNotExist() throws Exception {
        // extractOui() war @SuppressWarnings("unused") — soll gelöscht sein
        boolean found = false;
        for (Method m : OsDetector.class.getDeclaredMethods()) {
            if (m.getName().equals("extractOui")) { found = true; break; }
        }
        assertFalse(found, "extractOui() sollte entfernt worden sein");
    }

    @Test
    void detectWithConfidence_unreachable_returnsResult() {
        // darf nicht hängen bleiben (ExecutorService wird korrekt beendet)
        OsDetector.OsResult r = OsDetector.detectWithConfidence("192.0.2.1");
        assertNotNull(r);
        assertNotNull(r.confidence);
    }

    @Test
    void detect_doesNotThrowOnUnreachable() {
        assertDoesNotThrow(() -> OsDetector.detect("192.0.2.1"));
    }

    @Test
    void classifyHostname_null_returnsNull() {
        assertNull(OsDetector.classifyHostname(null));
    }

    @Test
    void classifyHostname_raspberry_detected() {
        assertEquals("Raspberry Pi (Linux)", OsDetector.classifyHostname("raspberrypi.local"));
    }
}