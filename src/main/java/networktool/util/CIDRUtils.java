package main.java.networktool.util;

import java.util.ArrayList;
import java.util.List;

public final class CIDRUtils {

    private CIDRUtils() {}

    public static List<String> getAllIPs(String cidr) {
        String[] parts = cidr.split("/");
        int prefix     = Integer.parseInt(parts[1]);
        int ipInt      = ipToInt(parts[0]);
        int mask       = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        int network    = ipInt & mask;
        int broadcast  = network | ~mask;

        final int first = network + 1;
        final int last = broadcast - 1;
        final int count = Math.max(0, last - first + 1);

        return new java.util.AbstractList<String>() {
            @Override
            public String get(int index) {
                if (index < 0 || index >= count) throw new IndexOutOfBoundsException();
                return intToIp(first + index);
            }

            @Override
            public int size() {
                return count;
            }
        };
    }

    /** Liefert CIDR-Strings für alle /24-Blöcke im Netz (für Host-Scanner). */
    public static List<String> getSubnet24Prefixes(String cidr) {
        String[] parts = cidr.split("/");
        int prefix  = Integer.parseInt(parts[1]);
        int ipInt   = ipToInt(parts[0]);
        int mask    = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        int network = ipInt & mask;

        List<String> prefixes = new ArrayList<>();
        if (prefix >= 24) {
            prefixes.add(subnet24String(network));
        } else {
            int count = 1 << (24 - prefix);
            for (int i = 0; i < count; i++)
                prefixes.add(subnet24String(network + (i << 8)));
        }
        return prefixes;
    }

    public static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        int result = 0;
        for (String p : parts) result = (result << 8) | Integer.parseInt(p);
        return result;
    }

    public static String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8)  & 0xFF) + "."
                +  (ip        & 0xFF);
    }

    private static String subnet24String(int networkBase) {
        return ((networkBase >> 24) & 0xFF) + "."
                + ((networkBase >> 16) & 0xFF) + "."
                + ((networkBase >> 8)  & 0xFF);
    }
}