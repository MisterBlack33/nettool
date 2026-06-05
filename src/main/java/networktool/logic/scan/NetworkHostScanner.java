package main.java.networktool.logic.scan;

import main.java.networktool.logic.analysis.OsDetector;
import main.java.networktool.model.HostResult;
import main.java.networktool.model.ScanResult;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class NetworkHostScanner {

    private NetworkHostScanner() {}

    private static final int THREAD_COUNT = Math.max(20, Runtime.getRuntime().availableProcessors() * 4);
    private static final int DNS_TIMEOUT  = 800;

    private static final Pattern MAC_PATTERN = Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "00:AA:00:00:00:00");

    public static List<HostResult> scan(List<String> subnets) {
        int total = subnets.size() * 254;
        System.out.println("Starte Scan über " + subnets.size()
                + " Subnetz(e) (" + total + " Hosts), Threads: " + THREAD_COUNT);

        List<HostResult> found    = Collections.synchronizedList(new ArrayList<>());
        ScanProgress     progress = new ScanProgress(total);
        ExecutorService  executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (String subnet : subnets) {
            for (int i = 1; i < 255; i++) {
                final String host = subnet + "." + i;
                executor.submit(() -> { scanHost(host, found); progress.step(); });
            }
        }

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        if (!found.isEmpty()) {
            List<ScanResult> sr = found.stream()
                    .map(h -> new ScanResult(h.ip, h.hostname, h.ports, h.os))
                    .toList();
            ScanHistory.getInstance().add("Lokaler Scan", sr);
        }
        return found;
    }

    private static void scanHost(String ip, List<HostResult> found) {
        try {
            if (!HostAliveChecker.isAlive(ip)) return;
            triggerArpEntry(ip);
            String mac      = readMacFromArp(ip);
            String hostname = resolveHostname(ip);
            String os       = OsDetector.detect(ip);
            if (os.equals("Unbekannt") || os.isBlank()) {
                String fromHn = OsDetector.detectFromHostname(hostname, ip);
                if (fromHn != null) os = fromHn;
            }
            found.add(new HostResult(ip, buildDisplay(hostname, mac), os));
        } catch (Exception ignored) {}
    }

    private static String resolveHostname(String ip) {
        String dns = dnsLookup(ip);
        if (dns != null && !dns.equals(ip)) return dns;
        String nb = netbiosLookup(ip);
        if (nb != null && !nb.isBlank()) return nb;
        return "host-" + ip.replace('.', '-');
    }

    private static String dnsLookup(String ip) {
        String[] result = {null};
        Thread t = new Thread(() -> {
            try { result[0] = InetAddress.getByName(ip).getCanonicalHostName(); }
            catch (Exception ignored) {}
        });
        t.setDaemon(true); t.start();
        try { t.join(DNS_TIMEOUT); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return (result[0] != null && !result[0].equals(ip)) ? result[0] : null;
    }

    private static String netbiosLookup(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            // FIX: Array-Form statt String → kein Shell-Parsing-Bug
            String[] cmd = win ? new String[]{"nbtstat", "-A", ip}
                    : new String[]{"nmblookup", "-A", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
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

    private static void triggerArpEntry(String ip) {
        try {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            // FIX: Array-Form
            String[] cmd = win ? new String[]{"ping", "-n", "1", "-w", "100", ip}
                    : new String[]{"ping", "-c", "1", "-W", "1",   ip};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(800, TimeUnit.MILLISECONDS);
            p.destroy();
        } catch (Exception ignored) {}
    }

    static String readMacFromArp(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        // FIX: Array-Form für alle exec()-Aufrufe
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
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
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
        return mac != null && !mac.isBlank() ? hostname + " [" + mac + "]" : hostname;
    }
}