package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TailscaleRouteSourceTest {

    private static final String JSON =
            "{\"PrimaryRoutes\":[\"10.0.0.0/8\",\"192.168.1.0/24\",\"192.168.1.5/32\"]}";

    @Test void routes_filtersByPrefixRange() {
        assertEquals(List.of("192.168.1.0/24"), new TailscaleRouteSource(() -> JSON).routes(16, 30));
    }

    @Test void routes_emptyStatus_empty() {
        assertTrue(new TailscaleRouteSource(() -> "").routes(16, 30).isEmpty());
    }

    @Test void read_doesNotThrow_evenWithoutTailscale() {
        assertNotNull(assertDoesNotThrow(() -> TailscaleRouteSource.read(16, 30)));
    }

    @Test void runCommand_missingExecutable_returnsEmpty() {
        assertEquals("", TailscaleRouteSource.runCommand(new String[]{"__no_such_command__"}));
    }

    @Test void runCommand_capturesOutput() {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        assertTrue(TailscaleRouteSource.runCommand(new String[]{java, "-version"}).contains("version"));
    }
}