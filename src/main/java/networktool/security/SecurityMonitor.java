package main.java.networktool.security;

import main.java.networktool.logic.analysis.OuiDatabase;
import main.java.networktool.logic.messaging.MessageSender;
import main.java.networktool.storage.NetworkStore;
import main.java.networktool.gui.notification.LocalToast;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Erkennt ARP-Spoofing und ARP-Poisoning.
 * Rogue-Device-Erkennung deaktiviert (zu viel Spam in fremden Netzen).
 */
public final class SecurityMonitor {

    private static final class Holder { static final SecurityMonitor INSTANCE = new SecurityMonitor(); }
    public static SecurityMonitor getInstance() { return Holder.INSTANCE; }

    private static final int     SCAN_INTERVAL_SEC = 30;
    private static final Pattern MAC_PAT = Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PAT  = Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    private final Map<String, String> ipToMac = new ConcurrentHashMap<>();
    private final Map<String, String> macToIp = new ConcurrentHashMap<>();
    private final Set<String>         alerted = ConcurrentHashMap.newKeySet();

    private volatile boolean         active    = false;
    private ScheduledExecutorService scheduler;
    private String                   ntfyTopic = "";

    private SecurityMonitor() {}

    public synchronized void start(String ntfyTopic) {
        if (active) return;
        this.ntfyTopic = ntfyTopic != null ? ntfyTopic : "";
        this.active    = true;

        loadSavedHostsBaseline();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecurityMonitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::scan, 0, SCAN_INTERVAL_SEC, TimeUnit.SECONDS);
        AuditLogger.getInstance().log("SECURITY_MONITOR_START", "Interval=" + SCAN_INTERVAL_SEC + "s");
        System.out.println("[SecurityMonitor] Gestartet (" + SCAN_INTERVAL_SEC + "s)");
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        if (scheduler != null) scheduler.shutdownNow();
        AuditLogger.getInstance().log("SECURITY_MONITOR_STOP", "");
        System.out.println("[SecurityMonitor] Gestoppt.");
    }

    public boolean isActive() { return active; }

    public void addToWhitelist(String ip) { /* kept for API compat */ }

    public void addBaseline(String ip, String mac) {
        if (ip == null || mac == null) return;
        String normalized = normalize(mac);
        ipToMac.put(ip, normalized);
        macToIp.put(normalized, ip);
    }

    // ── Baseline ─────────────────────────────────────────────────────────

    private void loadSavedHostsBaseline() {
        NetworkStore.getInstance().getAllHosts().forEach(h -> {
            String mac = extractMac(h.hostname);
            if (mac != null) addBaseline(h.ip, mac);
        });
    }

    // ── Scan ──────────────────────────────────────────────────────────────

    private void scan() {
        Map<String, String> current = readArpCache();
        if (current.isEmpty()) return;
        checkArpPoisoning(current);
        checkArpSpoofing(current);
        checkIpSpoofing(current);
    }

    private void checkArpPoisoning(Map<String, String> current) {
        Map<String, List<String>> macToIpList = new HashMap<>();
        current.forEach((ip, mac) -> macToIpList.computeIfAbsent(mac, k -> new ArrayList<>()).add(ip));
        macToIpList.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .filter(e -> !e.getKey().startsWith("FF:FF") && !e.getKey().startsWith("01:"))
                .forEach(e -> {
                    String key = "ARP_POISON:" + e.getKey();
                    if (alerted.add(key))
                        warn("ARP-POISONING", "MAC " + e.getKey() + " auf " + e.getValue().size() + " IPs: " + e.getValue());
                });
    }

    private void checkArpSpoofing(Map<String, String> current) {
        current.forEach((ip, mac) -> {
            String known = ipToMac.get(ip);
            if (known != null && !known.equalsIgnoreCase(mac)) {
                String key = "ARP_SPOOF:" + ip + ":" + mac;
                if (alerted.add(key))
                    warn("ARP-SPOOFING", "IP " + ip + ": " + known + " → " + mac);
                ipToMac.put(ip, mac);
            } else if (known == null) {
                ipToMac.put(ip, mac);
            }
        });
    }

    private void checkIpSpoofing(Map<String, String> current) {
        current.forEach((ip, mac) -> {
            String knownIp = macToIp.get(mac);
            if (knownIp != null && !knownIp.equals(ip)) {
                String key = "IP_SPOOF:" + mac + ":" + ip;
                if (alerted.add(key))
                    warn("IP-SPOOFING", "MAC " + mac + ": " + knownIp + " → " + ip);
            }
        });
    }

    private void warn(String type, String msg) {
        System.out.println("\n⚠  [" + type + "] " + msg);
        AuditLogger.getInstance().log("SECURITY_ALERT_" + type, msg);
        LocalToast.show("NetTool – " + type, msg);
        if (!ntfyTopic.isBlank()) MessageSender.send("localhost", msg, ntfyTopic);
    }

    // ── ARP-Cache ─────────────────────────────────────────────────────────

    private static Map<String, String> readArpCache() {
        Map<String, String> result = new LinkedHashMap<>();
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            Process p = Runtime.getRuntime().exec(isWin ? "arp -a" : "arp -a -n");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher ipM  = IP_PAT.matcher(line);
                    Matcher macM = MAC_PAT.matcher(line);
                    if (ipM.find() && macM.find()) {
                        String ip  = ipM.group();
                        String mac = normalize(macM.group());
                        if (!isNoisyAddress(ip) && !mac.startsWith("FF:FF") && !mac.startsWith("01:"))
                            result.put(ip, mac);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String normalize(String mac)       { return mac.toUpperCase().replace("-", ":"); }
    private static String extractMac(String hostname)  {
        if (hostname == null) return null;
        Matcher m = MAC_PAT.matcher(hostname);
        return m.find() ? m.group() : null;
    }
    private static boolean isNoisyAddress(String ip) {
        return ip.startsWith("224.") || ip.startsWith("239.") ||
                ip.startsWith("255.") || ip.endsWith(".255") || ip.endsWith(".0");
    }
}