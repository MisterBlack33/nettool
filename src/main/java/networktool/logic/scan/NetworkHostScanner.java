package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.OsDetector;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;
import main.java.networktool.util.CIDRUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class NetworkHostScanner {

    private NetworkHostScanner() {}

    private static final int THREAD_COUNT = Math.min(64,
            Math.max(20, Runtime.getRuntime().availableProcessors() * 4));
    private static final int DNS_TIMEOUT  = 600;

    private static final Pattern MAC_PATTERN = Pattern.compile(
            "([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "00:AA:00:00:00:00");

    /** Scannt /24-Präfixe (z.B. "192.168.1") — je 254 IPs. */
    public static List<HostResult> scan(List<String> subnets) {
        // ARP-Cache einmal laden (vermeidet 254x "arp -a")
        HostAliveChecker.warmCache();
        List<String> ips = expandSubnets(subnets);
        return scanIpList(ips);
    }

    /** Scannt einen beliebigen CIDR-Block. */
    public static List<HostResult> scanCidr(String cidr) {
        HostAliveChecker.warmCache();
        List<String> ips = CIDRUtils.getAllIPs(cidr);
        return scanIpList(ips);
    }

    private static List<String> expandSubnets(List<String> subnets) {
        List<String> ips = new ArrayList<>(subnets.size() * 254);
        for (String subnet : subnets)
            for (int i = 1; i < 255; i++)
                ips.add(subnet + "." + i);
        return ips;
    }

    private static List<HostResult> scanIpList(List<String> ips) {
        System.out.println("Starte Scan: " + ips.size() + " Hosts, Threads: " + THREAD_COUNT);
        List<HostResult>  found    = Collections.synchronizedList(new ArrayList<>());
        ScanProgress      progress = new ScanProgress(ips.size());
        ExecutorService   executor = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

        for (String host : ips)
            executor.submit(() -> { scanHost(host, found); progress.step(); });

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        persistToHistory(found);
        System.out.println("Scan abgeschlossen: " + found.size() + " Gerät(e) gefunden.");
        return found;
    }

    private static void persistToHistory(List<HostResult> found) {
        if (found.isEmpty()) return;
        List<ScanResult> sr = found.stream()
                .map(h -> new ScanResult(h.ip, h.hostname, h.ports, h.os))
                .toList();
        ScanHistory.getInstance().add("Lokaler Scan", sr);
    }

    private static void scanHost(String ip, List<HostResult> found) {
        try {
            if (!HostAliveChecker.isAlive(ip)) return;
            String mac      = readMacFromArp(ip);
            String hostname = resolveHostname(ip);
            // OS-Detection leicht: nur Hostname-Heuristik, kein Port-Scan pro Host
            String os = detectOsFast(ip, hostname, mac);
            found.add(new HostResult(ip, buildDisplay(hostname, mac), os));
        } catch (Exception ignored) {}
    }

    /**
     * Schnelle OS-Erkennung: Hostname-Heuristik + OUI-Lookup.
     * Kein vollständiger Port-Scan pro Host (das dauert zu lang).
     */
    private static String detectOsFast(String ip, String hostname, String mac) {
        String fromHn = OsDetector.detectFromHostname(hostname, ip);
        if (fromHn != null) return fromHn;
        if (mac != null && mac.length() >= 8) {
            String vendor = main.java.networktool.logic.analysis.OuiDatabase.lookup(
                    mac.substring(0, 8));
            if (vendor != null) return vendor;
        }
        return "Unbekannt";
    }

    private static String resolveHostname(String ip) {
        String dns = dnsLookup(ip);
        if (dns != null) return dns;
        String nb = netbiosLookup(ip);
        if (nb != null && !nb.isBlank()) return nb;
        return "host-" + ip.replace('.', '-');
    }

    private static String dnsLookup(String ip) {
        String[] result = {null};
        Thread t = new Thread(() -> {
            try {
                String name = InetAddress.getByName(ip).getCanonicalHostName();
                if (!name.equals(ip)) result[0] = name;
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        try { t.join(DNS_TIMEOUT); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }

    private static String netbiosLookup(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            String[] cmd = win
                    ? new String[]{"nbtstat", "-A", ip}
                    : new String[]{"nmblookup", "-A", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String name = parseNetbiosLine(line, win);
                    if (name != null) { p.destroy(); return name; }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return null;
    }

    private static String parseNetbiosLine(String line, boolean isWin) {
        if (line == null || line.isBlank()) return null;
        if (isWin) {
            String t = line.trim();
            if (t.contains("<00>") && !t.contains("__MSBROWSE__") && !t.startsWith("MAC")) {
                String[] parts = t.split("\\s+");
                if (parts.length > 0 && parts[0].length() > 1) return parts[0].trim();
            }
        } else {
            if (line.contains("name=") && line.contains("<0x0>")) {
                int s = line.indexOf("name=") + 5, e = line.indexOf('<', s);
                if (e > s) return line.substring(s, e).trim();
            }
        }
        return null;
    }

    static String readMacFromArp(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[][] cmds = win
                ? new String[][]{{"arp", "-a", ip}, {"arp", "-a"}}
                : new String[][]{{"arp", "-n", ip}, {"arp", "-a", "-n"}, {"arp", "-a"}};
        for (String[] cmd : cmds) {
            String mac = queryArp(cmd, ip);
            if (mac != null) return mac;
        }
        return null;
    }

    private static String queryArp(String[] cmd, String targetIp) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.contains(targetIp)) continue;
                    String mac = extractMac(line);
                    if (mac != null) { p.destroy(); return mac; }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractMac(String line) {
        Matcher m = MAC_PATTERN.matcher(line);
        if (!m.find()) return null;
        String mac = m.group().toUpperCase().replace("-", ":");
        if (INVALID_MACS.contains(mac)) return null;
        if (mac.startsWith("FF:FF") || mac.startsWith("01:")) return null;
        return mac;
    }

    private static String buildDisplay(String hostname, String mac) {
        return mac != null && !mac.isBlank()
                ? hostname + " [" + mac + "]"
                : hostname;
    }
}