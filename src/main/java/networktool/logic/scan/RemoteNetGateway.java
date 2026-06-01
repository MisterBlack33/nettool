package main.java.networktool.logic.scan;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Gateway detection and routing hint output. Package-private. */
final class RemoteNetGateway {

    private RemoteNetGateway() {}

    static String detectDefaultGateway() {
        try {
            boolean win = isWin();
            ProcessBuilder pb = win
                    ? new ProcessBuilder("cmd", "/c", "route", "print", "0.0.0.0")
                    : new ProcessBuilder("sh", "-c",
                    "ip route show default 2>/dev/null || netstat -rn 2>/dev/null");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (win && line.trim().startsWith("0.0.0.0")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) return parts[2];
                    } else if (!win && line.startsWith("default via ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) return parts[2];
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    static void printRoutingHints(String cidr) {
        cidr = RemoteNetScanner.normalizeCidr(cidr);
        String[] parts  = cidr.split("/");
        String network  = parts[0];
        String prefix   = parts.length > 1 ? parts[1] : "24";
        String netmask  = prefixToNetmask(Integer.parseInt(prefix));
        String gw       = detectDefaultGateway();
        String gwStr    = gw != null ? gw : "<GATEWAY-IP>";

        System.out.println("\n  ── Routing einrichten ───────────────────────────");
        System.out.println("  Ziel:    " + cidr);
        System.out.println("  Gateway: " + gwStr);
        System.out.println();
        if (isWin()) {
            System.out.println("  [Windows] route add " + network + " mask " + netmask + " " + gwStr);
            System.out.println("  [Windows] Dauerhaft: route -p add " + network
                    + " mask " + netmask + " " + gwStr);
        } else {
            System.out.println("  [Linux]   ip route add " + cidr + " via " + gwStr);
            System.out.println("  [macOS]   sudo route -n add -net "
                    + network + "/" + prefix + " " + gwStr);
        }
        System.out.println("  ────────────────────────────────────────────────");
    }

    private static String prefixToNetmask(int prefix) {
        int mask = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        return ((mask >> 24) & 0xFF) + "." + ((mask >> 16) & 0xFF)
                + "." + ((mask >> 8) & 0xFF) + "." + (mask & 0xFF);
    }

    private static boolean isWin() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}