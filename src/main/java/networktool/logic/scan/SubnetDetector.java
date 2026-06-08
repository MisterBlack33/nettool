package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;

/**
 * Erkennt aktive IPv4-Subnetze der lokalen Netzwerkschnittstellen.
 *
 * Akzeptiert Interfaces mit Prefix >= 16 (/16–/30), extrahiert daraus
 * immer das zugehörige /24-Segment für den Host-Scan.
 * Virtuelle/interne Interfaces werden übersprungen.
 */
public final class SubnetDetector {

    private SubnetDetector() {}

    /** Mindest-Prefix: /16 – engere Netze (/8 etc.) werden übersprungen. */
    private static final int MIN_PREFIX = 16;

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "docker", "br-", "veth", "virbr", "tun", "tap",
            "wg", "utun", "lo", "vmnet", "vbox"
    );

    public static List<String> getAllSubnets() throws SocketException {
        List<String> subnets = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        if (ifaces == null) return subnets;

        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            if (shouldSkip(ni)) continue;
            for (InterfaceAddress ia : ni.getInterfaceAddresses())
                addSubnet(ia, subnets);
        }
        return subnets;
    }

    private static boolean shouldSkip(NetworkInterface ni) {
        try {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) return true;
            String name = ni.getName().toLowerCase();
            return SKIP_PREFIXES.stream().anyMatch(name::startsWith);
        } catch (SocketException e) {
            return true;
        }
    }

    private static void addSubnet(InterfaceAddress ia, List<String> subnets) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address)) return;

        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX) return; // /8 etc. → überspringen

        byte[] b = addr.getAddress();
        // Immer /24-Prefix extrahieren – unabhängig vom tatsächlichen Prefix
        String subnet = (b[0] & 0xFF) + "." + (b[1] & 0xFF) + "." + (b[2] & 0xFF);
        if (!subnets.contains(subnet)) subnets.add(subnet);
    }
}