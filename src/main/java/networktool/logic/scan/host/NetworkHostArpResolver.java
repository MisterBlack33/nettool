package main.java.networktool.logic.scan.host;

import main.java.networktool.logging.DebugLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ARP-Cache-Zugriff und -Parsing für {@link NetworkHostScanner}.
 * Package-private — reine Unterstützungsklasse der Scan-Orchestrierung.
 */
final class NetworkHostArpResolver {

    private NetworkHostArpResolver() {}

    private static final Pattern MAC_PATTERN = Pattern.compile(
            "([0-9A-Fa-f]{2}[:\\-]){5}[0-9A-Fa-f]{2}");
    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF");

    /**
     * Liest den ARP-Cache und filtert auf die gewünschten Subnetze.
     * Gibt IP→MAC zurück.
     */
    static Map<String, String> readArpCache(List<String> subnets) {
        Map<String, String> result = new LinkedHashMap<>();
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        try {
            Process p = Runtime.getRuntime().exec(isWin ? "arp -a" : "arp -a -n");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] entry = parseArpLine(line);
                    if (entry == null) continue;
                    String ip = entry[0], mac = entry[1];
                    if (subnets.stream().anyMatch(s -> ip.startsWith(s + ".")))
                        result.put(ip, mac);
                }
            }
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostArpResolver] ARP-Cache konnte nicht gelesen werden: " + e);
        }
        System.out.println("[NetworkHostScanner] ARP-Cache: " + result.size() + " Hosts gefunden");
        return result;
    }

    static String[] parseArpLine(String line) {
        try {
            Matcher ipM  = Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b").matcher(line);
            Matcher macM = MAC_PATTERN.matcher(line);
            if (!ipM.find() || !macM.find()) return null;
            String ip  = ipM.group();
            String mac = macM.group().toUpperCase().replace("-", ":");
            if (INVALID_MACS.contains(mac)) return null;
            if (mac.startsWith("FF:FF") || mac.startsWith("01:")) return null;
            if (ip.endsWith(".0") || ip.endsWith(".255")) return null;
            return new String[]{ip, mac};
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostArpResolver] ARP-Zeile nicht parsebar: " + e);
            return null;
        }
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
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.contains(targetIp)) continue;
                    String[] entry = parseArpLine(line);
                    if (entry != null) { p.destroy(); return entry[1]; }
                }
            }
            p.destroy();
        } catch (Exception e) {
            DebugLogger.getInstance().log("FINE", "[NetworkHostArpResolver] arp-Abfrage fehlgeschlagen (" + targetIp + "): " + e);
        }
        return null;
    }
}
