package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class TrafficSonifierConfigTest {
    TrafficSonifier s = TrafficSonifier.getInstance();

    @AfterEach void stop() { s.stop(); }

    @Test void setConfig_getConfig_roundtrip() {
        SonifyConfig cfg = new SonifyConfig();
        cfg.highHz = 500;
        s.setConfig(cfg);
        assertEquals(500, s.getConfig().highHz);
    }
    @Test void getConfig_returnsCopy() {
        SonifyConfig cfg = s.getConfig();
        cfg.highHz = -1;
        assertNotEquals(-1, s.getConfig().highHz);
    }
    @Test void start_storesActiveInterface() {
        s.start("wlan0");
        assertEquals("wlan0", s.getActiveInterface());
    }
    @Test void start_thenStop_notActive() {
        s.start("eth1");
        s.stop();
        assertFalse(s.isActive());
    }
}