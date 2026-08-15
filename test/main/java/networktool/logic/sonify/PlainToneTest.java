package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlainToneTest {
    @Test void play_zeroVolume_doesNotThrow() {
        assertDoesNotThrow(() -> PlainTone.play(880, 100, 0f));
    }
    @Test void play_normal_doesNotThrow() {
        assertDoesNotThrow(() -> PlainTone.play(440, 50, 0.1f));
    }
    @Test void play_zeroDuration_doesNotThrow() {
        assertDoesNotThrow(() -> PlainTone.play(440, 0, 0.1f));
    }
}