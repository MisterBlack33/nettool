package main.java.networktool_v3.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Berechnet alle Host-IPs für eine CIDR-Notation (z.B. "192.168.1.0/24").
 * Netz- und Broadcast-Adresse werden nicht eingeschlossen.
 */
public final class CIDRUtils {

    private CIDRUtils() {}

    public static List<String> getAllIPs(String cidr) {
        String[] parts = cidr.split("/");
        int prefix     = Integer.parseInt(parts[1]);
        int ipInt      = ipToInt(parts[0]);
        int mask       = -1 << (32 - prefix);
        int network    = ipInt & mask;
        int broadcast  = network | ~mask;

        List<String> ips = new ArrayList<>();
        for (int i = network + 1; i < broadcast; i++) {
            ips.add(intToIp(i));
        }
        return ips;
    }

    private static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        int result = 0;
        for (String p : parts) result = (result << 8) | Integer.parseInt(p);
        return result;
    }

    private static String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "."
             + ((ip >> 16) & 0xFF) + "."
             + ((ip >> 8)  & 0xFF) + "."
             +  (ip        & 0xFF);
    }
}
