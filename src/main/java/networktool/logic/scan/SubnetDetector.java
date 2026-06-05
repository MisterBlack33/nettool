package main.java.networktool.logic.scan;

import java.net.*;
import java.util.*;

/**
 * Erkennt aktive IPv4-Subnetze der lokalen Netzwerkschnittstellen.
 *
 * Fehler-Fix: MIN_PREFIX war 8 → /8-Netze (VPN, Docker, WSL) erzeugten
 * 65.535 Subnet-Prefixes und damit ~16 Mio zu scannende IPs.
 * Jetzt: nur /24 und kleiner (/24, /25, /26...) werden akzeptiert.
 * Virtuelle Interfaces (docker, tun, wg, virbr...) werden übersprungen.
 */
public final class SubnetDetector {

    private SubnetDetector() {}

    /** Nur Netze mit Prefix >= 24 (/24 und kleiner) scannen. */
    private static final int MIN_PREFIX = 24;

    private static final Set<String> SKIP_NAMES = Set.of(
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
            return SKIP_NAMES.stream().anyMatch(name::startsWith);
        } catch (SocketException e) {
            return true;
        }
    }

    private static void addSubnet(InterfaceAddress ia, List<String> subnets) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address)) return;

        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX) return;  // /8, /16 etc. → überspringen

        byte[] b = addr.getAddress();
        // Nur das /24-Prefix des Interface bestimmen (die ersten 3 Oktette)
        String subnet = (b[0] & 0xFF) + "." + (b[1] & 0xFF) + "." + (b[2] & 0xFF);
        if (!subnets.contains(subnet)) subnets.add(subnet);
    }
}