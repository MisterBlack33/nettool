package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanProfile;
import main.java.networktool_v3.model.ScanResult;
import main.java.networktool_v3.storage.NetworkStore;
import main.java.networktool_v3.storage.ScanProfileStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Führt Scans zeitgesteuert aus und vergleicht mit dem Vorgänger-Ergebnis.
 *
 * Pro Profil läuft ein eigener ScheduledFuture in einem gemeinsamen
 * ScheduledExecutorService. Bei Änderungen wird ein ntfy-Push gesendet
 * (falls Topic im NetworkStore vorhanden).
 *
 * Singleton, thread-sicher.
 */
public final class ScanScheduler {

    private static final class Holder {
        static final ScanScheduler INSTANCE = new ScanScheduler();
    }
    public static ScanScheduler getInstance() { return Holder.INSTANCE; }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(4,
                    r -> { Thread t = new Thread(r, "Scheduler"); t.setDaemon(true); return t; });

    // Profile-Name → laufender Task
    private final Map<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();
    // Profile-Name → letztes Ergebnis (für Delta)
    private final Map<String, List<ScanResult>>   lastScan = new ConcurrentHashMap<>();

    private ScanScheduler() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Startet einen wiederkehrenden Scan für das angegebene Profil.
     *
     * @param profileName  Name des ScanProfile
     * @param intervalMin  Intervall in Minuten
     * @param ntfyTopic    ntfy-Topic für Push-Benachrichtigungen (leer = kein Push)
     */
    public void start(String profileName, int intervalMin, String ntfyTopic) {
        stop(profileName); // Eventuell laufenden Task stoppen

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> runScheduledScan(profileName, ntfyTopic),
                0, intervalMin, TimeUnit.MINUTES);

        running.put(profileName, future);
        System.out.println("[Scheduler] '" + profileName + "' gestartet  (alle "
                + intervalMin + " min)");
    }

    /** Stoppt einen laufenden Scheduler für ein Profil. */
    public void stop(String profileName) {
        ScheduledFuture<?> f = running.remove(profileName);
        if (f != null) {
            f.cancel(false);
            System.out.println("[Scheduler] '" + profileName + "' gestoppt.");
        }
    }

    public void stopAll() {
        new ArrayList<>(running.keySet()).forEach(this::stop);
    }

    /** Gibt alle laufenden Profil-Namen zurück. */
    public Set<String> getRunning() {
        return Collections.unmodifiableSet(running.keySet());
    }

    public boolean isRunning(String profileName) {
        return running.containsKey(profileName);
    }

    // ── Interner Scan-Loop ────────────────────────────────────────────────

    private void runScheduledScan(String profileName, String ntfyTopic) {
        Optional<ScanProfile> opt = ScanProfileStore.getInstance().get(profileName);
        if (opt.isEmpty()) { stop(profileName); return; }
        ScanProfile profile = opt.get();

        String ts = LocalDateTime.now().format(FMT);
        System.out.println("\n[Scheduler " + ts + "] Scan: " + profile.summary());

        try {
            List<ScanResult> current = runProfileScan(profile);
            List<ScanResult> previous = lastScan.get(profileName);

            if (previous != null && !previous.isEmpty()) {
                List<ScanDelta.DeltaEntry> delta = ScanDelta.compare(
                        previous, current, "vorheriger Lauf", ts);

                // Änderungen per ntfy melden
                if (!delta.isEmpty() && !ntfyTopic.isBlank()) {
                    long neu = delta.stream()
                            .filter(e -> e.type == ScanDelta.ChangeType.NEU).count();
                    long weg = delta.stream()
                            .filter(e -> e.type == ScanDelta.ChangeType.WEG).count();
                    String msg = "[NetTool] " + profileName + " – "
                            + delta.size() + " Änderung(en) "
                            + "(+" + neu + " neu, -" + weg + " weg)";
                    MessageSender.send("localhost", msg, ntfyTopic);
                }
            } else {
                System.out.println("  [Scheduler] Erster Lauf – kein Delta.");
            }

            lastScan.put(profileName, current);
            ScanProfileStore.getInstance().updateLastRun(
                    profileName, LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // AutoSave: neue Hosts in Kategorie speichern
            if (profile.autoSave && !profile.category.isBlank() && previous != null) {
                Set<String> knownIps = new HashSet<>();
                previous.forEach(r -> knownIps.add(r.getIp()));
                current.stream()
                        .filter(r -> !knownIps.contains(r.getIp()))
                        .forEach(r -> NetworkStore.getInstance().save(
                                new HostResult(
                                        r.getIp(), r.getHostname(),
                                        r.getOsGuess(), null,
                                        r.getOpenPorts(), ""),
                                profile.category));
            }

        } catch (Exception e) {
            System.err.println("[Scheduler] Fehler bei '" + profileName + "': "
                    + e.getMessage());
        }
    }

    private List<ScanResult> runProfileScan(ScanProfile profile) throws Exception {
        if (profile.cidrs.isEmpty()) {
            // Lokales Netz scannen
            List<String> subnets = SubnetDetector.getAllSubnets();
            List<HostResult> hosts =
                    NetworkHostScanner.scan(subnets);
            // HostResult → ScanResult konvertieren
            List<ScanResult> results = new ArrayList<>();
            hosts.forEach(h -> results.add(new ScanResult(
                    h.ip, h.hostname, h.ports, h.os)));
            return results;
        } else {
            List<ScanResult> all = new ArrayList<>();
            for (String cidr : profile.cidrs) {
                if (Thread.currentThread().isInterrupted()) break;
                all.addAll(NetworkScanner.scanCIDR(cidr));
            }
            return all;
        }
    }
}
