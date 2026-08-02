package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrafficToneTest {

    @Test void outgoing_highFreq() {
        assertEquals(ToneFrequency.HIGH, TrafficTone.forBit(true, true, false).frequency());
    }
    @Test void incoming_lowFreq() {
        assertEquals(ToneFrequency.LOW, TrafficTone.forBit(true, false, false).frequency());
    }
    @Test void highVolume_longDuration() {
        assertEquals(ToneDuration.LONG, TrafficTone.forBit(true, true, true).duration());
    }
    @Test void lowVolume_shortDuration() {
        assertEquals(ToneDuration.SHORT, TrafficTone.forBit(true, true, false).duration());
    }
    @Test void bitZero_notAudible() {
        assertFalse(TrafficTone.forBit(false, true, true).audible());
    }
    @Test void enumValues_haveExpectedHz() {
        assertEquals(880, ToneFrequency.HIGH.hz);
        assertEquals(220, ToneFrequency.LOW.hz);
    }
    @Test void enumValues_haveExpectedMs() {
        assertEquals(260, ToneDuration.LONG.ms);
        assertEquals(80,  ToneDuration.SHORT.ms);
    }
}