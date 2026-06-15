package main.java.networktool.logic.scan;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Erkennt Netzwerk-Rollen (DNS-Server, DHCP-Server, mDNS) durch aktives Probing.
 *
 * Strategie je Protokoll:
 *  DNS  → UDP-Query (A-Record "example.com") + TCP-Port-Check
 *  DHCP → UDP-Discover (Broadcast-fähig, Port 67) + Antwort-Listening
 *  mDNS → UDP-Query auf 224.0.0.251:5353
 *  NTP  → UDP-Request auf Port 123
 *
 * Ergebnisse werden für die Karten-Laufzeit gecacht.
 */
public class MapTrafficObserver {

    public enum NodeRole { DNS_SERVER, DHCP_SERVER, MDNS_NODE, NTP_SERVER, UNKNOWN }

    private static final int TIMEOUT_MS   = 600;
    private static final int DNS_PORT     = 53;
    private static final int DHCP_PORT    = 67;
    private static final int MDNS_PORT    = 5353;
    private static final int NTP_PORT     = 123;

    private final Map<String, NodeRole> roles = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────

    public void probe(String ip) {
        NodeRole role = detect(ip);
        if (role != NodeRole.UNKNOWN) roles.put(ip, role);
    }

    public void probeAll(List<String> ips) {
        ExecutorService exec = Executors.newFixedThreadPool(
                Math.min(ips.size(), 16));
        ips.forEach(ip -> exec.submit(() -> probe(ip)));
        exec.shutdown();
        try { exec.awaitTermination(TIMEOUT_MS * 2L + 500, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public NodeRole getRole(String ip) {
        return roles.getOrDefault(ip, NodeRole.UNKNOWN);
    }

    public Map<String, NodeRole> getAllRoles() {
        return Collections.unmodifiableMap(roles);
    }

    public void clear() { roles.clear(); }

    // ── Detection ─────────────────────────────────────────────────────────

    private NodeRole detect(String ip) {
        if (isDnsServer(ip))  return NodeRole.DNS_SERVER;
        if (isDhcpServer(ip)) return NodeRole.DHCP_SERVER;
        if (isMdnsNode(ip))   return NodeRole.MDNS_NODE;
        if (isNtpServer(ip))  return NodeRole.NTP_SERVER;
        return NodeRole.UNKNOWN;
    }

    /**
     * DNS-Erkennung:
     *  1. TCP-Port 53 offen (schnell)
     *  2. UDP DNS-Query auf "version.bind" → antwortet nur echter DNS-Server
     */
    private boolean isDnsServer(String ip) {
        if (isTcpOpen(ip, DNS_PORT)) return true;
        return sendDnsQuery(ip);
    }

    /**
     * DHCP: UDP-Port 67.
     * Direktes Verbinden auf UDP 67 ist auf den meisten OS blockiert,
     * daher prüfen wir ob der Host auf einen simplen UDP-Paket antwortet.
     */
    private boolean isDhcpServer(String ip) {
        return isUdpResponsive(ip, DHCP_PORT, buildDhcpDiscover());
    }

    /**
     * mDNS: Multicast 224.0.0.251:5353.
     * Wir senden eine mDNS-Query und prüfen ob eine Antwort kommt.
     * Fallback: TCP/UDP 5353 offen.
     */
    private boolean isMdnsNode(String ip) {
        if (isTcpOpen(ip, MDNS_PORT)) return true;
        return sendMdnsQuery(ip);
    }

    private boolean isNtpServer(String ip) {
        return isUdpResponsive(ip, NTP_PORT, buildNtpRequest());
    }

    // ── DNS Query (UDP) ───────────────────────────────────────────────────

    /**
     * Sendet einen minimalen DNS-Query für "version.bind" (CHAOS/TXT).
     * Echter DNS-Server antwortet, andere Geräte antworten nicht.
     */
    private boolean sendDnsQuery(String ip) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] query = buildDnsQuery("version.bind", true);
            InetAddress addr = InetAddress.getByName(ip);
            socket.send(new DatagramPacket(query, query.length, addr, DNS_PORT));
            byte[] buf = new byte[512];
            socket.receive(new DatagramPacket(buf, buf.length));
            // Antwort empfangen → DNS-Server bestätigt
            return true;
        } catch (SocketTimeoutException e) {
            return false; // kein DNS-Server
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Minimaler DNS-Query.
     * @param name  Hostname (z.B. "version.bind")
     * @param chaos true = CHAOS-Klasse, false = IN-Klasse
     */
    private byte[] buildDnsQuery(String name, boolean chaos) {
        // ID=0x1337, QR=0 (Query), Opcode=0, RD=1
        byte[] header = {
                0x13, 0x37,  // ID
                0x01, 0x00,  // Flags: RD=1
                0x00, 0x01,  // QDCOUNT=1
                0x00, 0x00,  // ANCOUNT=0
                0x00, 0x00,  // NSCOUNT=0
                0x00, 0x00   // ARCOUNT=0
        };

        // Name kodieren
        String[] labels = name.split("\\.");
        ByteArrayOutputStream nameBytes = new ByteArrayOutputStream();
        for (String label : labels) {
            byte[] lb = label.getBytes(StandardCharsets.US_ASCII);
            nameBytes.write(lb.length);
            nameBytes.write(lb, 0, lb.length);
        }
        nameBytes.write(0x00); // End-of-Name

        // QTYPE=TXT(16), QCLASS=CHAOS(3) oder IN(1)
        byte qclass = chaos ? (byte) 0x03 : (byte) 0x01;
        byte[] qtype = {0x00, 0x10, 0x00, qclass};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header, 0, header.length);
        byte[] nb = nameBytes.toByteArray();
        out.write(nb, 0, nb.length);
        out.write(qtype, 0, qtype.length);
        return out.toByteArray();
    }

    // ── DHCP Discover (UDP) ───────────────────────────────────────────────

    /**
     * Minimales DHCP-Discover-Paket.
     * Wird an den spezifischen Host gesendet (kein Broadcast) um Kollisionen zu vermeiden.
     */
    private byte[] buildDhcpDiscover() {
        byte[] pkt = new byte[300];
        pkt[0]  = 0x01; // BOOTREQUEST
        pkt[1]  = 0x01; // Ethernet
        pkt[2]  = 0x06; // Hardware address length
        pkt[3]  = 0x00; // Hops
        // XID (random)
        new Random().nextBytes(new byte[4]);
        pkt[4] = 0x39; pkt[5] = 0x03; pkt[6] = (byte)0xF3; pkt[7] = 0x26;
        // Flags: broadcast
        pkt[10] = (byte)0x80; pkt[11] = 0x00;
        // Magic cookie
        pkt[236] = 0x63; pkt[237] = (byte)0x82; pkt[238] = 0x53; pkt[239] = 0x63;
        // Option 53: DHCP Discover
        pkt[240] = 0x35; pkt[241] = 0x01; pkt[242] = 0x01;
        // End option
        pkt[243] = (byte)0xFF;
        return pkt;
    }

    // ── mDNS Query (UDP Multicast) ────────────────────────────────────────

    private boolean sendMdnsQuery(String ip) {
        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setTimeToLive(1);
            byte[] query = buildDnsQuery("_services._dns-sd._udp.local", false);
            InetAddress mdnsGroup = InetAddress.getByName("224.0.0.251");
            socket.send(new DatagramPacket(query, query.length, mdnsGroup, MDNS_PORT));
            byte[] buf = new byte[512];
            DatagramPacket response = new DatagramPacket(buf, buf.length);
            socket.receive(response);
            // Prüfen ob Antwort von der gesuchten IP kommt
            return ip.equals(response.getAddress().getHostAddress());
        } catch (Exception e) {
            return false;
        }
    }

    // ── NTP Request (UDP) ─────────────────────────────────────────────────

    private byte[] buildNtpRequest() {
        byte[] pkt = new byte[48];
        pkt[0] = 0x1B; // LI=0, VN=3, Mode=3 (Client)
        return pkt;
    }

    // ── Generic UDP probe ─────────────────────────────────────────────────

    private boolean isUdpResponsive(String ip, int port, byte[] payload) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress addr = InetAddress.getByName(ip);
            socket.send(new DatagramPacket(payload, payload.length, addr, port));
            byte[] buf = new byte[512];
            socket.receive(new DatagramPacket(buf, buf.length));
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ── TCP helper ────────────────────────────────────────────────────────

    private boolean isTcpOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}