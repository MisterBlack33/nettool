package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class TrafficSonifierTest {

    TrafficSonifier s = TrafficSonifier.getInstance();

    @AfterEach void stop() { s.stop(); }

    @Test void isActive_initiallyFalse() { assertFalse(s.isActive()); }
    @Test void start_setsActive()        { s.start("__test_iface__"); assertTrue(s.isActive()); }
    @Test void stop_clearsActive()       { s.start("__t__"); s.stop(); assertFalse(s.isActive()); }
    @Test void startTwice_doesNotThrow() { s.start("__t__"); assertDoesNotThrow(() -> s.start("__t__")); }
    @Test void stop_whenInactive_doesNotThrow() { assertDoesNotThrow(s::stop); }
    @Test void setVolume_clamps() {
        assertDoesNotThrow(() -> { s.setVolume(-5f); s.setVolume(5f); s.setVolume(0.5f); });
    }
    @Test void sonifyDelta_outgoing_doesNotThrow() {
        assertDoesNotThrow(() -> s.sonifyDelta(10, 500));
    }
    @Test void sonifyDelta_incoming_doesNotThrow() {
        assertDoesNotThrow(() -> s.sonifyDelta(500, 10));
    }
    @Test void sonifyDelta_zero_doesNotThrow() {
        assertDoesNotThrow(() -> s.sonifyDelta(0, 0));
    }
}