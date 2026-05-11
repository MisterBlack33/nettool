package main.java.networktool_v3.logic.analysis;

import main.java.networktool_v3.logic.messaging.MessageSender;
import main.java.networktool_v3.storage.NetworkStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * ARP-Spoofing-Detektor.
 *
 * Liest den ARP-Cache in regelmäßigen Abständen und meldet wenn:
 *  - Eine bekannte IP mit neuer MAC auftaucht (mögliches ARP-Spoofing)
 *  - Mehrere IPs dieselbe MAC haben (ARP-Poisoning / MITM)
 *
 * Funktioniert ohne Root-Rechte (liest nur den ARP-Cache via arp -a).
 *
 * Singleton, startet als Daemon-Thread.
 */
public final class ArpMonitor {

    private static final class Holder {
        static final ArpMonitor INSTANCE = new ArpMonitor();
    }
    public static ArpMonitor getInstance() { return Holder.INSTANCE; }

    private static final int SCAN_INTERVAL_SEC = 30;
    private static final Pattern MAC_PATTERN   =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PATTERN    =
            Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    // Bekannte IP → MAC Zuordnungen
    private final Map<String, String>       knownIpMac  = new ConcurrentHashMap<>();
    // MAC → Liste der IPs die sie haben (für Duplikat-Erkennung)
    private final Map<String, Set<String>>  macToIps    = new ConcurrentHashMap<>();
    // Gemeldete Warnungen (um Doppel-Alerts zu vermeiden)
    private final Set<String>              alertedKeys  = ConcurrentHashMap.newKeySet();

    private volatile boolean active = false;
    private ScheduledExecutorService scheduler;
    private String ntfyTopic = "";

    private ArpMonitor() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Startet den ARP-Monitor.
     *
     * @param ntfyTopic  ntfy-Topic für Push-Alarme (leer = nur Konsole)
     */
    public synchronized void start(String ntfyTopic) {
        if (active) { System.out.println("[ARP-Monitor] Läuft bereits."); return; }
        this.ntfyTopic = ntfyTopic;
        this.active    = true;

        // Initialzustand aus gespeicherten Hosts laden
        NetworkStore.getInstance().getAllHosts().forEach(h -> {
            String mac = extractMac(h.hostname);
            if (mac != null) {
                knownIpMac.put(h.ip, mac.toUpperCase());
                macToIps.computeIfAbsent(mac.toUpperCase(), k -> new HashSet<>()).add(h.ip);
            }
        });

        scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "ARP-Monitor"); t.setDaemon(true); return t; });
        scheduler.scheduleAtFixedRate(this::scan, 0,
                SCAN_INTERVAL_SEC, TimeUnit.SECONDS);

        System.out.println("[ARP-Monitor] Gestartet (Intervall: "
                + SCAN_INTERVAL_SEC + "s)");
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        System.out.println("[ARP-Monitor] Gestoppt.");
    }

    public boolean isActive() { return active; }

    /** Fügt eine bekannte IP→MAC Zuordnung manuell hinzu (Baseline). */
    public void addBaseline(String ip, String mac) {
        String macUpper = mac.toUpperCase();
        knownIpMac.put(ip, macUpper);
        macToIps.computeIfAbsent(macUpper, k -> new HashSet<>()).add(ip);
    }

    // ── Scan-Logik ────────────────────────────────────────────────────────

    private void scan() {
        Map<String, String> current = readArpCache();
        if (current.isEmpty()) return;

        // Duplikate: gleiche MAC für mehrere IPs
        Map<String, Set<String>> macMap = new HashMap<>();
        current.forEach((ip, mac) ->
                macMap.computeIfAbsent(mac, k -> new HashSet<>()).add(ip));

        macMap.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> {
                    String key = "DUP:" + e.getKey();
                    if (alertedKeys.add(key)) {
                        String msg = "⚠ ARP-WARNUNG: MAC " + e.getKey()
                                + " hat " + e.getValue().size() + " IPs: " + e.getValue()
                                + " – mögliches ARP-Poisoning / MITM!";
                        alert(msg);
                    }
                });

        // MAC-Wechsel für bekannte IPs
        current.forEach((ip, mac) -> {
            String known = knownIpMac.get(ip);
            if (known != null && !known.equalsIgnoreCase(mac)) {
                String key = "CHANGE:" + ip + ":" + mac;
                if (alertedKeys.add(key)) {
                    String msg = "⚠ ARP-SPOOFING: IP " + ip
                            + " hat MAC gewechselt!  "
                            + known + " → " + mac;
                    alert(msg);
                }
                // Neuen Wert merken
                knownIpMac.put(ip, mac.toUpperCase());
            } else if (known == null) {
                knownIpMac.put(ip, mac.toUpperCase());
            }
        });
    }

    private void alert(String msg) {
        System.out.println("\n  " + msg);
        if (!ntfyTopic.isBlank()) {
            MessageSender.send("localhost", msg, ntfyTopic);
        }
    }

    // ── ARP-Cache lesen ───────────────────────────────────────────────────

    /** Liest den ARP-Cache des Systems via 'arp -a'. */
    private Map<String, String> readArpCache() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String os  = System.getProperty("os.name", "").toLowerCase();
            String cmd = os.contains("win") ? "arp -a" : "arp -a -n";
            Process p  = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher ipM  = IP_PATTERN.matcher(line);
                Matcher macM = MAC_PATTERN.matcher(line);
                if (ipM.find() && macM.find()) {
                    String ip  = ipM.group();
                    String mac = macM.group().toUpperCase()
                            .replace("-", ":");
                    // Broadcast/Multicast filtern
                    if (!mac.startsWith("FF:FF") && !mac.startsWith("01:"))
                        result.put(ip, mac);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String extractMac(String hostname) {
        if (hostname == null) return null;
        Matcher m = MAC_PATTERN.matcher(hostname);
        return m.find() ? m.group() : null;
    }
}
