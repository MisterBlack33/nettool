package main.java.networktool.logic.scan.host;

import main.java.networktool.util.Ipv6AddressUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adressarithmetik für IPv6-CIDRs. Ein /64 hat 2^64 Adressen, daher liefert
 * {@link #expand} höchstens {@link #MAX_HOSTS} Kandidaten ab Netzanfang.
 */
final class Ipv6HostRange {

    static final int MAX_HOSTS = 256;

    private static final int ADDRESS_BITS = 128;
    private static final int ADDRESS_BYTES = 16;
    private static final int HEX_RADIX = 16;

    private Ipv6HostRange() {}

    static List<String> expand(String cidr) {
        int prefix = prefixOf(cidr);
        BigInteger network = networkOf(cidr, prefix);
        int hostBits = ADDRESS_BITS - prefix;
        if (hostBits == 0) return List.of(toText(network));
        BigInteger usable = BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE);
        int count = usable.min(BigInteger.valueOf(MAX_HOSTS)).intValue();
        List<String> hosts = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) hosts.add(toText(network.add(BigInteger.valueOf(i))));
        return hosts;
    }

    static boolean contains(String cidr, String ip) {
        if (!Ipv6AddressUtils.isValidIpv6(ip)) return false;
        int prefix = prefixOf(cidr);
        return toInteger(ip).andNot(hostMask(prefix)).equals(networkOf(cidr, prefix));
    }

    /** Einheitliche Textform, damit dieselbe Adresse nie doppelt auftaucht. */
    static String canonical(String ip) {
        return toText(toInteger(ip));
    }

    private static int prefixOf(String cidr) {
        return Ipv6AddressUtils.parsePrefixLength(cidr)
                .orElseThrow(() -> new IllegalArgumentException("Ungültiges IPv6-CIDR: " + cidr));
    }

    private static BigInteger networkOf(String cidr, int prefix) {
        String address = cidr.substring(0, cidr.lastIndexOf('/'));
        return toInteger(address).andNot(hostMask(prefix));
    }

    private static BigInteger hostMask(int prefix) {
        return BigInteger.ONE.shiftLeft(ADDRESS_BITS - prefix).subtract(BigInteger.ONE);
    }

    private static BigInteger toInteger(String address) {
        return new BigInteger(Ipv6AddressUtils.expand(address).replace(":", ""), HEX_RADIX);
    }

    private static String toText(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] bytes = new byte[ADDRESS_BYTES];
        int length = Math.min(raw.length, ADDRESS_BYTES);
        System.arraycopy(raw, raw.length - length, bytes, ADDRESS_BYTES - length, length);
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Ungültige IPv6-Adresse", e);
        }
    }
}
