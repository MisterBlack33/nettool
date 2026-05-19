package main.java.networktool_v3.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP-Adressen- und CIDR-Validierung.
 */
public final class IpValidator {

    private IpValidator() {}

    public static boolean isValidIpv4(String ip) {
        if (!PlatformUtils.isSafeIp(ip)) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            try {
                int v = Integer.parseInt(p);
                if (v < 0 || v > 255) return false;
            } catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    public static boolean isValidCidr(String cidr) {
        if (!PlatformUtils.isSafeCidr(cidr)) return false;
        String[] parts = cidr.split("/");
        if (!isValidIpv4(parts[0])) return false;
        try {
            int prefix = Integer.parseInt(parts[1]);
            return prefix >= 0 && prefix <= 32;
        } catch (NumberFormatException e) { return false; }
    }

    public static boolean isValidHostname(String host) {
        if (isValidIpv4(host)) return true;
        return PlatformUtils.isSafeHostname(host);
    }

    /** Versucht DNS-Auflösung – gibt false zurück wenn nicht auflösbar. */
    public static boolean isResolvable(String host) {
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException e) { return false; }
    }

    /** Bereinigt IP – entfernt Leerzeichen, gibt null zurück wenn ungültig. */
    public static String sanitize(String ip) {
        if (ip == null) return null;
        String s = ip.trim();
        return isValidIpv4(s) ? s : null;
    }
}