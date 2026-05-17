package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.model.HostResult;
import main.java.networktool_v3.model.ScanResult;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Scannt eine Liste von Subnetzen auf aktive Hosts.
 * Traegt Ergebnisse in ScanHistory ein (fuer Netzwerk-Karte).
 */
public final class NetworkHostScanner {

    private NetworkHostScanner() {}

    private static final int THREAD_COUNT = 200;
    private static final int DNS_TIMEOUT  = 1_000;

    private static final Pattern MAC_FULL =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");

    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "00:AA:00:00:00:00");

    public static List<HostResult> scan(List<String> subnets) {
        int total = subnets.size() * 254;
        System.out.println("Starte Scan ueber " + subnets.size()
                + " Subnetz(e) (" + total + " Hosts)...");

        List<HostResult> found   = Collections.synchronizedList(new ArrayList<>());
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

        // ScanHistory befuellen damit Netzwerk-Karte lokale Scans anzeigt
        if (!found.isEmpty()) {
            List<ScanResult> scanResults = found.stream()
                    .map(h -> new ScanResult(h.ip, h.hostname, h.ports, h.os))
                    .toList();
            ScanHistory.getInstance().add("Lokaler Scan", scanResults);
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

    // ── Hostname-Aufloesung ───────────────────────────────────────────────

    private static String resolveHostname(String ip) {
        String dns = dnsLookup(ip);
        if (dns != null && !dns.equals(ip) && !dns.isBlank()) return dns;

        String netbios = netbiosLookup(ip);
        if (netbios != null && !netbios.isBlank()) return netbios;

        String mdns = mdnsLookup(ip);
        if (mdns != null && !mdns.isBlank()) return mdns;

        return "host-" + ip.replace('.', '-');
    }

    private static String dnsLookup(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            String[] result = {null};
            Thread t = new Thread(() -> {
                try { result[0] = addr.getCanonicalHostName(); } catch (Exception ignored) {}
            });
            t.setDaemon(true); t.start(); t.join(DNS_TIMEOUT);
            String name = result[0];
            if (name != null && !name.equals(ip)) return name;
        } catch (Exception ignored) {}
        return null;
    }

    private static String netbiosLookup(String ip) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            String[] cmd = os.contains("win")
                    ? new String[]{"nbtstat", "-A", ip}
                    : new String[]{"nmblookup", "-A", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                String name = parseNetbiosLine(line, os.contains("win"));
                if (name != null) return name;
            }
            p.destroy();
        } catch (Exception ignored) {}
        return null;
    }

    private static String parseNetbiosLine(String line, boolean isWin) {
        if (line == null || line.isBlank()) return null;
        if (isWin) {
            line = line.trim();
            if (line.contains("<00>") && !line.contains("__MSBROWSE__") && !line.startsWith("MAC")) {
                String[] parts = line.split("\\s+");
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

    private static String mdnsLookup(String ip) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"avahi-resolve-address", ip});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.destroy();
            if (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) return parts[1].replaceAll("\\.$", "");
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── MAC-Aufloesung ────────────────────────────────────────────────────

    private static void triggerArpEntry(String ip) {
        try {
            String os  = System.getProperty("os.name", "").toLowerCase();
            String cmd = os.contains("win") ? "ping -n 1 -w 200 " + ip : "ping -c 1 -W 1 " + ip;
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(1_200, TimeUnit.MILLISECONDS); p.destroy();
        } catch (Exception ignored) {}
    }

    static String readMacFromArp(String ip) {
        String mac = queryArp("arp -n " + ip, ip);
        if (mac != null) return mac;
        mac = queryArp("arp -a", ip);
        if (mac != null) return mac;
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            mac = queryArp("arp -a " + ip, ip);
        return mac;
    }

    private static String queryArp(String cmd, String targetIp) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(targetIp)) continue;
                String mac = extractMacFromLine(line);
                if (mac != null) { p.destroy(); return mac; }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractMacFromLine(String line) {
        Matcher m = MAC_FULL.matcher(line);
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