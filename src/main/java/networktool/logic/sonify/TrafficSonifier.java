package main.java.networktool.logic.sonify;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wandelt den Datendurchsatz eines Netzwerk-Interfaces in hörbare Töne um.
 * Pro Poll-Intervall wird das Byte-Delta (ausgehend/eingehend) auf je 8 Bit
 * reduziert ({@link BitEncoder}) und jedes Bit als {@link TrafficTone}
 * ausgegeben ({@link ToneGenerator}).
 */
public final class TrafficSonifier {

    private static final Logger LOG = Logger.getLogger(TrafficSonifier.class.getName());
    private static final TrafficSonifier INSTANCE = new TrafficSonifier();
    private static final int POLL_MS = 250;

    private volatile boolean active;
    private volatile float volume = 0.5f;
    private Thread worker;

    private TrafficSonifier() {}

    public static TrafficSonifier getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    /** Lautstärke 0.0–1.0, Werte außerhalb werden geklemmt. */
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
    }

    public synchronized void start(String interfaceName) {
        if (active) return;
        active = true;
        worker = new Thread(() -> run(interfaceName), "traffic-sonifier");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        active = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    /**
     * Vertont ein Byte-Delta direkt (öffentlich, damit einzeln testbar).
     * Best-effort – wirft nie, auch nicht ohne Audio-Device.
     */
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

    private void run(String interfaceName) {
        long[] last = InterfaceStatsReader.read(interfaceName);
        try {
            while (active) {
                Thread.sleep(POLL_MS);
                long[] cur = InterfaceStatsReader.read(interfaceName);
                if (last != null && cur != null) {
                    sonifyDelta(cur[1] - last[1], cur[0] - last[0]);
                }
                last = cur;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
