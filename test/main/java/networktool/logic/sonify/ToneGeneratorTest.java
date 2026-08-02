package main.java.networktool.logic.sonify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToneGeneratorTest {

    @Test void play_audible_doesNotThrow() {
        assertDoesNotThrow(() -> ToneGenerator.play(
                new TrafficTone(true, ToneFrequency.HIGH, ToneDuration.SHORT), 0.1f));
    }

    @Test void play_silence_doesNotThrow() {
        assertDoesNotThrow(() -> ToneGenerator.play(
                new TrafficTone(false, ToneFrequency.LOW, ToneDuration.SHORT), 0.1f));
    }

    @Test void play_zeroVolume_doesNotThrow() {
        assertDoesNotThrow(() -> ToneGenerator.play(
                new TrafficTone(true, ToneFrequency.HIGH, ToneDuration.LONG), 0f));
    }
}