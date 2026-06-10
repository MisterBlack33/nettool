package main.java.networktool.logic.scan;

import main.java.networktool.util.CIDRUtils;

import java.net.*;
import java.util.*;

public final class SubnetDetector {

    private SubnetDetector() {}

    /** Kleinster erlaubter Prefix (kein /8 etc.). */
    private static final int MIN_PREFIX  = 16;
    /** Max. /24-Subnetze über ALLE Interfaces. */
    private static final int MAX_SUBNETS = 64;
    /** Max. /24-Blöcke pro einzelnem Interface (verhindert /16-Explosion). */
    private static final int MAX_PER_IFACE = 16;

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "docker", "br-", "veth", "virbr", "tun", "tap",
            "wg", "utun", "lo", "vmnet", "vbox"
    );

    /** CIDR-Strings der lokalen Interfaces, z.B. ["192.168.1.0/24"]. */
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
     * /24-Präfixe für den Host-Scanner.
     * Pro Interface maximal MAX_PER_IFACE Präfixe, insgesamt MAX_SUBNETS.
     */
    public static List<String> getAllSubnets() throws SocketException {
        List<String> result = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        if (ifaces == null) return result;

        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            if (shouldSkip(ni)) continue;

            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress addr = ia.getAddress();
                if (!(addr instanceof Inet4Address)) continue;
                int prefix = ia.getNetworkPrefixLength();
                if (prefix < MIN_PREFIX) continue;

                List<String> prefixes = CIDRUtils.getSubnet24Prefixes(
                        buildCidr(addr, prefix));

                // Begrenzt auf MAX_PER_IFACE pro Interface
                int added = 0;
                for (String p : prefixes) {
                    if (added >= MAX_PER_IFACE) break;
                    if (result.size() >= MAX_SUBNETS) break;
                    if (!result.contains(p)) { result.add(p); added++; }
                }
            }
            if (result.size() >= MAX_SUBNETS) break;
        }

        if (!result.isEmpty())
            System.out.printf("[SubnetDetector] %d /24-Subnetz(e): %s%n",
                    result.size(), result);
        return result;
    }

    public static int getTotalHostCount(List<String> subnets) {
        return subnets.size() * 254;
    }

    // ── private ───────────────────────────────────────────────────────────

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
        String cidr = buildCidr(addr, prefix);
        if (!cidrs.contains(cidr)) {
            System.out.printf("[SubnetDetector] Interface %s → %s%n",
                    addr.getHostAddress(), cidr);
            cidrs.add(cidr);
        }
    }

    private static String buildCidr(InetAddress addr, int prefix) {
        byte[] b = addr.getAddress();
        int ipInt   = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) <<  8) |  (b[3] & 0xFF);
        int mask    = 0xFFFFFFFF << (32 - prefix);
        int network = ipInt & mask;
        return CIDRUtils.intToIp(network) + "/" + prefix;
    }
}