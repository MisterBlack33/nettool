package main.java.networktool.logic.sonify;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Spielt einen durchgehenden Sinuston fester Länge (kein Pulsen). */
final class PlainTone {

    private static final int SAMPLE_RATE = 44_100;

    private PlainTone() {}

    static void play(int hz, int durationMs, float volume) {
        if (volume <= 0f || durationMs <= 0) return;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            int samples = SAMPLE_RATE * durationMs / 1000;
            byte[] buf = new byte[samples];
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * hz / SAMPLE_RATE;
                buf[i] = (byte) (Math.sin(angle) * 127 * volume);
            }
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();
        } catch (Exception ignored) {
            // kein Audio-Device verfügbar – best effort
        }
    }
}