package main.java.networktool.logic.analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

/** ARP-Abfrage und MAC-Extraktion. Paket-privat, nur von OsDetector verwendet. */
final class OsDetectorArp {

    private OsDetectorArp() {}

    private static final Set<String> INVALID_MACS = Set.of(
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF",
            "00:AA:00:00:00:00", "00:AA:00:AA:00:AA");

    private static final Pattern MAC_COLON =
            Pattern.compile("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");
    private static final Pattern MAC_DASH  =
            Pattern.compile("([0-9A-Fa-f]{2}-){5}[0-9A-Fa-f]{2}");

    static String getMacFromArp(String ip) {
        triggerArp(ip);
        String[][] cmds = isWin()
                ? new String[][]{{"arp", "-a", ip}, {"arp", "-a"}}
                : new String[][]{{"arp", "-n", ip}, {"arp", "-a", "-n"}, {"arp", "-a"}};
        for (String[] cmd : cmds) {
            String mac = queryArp(cmd, ip);
            if (mac != null) return mac;
        }
        return null;
    }

    static int getTtl(String ip) {
        try {
            String[] cmd = isWin()
                    ? new String[]{"ping", "-n", "1", ip}
                    : new String[]{"ping", "-c", "1", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append(' ');
            p.destroy();
            Matcher m = Pattern.compile("(?i)ttl[=:]\\s*(\\d+)").matcher(sb);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    private static void triggerArp(String ip) {
        try {
            String[] cmd = isWin()
                    ? new String[]{"ping", "-n", "1", "-w", "300", ip}
                    : new String[]{"ping", "-c", "1", "-W", "1", ip};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(700, TimeUnit.MILLISECONDS);
            p.destroy();
        } catch (Exception ignored) {}
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
        Matcher m1 = MAC_COLON.matcher(line);
        if (m1.find()) { String m = m1.group().toUpperCase(); return isValidMac(m) ? m : null; }
        Matcher m2 = MAC_DASH.matcher(line);
        if (m2.find()) { String m = m2.group().replace("-",":").toUpperCase(); return isValidMac(m) ? m : null; }
        return null;
    }

    private static boolean isValidMac(String mac) {
        if (mac == null || mac.length() < 17) return false;
        if (INVALID_MACS.contains(mac))        return false;
        if (mac.startsWith("FF:FF"))           return false;
        if (mac.startsWith("01:"))             return false;
        if (mac.replace(":","").replace("0","").isEmpty()) return false;
        return true;
    }

    private static boolean isWin() {
        return System.getProperty("os.name","").toLowerCase().contains("win");
    }
}