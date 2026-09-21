package main.java.networktool.logic.scan.host;

import main.java.networktool.util.CIDRUtils;
import main.java.networktool.logic.windows.PsCidrResolver;

import java.net.*;
import java.util.*;

public final class SubnetDetector {

    private SubnetDetector() {}

    private static final int MIN_PREFIX    = 16;
    private static final int MAX_PREFIX    = 30;
    static final int         MAX_SUBNETS   = 256;
    private static final int MAX_PER_IFACE = 256;

    private static final Set<String> SKIP_NAME_PREFIXES = Set.of(
            "docker", "br-", "veth", "virbr", "tun", "tap",
            "wg", "utun", "lo", "vmnet", "vbox"
    );

    /** Lokale Netze plus Heimnetz-Routen aus einem aktiven Tailscale-Subnet-Router. */
    public static List<String> getAllCidrs() throws SocketException {
        List<String> cidrs = new ArrayList<>(PsCidrResolver.resolveCidrs());
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        if (ifaces != null) collectInterfaceCidrs(cidrs, ifaces);
        addRouteCidrs(cidrs, TailscaleRouteSource.read(MIN_PREFIX, MAX_PREFIX));
        return cidrs;
    }

    public static List<String> getAllSubnets() throws SocketException {
        List<String> result = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        if (ifaces != null) collectInterfaceSubnets(result, ifaces);
        addRoutePrefixes(result, TailscaleRouteSource.read(MIN_PREFIX, MAX_PREFIX));

        if (!result.isEmpty())
            System.out.printf("[SubnetDetector] %d /24-Subnetz(e): %s%n", result.size(), result);
        return result;
    }

    public static int getTotalHostCount(List<String> subnets) {
        return subnets.size() * 254;
    }

    // ── Routen ────────────────────────────────────────────────────────────

    static void addRouteCidrs(List<String> cidrs, List<String> routeCidrs) {
        for (String cidr : routeCidrs)
            if (!cidrs.contains(cidr)) cidrs.add(cidr);
    }

    static void addRoutePrefixes(List<String> result, List<String> routeCidrs) {
        for (String cidr : routeCidrs)
            for (String prefix : CIDRUtils.getSubnet24Prefixes(cidr)) {
                if (result.size() >= MAX_SUBNETS) return;
                if (!result.contains(prefix)) result.add(prefix);
            }
    }

    // ── Interfaces ────────────────────────────────────────────────────────

    private static void collectInterfaceCidrs(List<String> cidrs, Enumeration<NetworkInterface> ifaces) {
        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            if (shouldSkip(ni)) continue;
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) addCidr(ia, cidrs);
        }
    }

    private static void collectInterfaceSubnets(List<String> result, Enumeration<NetworkInterface> ifaces) {
        while (ifaces.hasMoreElements() && result.size() < MAX_SUBNETS) {
            NetworkInterface ni = ifaces.nextElement();
            if (shouldSkip(ni)) continue;
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) addSubnets(ia, result);
        }
    }

    /** /31 und /32 (z.B. Tailscale-Adresse 100.x.y.z/32) sind kein scanbares Netz. */
    private static void addSubnets(InterfaceAddress ia, List<String> result) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address) || isLinkLocal(addr)) return;
        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX || prefix > MAX_PREFIX) return;

        int added = 0;
        for (String p : CIDRUtils.getSubnet24Prefixes(buildCidr(addr, prefix))) {
            if (added >= MAX_PER_IFACE || result.size() >= MAX_SUBNETS) break;
            if (!result.contains(p)) { result.add(p); added++; }
        }
    }

    private static boolean isLinkLocal(InetAddress addr) {
        return addr.isLinkLocalAddress() || addr.getHostAddress().startsWith("169.254.");
    }

    private static boolean shouldSkip(NetworkInterface ni) {
        try {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) return true;
            String name = ni.getName().toLowerCase();
            return SKIP_NAME_PREFIXES.stream().anyMatch(name::startsWith);
        } catch (SocketException e) { return true; }
    }

    private static void addCidr(InterfaceAddress ia, List<String> cidrs) {
        InetAddress addr = ia.getAddress();
        if (!(addr instanceof Inet4Address)) return;
        if (isLinkLocal(addr)) return;
        int prefix = ia.getNetworkPrefixLength();
        if (prefix < MIN_PREFIX || prefix > MAX_PREFIX) return;
        String cidr = buildCidr(addr, prefix);
        if (!cidrs.contains(cidr)) {
            System.out.printf("[SubnetDetector] Interface %s → %s%n", addr.getHostAddress(), cidr);
            cidrs.add(cidr);
        }
    }

    private static String buildCidr(InetAddress addr, int prefix) {
        byte[] b = addr.getAddress();
        int ipInt   = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) <<  8) |  (b[3] & 0xFF);
        int mask    = prefix == 0 ? 0 : (0xFFFFFFFF << (32 - prefix));
        int network = ipInt & mask;
        return CIDRUtils.intToIp(network) + "/" + prefix;
    }
}