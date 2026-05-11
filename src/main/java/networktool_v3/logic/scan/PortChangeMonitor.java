package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.logic.ports.PortScanner;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.storage.NetworkStore;

import java.util.*;
import java.util.concurrent.*;

/**
 * Überwacht die offenen Ports gespeicherter Hosts.
 *
 * Vergleicht bei jedem Scan-Lauf die aktuellen Ports mit den
 * gespeicherten und meldet Änderungen:
 *   + neuer Port offen  → ntfy-Push
 *   - Port geschlossen  → ntfy-Push
 *
 * Singleton, läuft als Daemon-Thread.
 */
public final class PortChangeMonitor {

    private static final class Holder {
        static final PortChangeMonitor INSTANCE = new PortChangeMonitor();
    }
    public static PortChangeMonitor getInstance() { return Holder.INSTANCE; }

    private static final int PORT_TIMEOUT_MS = 500;

    // IP → zuletzt bekannte offene Ports
    private final Map<String, Set<Integer>> lastKnownPorts = new ConcurrentHashMap<>();

    private volatile boolean       active    = false;
    private ScheduledExecutorService scheduler;
    private int    intervalMin = 5;
    private String ntfyTopic   = "";

    private PortChangeMonitor() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Startet den Port-Monitor.
     *
     * @param intervalMin  Prüfintervall in Minuten
     * @param ntfyTopic    ntfy-Topic (leer = nur Konsole)
     */
    public synchronized void start(int intervalMin, String ntfyTopic) {
        if (active) { System.out.println("[PortMonitor] Läuft bereits."); return; }
        this.intervalMin = intervalMin;
        this.ntfyTopic   = ntfyTopic;
        this.active      = true;

        // Baseline aus gespeicherten Hosts laden
        NetworkStore.getInstance().getAllHosts().forEach(h -> {
            if (h.ports != null && !h.ports.isEmpty())
                lastKnownPorts.put(h.ip, new HashSet<>(h.ports.keySet()));
        });

        scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "PortMonitor"); t.setDaemon(true); return t; });
        scheduler.scheduleAtFixedRate(this::checkAll, 0, intervalMin, TimeUnit.MINUTES);
        System.out.println("[PortMonitor] Gestartet ("
                + intervalMin + " min, " + lastKnownPorts.size() + " Host(s))");
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        System.out.println("[PortMonitor] Gestoppt.");
    }

    public boolean isActive()    { return active; }
    public int     getInterval() { return intervalMin; }

    // ── Prüf-Logik ────────────────────────────────────────────────────────

    private void checkAll() {
        List<HostResult> hosts = NetworkStore.getInstance().getAllHosts();
        if (hosts.isEmpty()) return;

        System.out.println("[PortMonitor] Prüfe " + hosts.size() + " Host(s)...");

        ExecutorService exec = Executors.newFixedThreadPool(
                Math.min(hosts.size(), 20));
        hosts.forEach(h -> exec.submit(() -> checkHost(h)));
        exec.shutdown();
        try { exec.awaitTermination(2, TimeUnit.MINUTES); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void checkHost(HostResult host) {
        try {
            // Aktueller Scan
            Map<Integer, String> current =
                    PortScanner.scanSimple(host.ip, PORT_TIMEOUT_MS);
            Set<Integer> currentPorts = current.keySet();
            Set<Integer> previous     = lastKnownPorts.getOrDefault(
                    host.ip, Collections.emptySet());

            // Neue Ports
            Set<Integer> newPorts = new HashSet<>(currentPorts);
            newPorts.removeAll(previous);

            // Geschlossene Ports
            Set<Integer> closedPorts = new HashSet<>(previous);
            closedPorts.removeAll(currentPorts);

            if (!newPorts.isEmpty() || !closedPorts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[PortMonitor] ").append(host.ip)
                  .append(" (").append(host.hostname).append("):");
                if (!newPorts.isEmpty()) sb.append("  +").append(sorted(newPorts));
                if (!closedPorts.isEmpty()) sb.append("  -").append(sorted(closedPorts));
                String msg = sb.toString();
                System.out.println("\n  " + msg);

                if (!ntfyTopic.isBlank())
                    MessageSender.send("localhost", msg, ntfyTopic);

                // Store aktualisieren
                NetworkStore.getInstance().updateNotes(
                        host.ip, NetworkStore.ALL_CATEGORY,
                        host.notes); // Notes unverändert, nur Ports werden neu gesetzt durch Rechtsklick-Save
            }

            lastKnownPorts.put(host.ip, new HashSet<>(currentPorts));

        } catch (Exception e) {
            System.err.println("[PortMonitor] " + host.ip + ": " + e.getMessage());
        }
    }

    private static String sorted(Set<Integer> ports) {
        return new TreeSet<>(ports).toString();
    }
}
