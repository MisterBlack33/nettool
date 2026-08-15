package main.java.networktool.logic.sonify;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vertont Netzwerk-Aktivität periodisch: hoher Ton + tiefer Ton (je Länge aus
 * {@link SonifyConfig#toneMs}), gefolgt von 1s Stille, solange aktiv.
 */
public final class TrafficSonifier {

    private static final Logger LOG = Logger.getLogger(TrafficSonifier.class.getName());
    private static final TrafficSonifier INSTANCE = new TrafficSonifier();

    private static final int SILENCE_MS = 1000;

    private volatile boolean active;
    private volatile float volume = 0.5f;
    private volatile SonifyConfig config = SonifyConfigStore.load();
    private volatile String activeInterface;
    private Thread worker;

    private TrafficSonifier() {}

    public static TrafficSonifier getInstance() { return INSTANCE; }

    public boolean isActive() { return active; }
    public void setVolume(float v) { volume = Math.max(0f, Math.min(1f, v)); }
    public void setConfig(SonifyConfig cfg) { config = cfg.copy(); }
    public SonifyConfig getConfig() { return config.copy(); }
    public String getActiveInterface() { return activeInterface; }

    public synchronized void start(String interfaceName) {
        if (active) return;
        activeInterface = interfaceName;
        active = true;
        worker = new Thread(this::run, "traffic-sonifier");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        active = false;
        if (worker != null) { worker.interrupt(); worker = null; }
    }

    /** Legacy-API: vertont ein Byte-Delta direkt (Bit-für-Bit), für Einzelaufrufe/Tests. */
    public void sonifyDelta(long outgoingDelta, long incomingDelta) {
        try {
            boolean highVolume = volume > 0.5f;
            int outByte = (int) Math.min(Math.max(outgoingDelta, 0), 255);
            int inByte  = (int) Math.min(Math.max(incomingDelta, 0), 255);
            for (boolean bit : BitEncoder.toBits(outByte))
                ToneGenerator.play(TrafficTone.forBit(bit, true, highVolume), volume);
            for (boolean bit : BitEncoder.toBits(inByte))
                ToneGenerator.play(TrafficTone.forBit(bit, false, highVolume), volume);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Traffic-Delta konnte nicht vertont werden", e);
        }
    }

    private void run() {
        while (active) {
            SonifyConfig cfg = config;
            PlainTone.play(cfg.highHz, cfg.toneMs, volume);
            if (!active) break;
            PlainTone.play(cfg.lowHz, cfg.toneMs, volume);
            if (!active || !sleepQuiet(SILENCE_MS)) break;
        }
    }

    private boolean sleepQuiet(int ms) {
        try { Thread.sleep(ms); return active; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
}