package main.java.networktool_v3.logic.scan;

import main.java.networktool_v3.logic.analysis.OsDetector;
import main.java.networktool_v3.model.HostResult;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Scannt eine Liste von Subnetzen auf aktive Hosts.
 *
 * Verbesserungen gegenüber der Vorgängerversion:
 *  - Hostname-Auflösung: mehrere Methoden (DNS, NetBIOS, mDNS)
 *  - MAC-Auflösung:      ARP-Ping erzwingt ARP-Cache-Eintrag vor dem Lesen
 *  - Fallback-Hostname:  nie die reine IP, immer ein sinnvoller Name
 *  - Plausibilitätsfilter: MAC "00:00:00:00:00:00" und "ff:ff:ff:ff:ff:ff"
 *    werden als ungültig abgelehnt.
 */
public final class NetworkHostScanner {

    private NetworkHostScanner() {}

    private static final int THREAD_COUNT  = 200;
    private static final int DNS_TIMEOUT   = 1_000;   // ms

    private static final Pattern MAC_FULL =
            Pattern.compile("([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_PAT   =
            Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b");

    // Bekannte ungültige MACs (Broadcast, All-Zeros)
    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF",
            "00:AA:00:00:00:00"  // Windows-ARP-Platzhalter
    );

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Scannt alle angegebenen Subnetze und gibt die gefundenen Hosts zurück.
     *
     * @param subnets Liste von Subnetz-Präfixen (z.B. ["192.168.1", "10.0.0"])
     * @return Liste der erreichbaren Hosts mit OS-Info, Hostname und MAC
     */
    public static List<HostResult> scan(List<String> subnets) {
        int total = subnets.size() * 254;
        System.out.println("Starte Scan über " + subnets.size()
                + " Subnetz(e) (" + total + " Hosts)...");

        List<HostResult> found   = Collections.synchronizedList(new ArrayList<>());
        ScanProgress progress    = new ScanProgress(total);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (String subnet : subnets) {
            for (int i = 1; i < 255; i++) {
                final String host = subnet + "." + i;
                executor.submit(() -> {
                    scanHost(host, found);
                    progress.step();
                });
            }
        }

        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        return found;
    }

    // ── Einzelner Host ────────────────────────────────────────────────────

    private static void scanHost(String ip, List<HostResult> found) {
        try {
            if (!HostAliveChecker.isAlive(ip)) return;

            // 1. ARP-Ping erzwingen (damit der ARP-Cache befüllt wird)
            triggerArpEntry(ip);

            // 2. MAC aus ARP-Cache lesen
            String mac = readMacFromArp(ip);

            // 3. Hostname auflösen (mehrere Methoden, nie die reine IP)
            String hostname = resolveHostname(ip);

            // 4. OS erkennen
            String os = OsDetector.detect(ip);
            if (os.equals("Unbekannt") || os.isBlank()) {
                String fromHostname = OsDetector.detectFromHostname(hostname, ip);
                if (fromHostname != null) os = fromHostname;
            }

            // 5. Anzeige-Hostname zusammenbauen: "hostname [MAC]" oder nur "hostname"
            String display = buildDisplay(hostname, mac);
            found.add(new HostResult(ip, display, os));

        } catch (Exception ignored) {}
    }

    // ── Hostname-Auflösung ────────────────────────────────────────────────

    /**
     * Versucht den Hostnamen über mehrere Kanäle zu ermitteln.
     * Gibt niemals die rohe IP zurück – immer einen sinnvollen Namen oder
     * einen generierten Fallback-Namen (z.B. "host-192-168-1-42").
     */
    private static String resolveHostname(String ip) {
        // Methode 1: Standard-Java-DNS mit Timeout
        String dns = dnsLookup(ip);
        if (dns != null && !dns.equals(ip) && !dns.isBlank()) return dns;

        // Methode 2: NetBIOS-Name (Windows-Hosts)
        String netbios = netbiosLookup(ip);
        if (netbios != null && !netbios.isBlank()) return netbios;

        // Methode 3: mDNS / .local-Name
        String mdns = mdnsLookup(ip);
        if (mdns != null && !mdns.isBlank()) return mdns;

        // Fallback: generierter Name statt roher IP
        return "host-" + ip.replace('.', '-');
    }

    private static String dnsLookup(String ip) {
        try {
            // InetAddress.getByName mit IP-String erzeugt ein Objekt ohne DNS-Lookup,
            // getCanonicalHostName() führt dann den Reverse-Lookup durch.
            InetAddress addr = InetAddress.getByName(ip);
            // Timeout via separatem Thread
            String[] result = {null};
            Thread t = new Thread(() -> {
                try { result[0] = addr.getCanonicalHostName(); }
                catch (Exception ignored) {}
            });
            t.setDaemon(true);
            t.start();
            t.join(DNS_TIMEOUT);
            String name = result[0];
            if (name != null && !name.equals(ip)) return name;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * NetBIOS-Name via 'nbtstat -A <ip>' (Windows) oder 'nmblookup -A <ip>' (Linux).
     */
    private static String netbiosLookup(String ip) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            String[] cmd = os.contains("win")
                    ? new String[]{"nbtstat", "-A", ip}
                    : new String[]{"nmblookup", "-A", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                // Windows nbtstat: Name ist in den ersten 15 Zeichen der Tabelle
                // Linux nmblookup: "IP name=HOSTNAME<00>"
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
            // Format: "  COMPUTERNAME      <00>  UNIQUE   Registered"
            line = line.trim();
            if (line.contains("<00>") && !line.contains("__MSBROWSE__")
                    && !line.startsWith("MAC")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 0 && parts[0].length() > 1)
                    return parts[0].trim();
            }
        } else {
            // Format: "name=HOSTNAME<0x0>"
            if (line.contains("name=") && line.contains("<0x0>")) {
                int s = line.indexOf("name=") + 5;
                int e = line.indexOf('<', s);
                if (e > s) return line.substring(s, e).trim();
            }
        }
        return null;
    }

    /**
     * mDNS-Lookup via 'avahi-resolve' (Linux) oder DNS-SD auf macOS/Win.
     * Sucht nach <reversed-ip>.in-addr.arpa → .local-Namen.
     */
    private static String mdnsLookup(String ip) {
        try {
            // avahi-resolve-address ist das verlässlichste CLI-Tool
            Process p = Runtime.getRuntime().exec(
                    new String[]{"avahi-resolve-address", ip});
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.destroy();
            if (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) return parts[1].replaceAll("\\.$", "");
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── MAC-Auflösung ─────────────────────────────────────────────────────

    /**
     * Erzwingt einen ARP-Cache-Eintrag durch gezielten Ping.
     * Nach dem Ping ist die MAC in 'arp -n <ip>' verfügbar.
     */
    private static void triggerArpEntry(String ip) {
        try {
            String os  = System.getProperty("os.name", "").toLowerCase();
            String cmd = os.contains("win") ? "ping -n 1 -w 200 " + ip
                    : "ping -c 1 -W 1 " + ip;
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(1_200, TimeUnit.MILLISECONDS);
            p.destroy();
        } catch (Exception ignored) {}
    }

    /**
     * Liest die MAC-Adresse aus dem ARP-Cache.
     * Versucht mehrere Befehle und filtert ungültige MACs heraus.
     */
    static String readMacFromArp(String ip) {
        // Primär: 'arp -n <ip>' (gezielt)
        String mac = queryArp("arp -n " + ip, ip);
        if (mac != null) return mac;
        // Fallback: 'arp -a' (gesamter Cache, nach IP suchen)
        mac = queryArp("arp -a", ip);
        if (mac != null) return mac;
        // Windows: 'arp -a <ip>'
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            mac = queryArp("arp -a " + ip, ip);
        }
        return mac;
    }

    private static String queryArp(String cmd, String targetIp) {
        try {
            Process p  = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                // Nur Zeilen mit der Ziel-IP auswerten
                if (!line.contains(targetIp) && !cmd.equals("arp -n " + targetIp)) {
                    if (!line.contains(targetIp)) continue;
                }
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
        // Ungültige MACs filtern
        if (INVALID_MACS.contains(mac)) return null;
        // Broadcast/Multicast filtern
        if (mac.startsWith("FF:FF") || mac.startsWith("01:")) return null;
        return mac;
    }

    // ── Anzeige-Hostname ──────────────────────────────────────────────────

    private static String buildDisplay(String hostname, String mac) {
        if (mac != null && !mac.isBlank()) {
            return hostname + " [" + mac + "]";
        }
        return hostname;
    }
}