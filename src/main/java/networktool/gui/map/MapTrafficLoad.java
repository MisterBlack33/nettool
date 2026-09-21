package main.java.networktool.gui.map;

import main.java.networktool.logic.scan.schedule.MapTrafficObserver.NodeRole;

import java.util.HashMap;
import java.util.Map;

/**
 * Gewichtet die vom {@code MapTrafficObserver} erkannten Rollen als Last-Proxy je IP:
 * Infrastruktur-Dienste (DNS/DHCP/NTP) erzeugen typischerweise mehr Verkehr als Endgeräte.
 */
public final class MapTrafficLoad {

    private static final long WEIGHT_DNS  = 100;
    private static final long WEIGHT_DHCP = 60;
    private static final long WEIGHT_NTP  = 40;
    private static final long WEIGHT_MDNS = 20;

    private MapTrafficLoad() {}

    public static Map<String, Long> fromRoles(Map<String, NodeRole> roles) {
        Map<String, Long> load = new HashMap<>();
        if (roles == null) return load;
        roles.forEach((ip, role) -> {
            long weight = weightOf(role);
            if (weight > 0) load.put(ip, weight);
        });
        return load;
    }

    static long weightOf(NodeRole role) {
        if (role == null) return 0;
        return switch (role) {
            case DNS_SERVER  -> WEIGHT_DNS;
            case DHCP_SERVER -> WEIGHT_DHCP;
            case NTP_SERVER  -> WEIGHT_NTP;
            case MDNS_NODE   -> WEIGHT_MDNS;
            default          -> 0;
        };
    }
}
