package main.java.networktool.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** IPv6-Parsing/-Validierung, parallel zu {@link CIDRUtils} (IPv4). Keine Netzwerk-I/O. */
public final class Ipv6AddressUtils {

    private Ipv6AddressUtils() {}

    private static final Pattern IPV6 = Pattern.compile(
            "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
          + "|([0-9a-fA-F]{1,4}:){1,7}:"
          + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}"
          + "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}"
          + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}"
          + "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}"
          + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}"
          + "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})"
          + "|:((:[0-9a-fA-F]{1,4}){1,7}|:))$");

    public static boolean isValidIpv6(String ip) {
        return ip != null && IPV6.matcher(ip.trim()).matches();
    }

    public static boolean isValidCidr(String cidr) {
        return parsePrefixLength(cidr).isPresent();
    }

    public static Optional<Integer> parsePrefixLength(String cidr) {
        if (cidr == null) return Optional.empty();
        int slash = cidr.lastIndexOf('/');
        if (slash < 0) return Optional.empty();
        String addr = cidr.substring(0, slash);
        if (!isValidIpv6(addr)) return Optional.empty();
        try {
            int prefix = Integer.parseInt(cidr.substring(slash + 1).trim());
            return (prefix >= 0 && prefix <= 128) ? Optional.of(prefix) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Expandiert eine (ggf. mit "::" verkürzte) IPv6-Adresse auf 8 volle Hex-Gruppen. */
    public static String expand(String ipv6) {
        if (!isValidIpv6(ipv6)) throw new IllegalArgumentException("Ungültige IPv6-Adresse: " + ipv6);
        String trimmed = ipv6.trim();
        String[] parts = trimmed.split("::", -1);
        List<String> head = groupsOf(parts[0]);
        List<String> tail = parts.length > 1 ? groupsOf(parts[1]) : new ArrayList<>();
        List<String> full = new ArrayList<>(head);
        if (parts.length > 1) {
            int missing = 8 - head.size() - tail.size();
            for (int i = 0; i < missing; i++) full.add("0000");
        }
        full.addAll(tail);
        return String.join(":", full.stream().map(Ipv6AddressUtils::pad4).toList());
    }

    public static boolean isLinkLocal(String ipv6) {
        return isValidIpv6(ipv6) && expand(ipv6).toLowerCase().startsWith("fe80");
    }

    private static List<String> groupsOf(String segment) {
        if (segment == null || segment.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(segment.split(":")));
    }

    private static String pad4(String group) {
        StringBuilder sb = new StringBuilder(group.toLowerCase());
        while (sb.length() < 4) sb.insert(0, '0');
        return sb.toString();
    }
}
