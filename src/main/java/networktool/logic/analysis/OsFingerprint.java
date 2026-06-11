package main.java.networktool.logic.analysis;

import java.util.Map;

/**
 * Kombiniert TTL und offene Ports zu einer OS-Heuristik.
 * Präziser als TTL oder Ports allein. Package-private.
 */
final class OsFingerprint {

    private OsFingerprint() {}

    /** Gibt OS-String zurück oder null wenn nicht eindeutig. */
    static String resolve(String ip, int ttl, String mac) {
        if (ttl <= 0) return resolveByMac(mac);

        if (ttl <= 32)  return resolveSmallTtl(ip, mac);
        if (ttl <= 64)  return resolveMediumTtl(ip, mac);
        if (ttl <= 128) return resolveHighTtl(ip, mac);
        return resolveVeryHighTtl(ip, mac);
    }

    private static String resolveSmallTtl(String ip, String mac) {
        String fromMac = resolveByMac(mac);
        if (fromMac != null) return fromMac;
        if (isOpen(ip, 23) || isOpen(ip, 161)) return "Router / Netzwerkgerät";
        return "Router / Netzwerkgerät";
    }

    private static String resolveMediumTtl(String ip, String mac) {
        String fromMac = resolveByMac(mac);
        if (fromMac != null) return fromMac;
        if (isOpen(ip, 22) || isOpen(ip, 80) || isOpen(ip, 443)) return "Linux/Unix";
        if (isOpen(ip, 1883) || isOpen(ip, 8883))                 return "IoT-Gerät (MQTT)";
        return "Linux/Unix oder Android";
    }

    private static String resolveHighTtl(String ip, String mac) {
        String fromMac = resolveByMac(mac);
        if (fromMac != null) return fromMac;
        if (isOpen(ip, 445) || isOpen(ip, 3389) || isOpen(ip, 5985)) return "Windows";
        if (isOpen(ip, 135) && !isOpen(ip, 22))                       return "Windows";
        if (isOpen(ip, 548) || isOpen(ip, 5000))                      return "macOS";
        return "Android oder Windows";
    }

    private static String resolveVeryHighTtl(String ip, String mac) {
        String fromMac = resolveByMac(mac);
        if (fromMac != null) return fromMac;
        if (isOpen(ip, 548) || isOpen(ip, 5000))   return "macOS";
        if (isOpen(ip, 5353))                        return "Apple-Gerät (Bonjour)";
        if (isOpen(ip, 23) || isOpen(ip, 161))       return "Router / Netzwerkgerät";
        return "iOS / macOS";
    }

    private static String resolveByMac(String mac) {
        if (mac == null || mac.length() < 8) return null;
        return OuiDatabase.lookup(mac.substring(0, 8));
    }

    private static boolean isOpen(String ip, int port) {
        return OsDetectorPorts.isOpen(ip, port);
    }
}