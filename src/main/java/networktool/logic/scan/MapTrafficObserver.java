// src/main/java/networktool/logic/scan/MapTrafficObserver.java
package main.java.networktool.logic.scan;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Beobachtet DNS- und DHCP-Antworten passiv über den ARP-Cache
 * und leitet daraus Netzwerk-Rollen (DNS-Server, DHCP-Server) ab.
 * Kein Raw-Socket nötig – nutzt Port-Probing auf bekannten Ports.
 */
public class MapTrafficObserver {

    private static final int DNS_PORT  = 53;
    private static final int DHCP_PORT = 67;
    private static final int MDNS_PORT = 5353;
    private static final int TIMEOUT   = 400;

    private final Map<String, NodeRole> roles = new ConcurrentHashMap<>();

    public enum NodeRole { DNS_SERVER, DHCP_SERVER, MDNS_NODE, UNKNOWN }

    public void probe(String ip) {
        NodeRole role = detectRole(ip);
        if (role != NodeRole.UNKNOWN) roles.put(ip, role);
    }

    public NodeRole getRole(String ip) {
        return roles.getOrDefault(ip, NodeRole.UNKNOWN);
    }

    public Map<String, NodeRole> getAllRoles() {
        return Collections.unmodifiableMap(roles);
    }

    public void clear() { roles.clear(); }

    private NodeRole detectRole(String ip) {
        if (isPortOpen(ip, DNS_PORT) && !isPortOpen(ip, 80))  return NodeRole.DNS_SERVER;
        if (isPortOpen(ip, DHCP_PORT))                         return NodeRole.DHCP_SERVER;
        if (isPortOpen(ip, MDNS_PORT))                         return NodeRole.MDNS_NODE;
        return NodeRole.UNKNOWN;
    }

    private boolean isPortOpen(String ip, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(ip, port), TIMEOUT);
            return true;
        } catch (Exception e) { return false; }
    }
}