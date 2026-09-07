package main.java.networktool.logic.visualize;

import main.java.networktool.logic.sonify.InterfaceStatsReader;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Erfasst periodisch Netzwerk-Traffic-Deltas eines Interfaces für die visuelle
 * Darstellung (Diagramm). Reine Datenerfassung — kein Rendering, kein Audio.
 *
 * Ringpuffer-Kapazität ist konfigurierbar (Default 120 Samples ≈ 60s bei 500ms
 * Abtastrate), damit Renderer unterschiedliche Zeitfenster wählen können.
 */
public final class TrafficVisualizer {

    private static final TrafficVisualizer INSTANCE = new TrafficVisualizer();
    public static TrafficVisualizer getInstance() { return INSTANCE; }

    private static final int  DEFAULT_CAPACITY   = 120;
    private static final long SAMPLE_INTERVAL_MS = 500;

    private final Deque<TrafficSample> buffer = new ConcurrentLinkedDeque<>();
    private volatile int capacity = DEFAULT_CAPACITY;

    private volatile boolean active;
    private volatile String  activeInterface;
    private Thread worker;

    private TrafficVisualizer() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public boolean isActive()           { return active; }
    public String  getActiveInterface() { return activeInterface; }

    public void setCapacity(int newCapacity) {
        if (newCapacity <= 0) return;
        capacity = newCapacity;
        trimToCapacity();
    }

    public synchronized void start(String interfaceName) {
        if (active) return;
        activeInterface = interfaceName;
        active = true;
        worker = new Thread(() -> run(interfaceName), "traffic-visualizer");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        active = false;
        if (worker != null) { worker.interrupt(); worker = null; }
    }

    public void clear() { buffer.clear(); }

    /** Unveränderliche Momentaufnahme des Puffers, älteste Samples zuerst. */
    public List<TrafficSample> getSnapshot() {
        return List.copyOf(buffer);
    }

    // ── Erfassung ─────────────────────────────────────────────────────────

    private void run(String iface) {
        long[] last = InterfaceStatsReader.read(iface);
        while (active) {
            if (!sleepQuiet(SAMPLE_INTERVAL_MS)) break;
            long[] current = InterfaceStatsReader.read(iface);
            if (current == null) continue;
            if (last != null) addSample(current, last);
            last = current;
        }
    }

    private void addSample(long[] current, long[] last) {
        long rxDelta = Math.max(0, current[0] - last[0]);
        long txDelta = Math.max(0, current[1] - last[1]);
        offerSample(new TrafficSample(System.currentTimeMillis(), rxDelta, txDelta));
    }

    /** Package-private Test-Hook: fügt ein Sample direkt hinzu, ohne Interface-I/O. */
    void offerSample(TrafficSample sample) {
        buffer.addLast(sample);
        trimToCapacity();
    }

    private void trimToCapacity() {
        while (buffer.size() > capacity) buffer.pollFirst();
    }

    private boolean sleepQuiet(long ms) {
        try { Thread.sleep(ms); return active; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
}