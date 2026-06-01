package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;

/**
 * Erkennt alle aktiven IPv4-Subnetze der lokalen Netzwerkschnittstellen.
 * Loopback-Interfaces und Präfixe kürzer als /8 werden übersprungen.
 */
public final class SubnetDetector {

    private SubnetDetector() {}

    private static final int MIN_PREFIX = 8;

    /**
     * @return Liste von Subnetz-Präfixen (z.B. ["192.168.1", "10.0.0"])
     */
    public static List<String> getAllSubnets() throws SocketException {
        List<String> subnets = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (!ni.isUp() || ni.isLoopback()) continue;

            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                collectSubnets(ia, subnets);
            }
        }
        return subnets;
    }

    private static void collectSubnets(InterfaceAddress ia, List<String> subnets) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address)) return;

        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX) return;

        byte[] b     = addr.getAddress();
        int ipInt    = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                     | ((b[2] & 0xFF) <<  8) |  (b[3] & 0xFF);
        int mask      = -1 << (32 - prefix);
        int network   = ipInt & mask;
        int broadcast = network | ~mask;

        for (int base = network; base < broadcast; base += 256) {
            String subnet = ((base >> 24) & 0xFF) + "."
                          + ((base >> 16) & 0xFF) + "."
                          + ((base >>  8) & 0xFF);
            if (!subnets.contains(subnet)) subnets.add(subnet);
        }
    }
}
