package main.java.networktool.logic.scan;

import main.java.networktool.util.CIDRUtils;

import java.net.*;
import java.util.*;

public final class SubnetDetector {

    private SubnetDetector() {}

    private static final int MIN_PREFIX    = 16;  // kleinste akzeptierte Maske
    private static final int MAX_SUBNETS   = 256;

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "docker", "br-", "veth", "virbr", "tun", "tap",
            "wg", "utun", "lo", "vmnet", "vbox"
    );

    /** CIDR-Strings des lokalen Interfaces, z.B. ["192.168.1.0/24"]. */
    public static List<String> getAllCidrs() throws SocketException {
        List<String> cidrs = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        if (ifaces == null) return cidrs;
        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            if (shouldSkip(ni)) continue;
            for (InterfaceAddress ia : ni.getInterfaceAddresses())
                addCidr(ia, cidrs);
        }
        return cidrs;
    }

    /**
     * Legacy: /24-Präfixe für Scanner, maximal MAX_SUBNETS Einträge.
     * Große Netze (z.B. /8) werden auf die ersten MAX_SUBNETS /24-Blöcke begrenzt.
     */
    public static List<String> getAllSubnets() throws SocketException {
        List<String> result = new ArrayList<>();
        for (String cidr : getAllCidrs()) {
            for (String prefix : CIDRUtils.getSubnet24Prefixes(cidr)) {
                if (!result.contains(prefix)) result.add(prefix);
                if (result.size() >= MAX_SUBNETS) return result;
            }
        }
        return result;
    }

    private static boolean shouldSkip(NetworkInterface ni) {
        try {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) return true;
            String name = ni.getName().toLowerCase();
            return SKIP_PREFIXES.stream().anyMatch(name::startsWith);
        } catch (SocketException e) { return true; }
    }

    private static void addCidr(InterfaceAddress ia, List<String> cidrs) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address)) return;
        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX) return;
        byte[] b = addr.getAddress();
        int ipInt   = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) <<  8) |  (b[3] & 0xFF);
        int mask    = 0xFFFFFFFF << (32 - prefix);
        int network = ipInt & mask;
        String cidr = CIDRUtils.intToIp(network) + "/" + prefix;
        if (!cidrs.contains(cidr)) cidrs.add(cidr);
    }
}