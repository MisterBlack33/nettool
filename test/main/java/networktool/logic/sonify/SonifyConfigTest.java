package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SonifyConfigTest {
    @Test void defaults_matchConstants() {
        SonifyConfig c = new SonifyConfig();
        assertEquals(SonifyConfig.DEFAULT_HIGH_HZ, c.highHz);
        assertEquals(SonifyConfig.DEFAULT_LOW_HZ, c.lowHz);
    }
    @Test void copy_isIndependent() {
        SonifyConfig c = new SonifyConfig();
        SonifyConfig cp = c.copy();
        cp.highHz = 999;
        assertNotEquals(cp.highHz, c.highHz);
    }
}