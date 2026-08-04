package main.java.networktool.logic.sonify;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spielt einen {@link TrafficTone} über die System-Audioausgabe ab.
 * Best-effort: wenn kein Audio-Device verfügbar ist (z.B. CI/headless),
 * wird der Fehler geloggt statt geworfen.
 */
public final class ToneGenerator {

    private static final Logger LOG = Logger.getLogger(ToneGenerator.class.getName());
    private static final int SAMPLE_RATE = 44_100;

    private ToneGenerator() {}

    public static void play(TrafficTone tone, float volume) {
        if (tone == null || !tone.audible() || volume <= 0f) return;
        float vol = Math.max(0f, Math.min(1f, volume));
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            int samples = SAMPLE_RATE * tone.duration().ms / 1000;
            byte[] buf = new byte[samples];
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * tone.frequency().hz / SAMPLE_RATE;
                buf[i] = (byte) (Math.sin(angle) * 127 * vol);
            }
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();
        } catch (Exception e) {
            LOG.log(Level.FINE, "Ton konnte nicht abgespielt werden (kein Audio-Device verfügbar?)", e);
        }
    }
}
