package main.java.networktool.security;

import main.java.networktool.gui.notification.LocalToast;
import main.java.networktool.logic.analysis.OuiDatabase;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.storage.NetworkStore;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Erweiterter Sicherheits-Monitor mit kontextreichen Warnmeldungen.
 *
 * Erkennt:
 *  1. ARP-Spoofing   – bekannte IP hat neue MAC → mögliche Identitätsübernahme
 *  2. ARP-Poisoning  – mehrere IPs teilen eine MAC → MITM-Angriff wahrscheinlich
 *  3. IP-Spoofing    – bekannte MAC erscheint unter neuer IP → IP-Übernahme
 *  4. Rogue Device   – neues, unbekanntes Gerät im Netz
 *
 * Filterung:
 *  - FF:FF:FF:FF:FF:FF (Broadcast) wird ignoriert
 *  - 01:xx Multicast-MACs werden ignoriert
 *  - 255.x / .255 / .0 Broadcast-IPs werden ignoriert
 */
public final class SecurityMonitor {

    private static final class Holder { static final SecurityMonitor INSTANCE = new SecurityMonitor(); }
    public static SecurityMonitor getInstance() { return Holder.INSTANCE; }

    private static final int    SCAN_INTERVAL_SEC = 30;
    private static final Pattern MAC_PAT =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PAT  =
            Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    private final Map<String, String>      ipToMac   = new ConcurrentHashMap<>();
    private final Map<String, String>      macToIp   = new ConcurrentHashMap<>();
    private final Set<String>              alerted   = ConcurrentHashMap.newKeySet();
    private final Set<String>              whitelist = ConcurrentHashMap.newKeySet();

    private volatile boolean         active    = false;
    private ScheduledExecutorService scheduler;
    private String                   ntfyTopic = "";

    private SecurityMonitor() {}

    // ── Öffentliche API ───────────────────────────────────────────────────

    public synchronized void start(String ntfyTopic) {
        if (active) return;
        this.ntfyTopic = ntfyTopic != null ? ntfyTopic : "";
        this.active    = true;

        NetworkStore.getInstance().getAllHosts().forEach(h -> {
            String mac = extractMac(h.hostname);
            if (mac != null) {
                ipToMac.put(h.ip, normalize(mac));
                macToIp.put(normalize(mac), h.ip);
            }
            whitelist.add(h.ip);
        });

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecurityMonitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::scan, 0, SCAN_INTERVAL_SEC, TimeUnit.SECONDS);
        AuditLogger.getInstance().log("SECURITY_MONITOR_START",
                "Interval=" + SCAN_INTERVAL_SEC + "s");
        System.out.println("[SecurityMonitor] Gestartet (Intervall: " + SCAN_INTERVAL_SEC + "s)");
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        AuditLogger.getInstance().log("SECURITY_MONITOR_STOP", "");
        System.out.println("[SecurityMonitor] Gestoppt.");
    }

    public boolean isActive() { return active; }

    public void addToWhitelist(String ip) {
        if (ip != null && !ip.isBlank()) whitelist.add(ip.trim());
    }

    public void addBaseline(String ip, String mac) {
        if (ip == null || mac == null) return;
        String m = normalize(mac);
        ipToMac.put(ip, m);
        macToIp.put(m, ip);
        whitelist.add(ip);
    }

    // ── Scan-Logik ────────────────────────────────────────────────────────

    private void scan() {
        Map<String, String> current = readArpCache();
        if (current.isEmpty()) return;

        // 1. ARP-Poisoning: gleiche MAC für mehrere IPs
        Map<String, List<String>> macToIpList = new HashMap<>();
        current.forEach((ip, mac) ->
                macToIpList.computeIfAbsent(mac, k -> new ArrayList<>()).add(ip));

        macToIpList.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .filter(e -> !e.getKey().startsWith("FF:FF")) // Broadcast raus
                .filter(e -> !e.getKey().startsWith("01:"))   // Multicast raus
                .forEach(e -> {
                    String key = "ARP_POISON:" + e.getKey();
                    if (alerted.add(key)) {
                        String ips = String.join(", ", e.getValue());
                        warn("ARP-POISONING",
                                "MAC " + e.getKey() + " antwortet auf " + e.getValue().size()
                                + " verschiedene IPs: [" + ips + "]\n"
                                + "  → Möglicher Man-in-the-Middle-Angriff!\n"
                                + "  → Empfehlung: Netzwerk-Traffic mit Wireshark prüfen.");
                    }
                });

        // 2. ARP-Spoofing: bekannte IP hat neue MAC
        current.forEach((ip, mac) -> {
            String known = ipToMac.get(ip);
            if (known != null && !known.equalsIgnoreCase(mac)) {
                String key = "ARP_SPOOF:" + ip + ":" + mac;
                if (alerted.add(key)) {
                    warn("ARP-SPOOFING",
                            "IP " + ip + " hat MAC-Adresse gewechselt!\n"
                            + "  Bekannt:  " + known + "\n"
                            + "  Aktuell:  " + mac + "\n"
                            + "  → Mögliche Identitätsübernahme oder Gerätewechsel.\n"
                            + "  → Baseline aktualisieren wenn Gerät neu ist.");
                }
                ipToMac.put(ip, mac);
            } else if (known == null) {
                ipToMac.put(ip, mac);
            }
        });

        // 3. IP-Spoofing: bekannte MAC mit neuer IP
        current.forEach((ip, mac) -> {
            String knownIp = macToIp.get(mac);
            if (knownIp != null && !knownIp.equals(ip)) {
                String key = "IP_SPOOF:" + mac + ":" + ip;
                if (alerted.add(key)) {
                    warn("IP-SPOOFING",
                            "Gerät mit MAC " + mac + " hat IP gewechselt!\n"
                            + "  Bekannte IP: " + knownIp + "\n"
                            + "  Neue IP:     " + ip + "\n"
                            + "  → Mögliche IP-Übernahme oder DHCP-Konflikt.\n"
                            + "  → Prüfe DHCP-Leases auf dem Router.");
                }
            }
        });

        // 4. Rogue Device: neue IP, nicht in Whitelist
        current.forEach((ip, mac) -> {
            if (!whitelist.contains(ip) && !isNoisyAddress(ip)) {
                String key = "ROGUE:" + ip;
                if (alerted.add(key)) {
                    String vendor = tryVendorLookup(mac);
                    warn("ROGUE_DEVICE",
                            "Unbekanntes Gerät erkannt!\n"
                            + "  IP:         " + ip + "\n"
                            + "  MAC:        " + mac + "\n"
                            + "  Hersteller: " + (vendor != null ? vendor : "unbekannt") + "\n"
                            + "  → Gerät nicht in der Whitelist. Prüfe ob bekannt.");
                }
                whitelist.add(ip);
            }
        });
    }

    private void warn(String type, String msg) {
        String header = "⚠  [" + type + "]";
        String full   = header + "\n  " + msg;
        System.out.println("\n" + full + "\n");
        AuditLogger.getInstance().log("SECURITY_ALERT_" + type,
                msg.replace("\n", " | ").replace("  ", ""));
        LocalToast.show("NetTool – " + type, header);
        if (!ntfyTopic.isBlank())
            MessageSender.send("localhost", full, ntfyTopic);
    }

    // ── ARP-Cache lesen ───────────────────────────────────────────────────

    private static Map<String, String> readArpCache() {
        Map<String, String> result = new LinkedHashMap<>();
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        String cmd = isWin ? "arp -a" : "arp -a -n";
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher ipM  = IP_PAT.matcher(line);
                    Matcher macM = MAC_PAT.matcher(line);
                    if (ipM.find() && macM.find()) {
                        String ip  = ipM.group();
                        String mac = normalize(macM.group());
                        if (!isNoisyAddress(ip) && !mac.startsWith("FF:FF")
                                && !mac.startsWith("01:"))
                            result.put(ip, mac);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────

    private static String normalize(String mac) {
        return mac.toUpperCase().replace("-", ":");
    }

    private static String extractMac(String hostname) {
        if (hostname == null) return null;
        Matcher m = MAC_PAT.matcher(hostname);
        return m.find() ? m.group() : null;
    }

    /** Gibt true für Broadcast/Multicast-Adressen zurück (kein Alert nötig). */
    private static boolean isNoisyAddress(String ip) {
        return ip.startsWith("224.") || ip.startsWith("239.")
                || ip.startsWith("255.") || ip.endsWith(".255")
                || ip.endsWith(".0");
    }

    private static String tryVendorLookup(String mac) {
        try {
            return OuiDatabase.lookup(
                    mac.length() >= 8 ? mac.substring(0, 8) : mac);
        } catch (Exception e) { return null; }
    }
}
