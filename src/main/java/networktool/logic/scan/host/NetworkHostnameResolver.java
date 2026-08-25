package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.logic.TimeoutConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

/**
 * Hostname-Auflösung (DNS + NetBIOS-Fallback) für {@link NetworkHostScanner}.
 * Package-private — reine Unterstützungsklasse der Scan-Orchestrierung.
 */
final class NetworkHostnameResolver {

    private NetworkHostnameResolver() {}

    static String resolveHostname(String ip) {
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
            } catch (Exception e) {
                DebugLogger.getInstance().log("FINE", "[NetworkHostnameResolver] DNS-Lookup fehlgeschlagen (" + ip + "): " + e);
            }
        });
        t.setDaemon(true);
        t.start();
        try { t.join(TimeoutConfig.DNS_LOOKUP_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result[0];
    }

    private static String netbiosLookup(String ip) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            String[] cmd = win ? new String[]{"nbtstat", "-A", ip} : new String[]{"nmblookup", "-A", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String name = parseNetbiosLine(line, win);
                    if (name != null) { p.destroy(); return name; }
                }
            }
            p.destroy();
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostnameResolver] NetBIOS-Lookup fehlgeschlagen (" + ip + "): " + e);
        }
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
}
