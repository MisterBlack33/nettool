package main.java.networktool.logic.sonify;

import javax.sound.sampled.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spielt {@link TrafficTone}s über eine dauerhaft offene Line ab.
 * Vermeidet Open/Close-Overhead pro Ton (Hauptursache für Nachlauf-Stau).
 */
public final class ToneGenerator {

    private static final Logger LOG = Logger.getLogger(ToneGenerator.class.getName());
    private static final int SAMPLE_RATE = 44_100;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private static volatile SourceDataLine sharedLine;

    private ToneGenerator() {}

    /** Öffnet die gemeinsame Line einmalig. Muss vor der ersten play()-Serie aufgerufen werden. */
    public static synchronized void open() {
        if (sharedLine != null && sharedLine.isOpen()) return;
        try {
            sharedLine = AudioSystem.getSourceDataLine(FORMAT);
            sharedLine.open(FORMAT, SAMPLE_RATE / 4); // kleiner Puffer = weniger Latenz
            sharedLine.start();
        } catch (LineUnavailableException e) {
            LOG.log(Level.WARNING, "Kein Audio-Ausgabegerät verfügbar", e);
            sharedLine = null;
        }
    }

    public static synchronized void close() {
        if (sharedLine == null) return;
        sharedLine.drain();
        sharedLine.close();
        sharedLine = null;
    }

    /** Schreibt den Ton NICHT-blockierend in die offene Line (kein open/close pro Aufruf). */
    public static void play(TrafficTone tone, float volume) {
        if (tone == null || !tone.audible() || volume <= 0f) return;
        SourceDataLine line = sharedLine;
        if (line == null || !line.isOpen()) return;

        float vol = Math.max(0f, Math.min(1f, volume));
        int samples = SAMPLE_RATE * tone.duration().ms / 1000;
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * tone.frequency().hz / SAMPLE_RATE;
            short s = (short) (Math.sin(angle) * Short.MAX_VALUE * vol);
            buf[i * 2]     = (byte) (s & 0xFF);
            buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        line.write(buf, 0, buf.length); // blockiert nur wenn Puffer voll, kein Line-Overhead
    }
}